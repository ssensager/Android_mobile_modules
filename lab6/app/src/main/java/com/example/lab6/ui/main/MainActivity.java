package com.example.lab6.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6.R;
import com.example.lab6.data.projection.EquipmentWithMaxSet;
import com.example.lab6.data.repository.WorkoutRepository;
import com.example.lab6.ui.chart.ProgressChartView;
import com.example.lab6.ui.detail.EquipmentDetailActivity;
import com.example.lab6.util.CleanupPrefs;
import com.example.lab6.util.DateUtils;
import com.example.lab6.worker.CleanupWorker;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.List;

/**
 * Главный экран трекера «ПрогрессЗал».
 * Вывожу аналитику за сегодня, график объёма за 7 дней, поиск с задержкой 300 мс,
 * фильтрацию по группе мышц, быстрый ввод подхода, экспорт CSV и настройки автоочистки.
 */
public class MainActivity extends AppCompatActivity {

    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private MainViewModel viewModel;
    private EquipmentAdapter adapter;

    private TextView tvTodaySetsCount;
    private TextView tvTodayVolume;
    private TextView tvBestRecord;
    private TextView tvBestRecordName;
    private ProgressChartView progressChart;

    private MaterialCardView cardPrBanner;
    private TextView tvPrBannerDetails;

    private TextInputEditText etSearch;
    private ChipGroup chipGroupMuscles;
    private RecyclerView rvEquipment;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private MaterialButton btnResetFilters;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Если пользователь отказал в уведомлениях, приложение продолжает работать без сбоев
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Планирую ежедневную фоновую автоочистку устаревших подходов
        CleanupWorker.schedulePeriodicCleanup(this);
        requestNotificationPermissionIfNeeded();

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        bindViews();
        applyEdgeInsets();
        setupToolbar();
        setupRecyclerView();
        setupSearchAndFilters();
        observeViewModel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
    }

    private void bindViews() {
        tvTodaySetsCount = findViewById(R.id.tvTodaySetsCount);
        tvTodayVolume = findViewById(R.id.tvTodayVolume);
        tvBestRecord = findViewById(R.id.tvBestRecord);
        tvBestRecordName = findViewById(R.id.tvBestRecordName);
        progressChart = findViewById(R.id.progressChart);

        cardPrBanner = findViewById(R.id.cardPrBanner);
        tvPrBannerDetails = findViewById(R.id.tvPrBannerDetails);
        MaterialButton btnDismissPr = findViewById(R.id.btnDismissPr);
        btnDismissPr.setOnClickListener(v -> viewModel.dismissPrBanner());

        etSearch = findViewById(R.id.etSearch);
        chipGroupMuscles = findViewById(R.id.chipGroupMuscles);
        rvEquipment = findViewById(R.id.rvEquipment);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnResetFilters = findViewById(R.id.btnResetFilters);

        ExtendedFloatingActionButton fabAddEquipment = findViewById(R.id.fabAddEquipment);
        fabAddEquipment.setOnClickListener(v -> showAddEquipmentDialog());
    }

    /**
     * Подстраиваю отступы под системные панели через WindowInsetsCompat.
     */
    private void applyEdgeInsets() {
        View root = findViewById(R.id.rootMain);
        AppBarLayout appBar = findViewById(R.id.appBarMain);
        ExtendedFloatingActionButton fab = findViewById(R.id.fabAddEquipment);

        final int baseFabBottom = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin;
        final int baseRvBottom = rvEquipment.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            rvEquipment.setPadding(
                    rvEquipment.getPaddingLeft(),
                    rvEquipment.getPaddingTop(),
                    rvEquipment.getPaddingRight(),
                    baseRvBottom + bars.bottom
            );
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            lp.bottomMargin = baseFabBottom + bars.bottom;
            fab.setLayoutParams(lp);
            return insets;
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarMain);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export_csv) {
                exportHistoryCsv();
                return true;
            }
            if (id == R.id.action_cleanup_settings) {
                showCleanupSettingsDialog();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new EquipmentAdapter(new EquipmentAdapter.OnEquipmentInteractionListener() {
            @Override
            public void onEquipmentClick(EquipmentWithMaxSet item) {
                Intent intent = new Intent(MainActivity.this, EquipmentDetailActivity.class);
                intent.putExtra(EquipmentDetailActivity.EXTRA_EQUIPMENT_ID, item.id);
                startActivity(intent);
            }

            @Override
            public void onQuickAddSetClick(EquipmentWithMaxSet item) {
                showQuickAddSetDialog(item);
            }
        });
        rvEquipment.setLayoutManager(new LinearLayoutManager(this));
        rvEquipment.setAdapter(adapter);
    }

    /**
     * Добавил дебаунс 300 мс для поиска, чтобы быстрый ввод текста не дёргал SQLite на каждую букву.
     */
    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearchRunnable != null) {
                    searchHandler.removeCallbacks(pendingSearchRunnable);
                }
                final String query = s == null ? "" : s.toString();
                pendingSearchRunnable = () -> viewModel.setSearchQuery(query);
                searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroupMuscles.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            viewModel.setMuscleGroup(resolveMuscleGroupByChipId(checkedId));
        });

        btnResetFilters.setOnClickListener(v -> {
            if (pendingSearchRunnable != null) {
                searchHandler.removeCallbacks(pendingSearchRunnable);
            }
            etSearch.setText("");
            chipGroupMuscles.check(R.id.chipAll);
            viewModel.resetFilters();
        });
    }

    private String resolveMuscleGroupByChipId(int chipId) {
        if (chipId == R.id.chipChest) return getString(R.string.group_chest);
        if (chipId == R.id.chipBack) return getString(R.string.group_back);
        if (chipId == R.id.chipLegs) return getString(R.string.group_legs);
        if (chipId == R.id.chipShoulders) return getString(R.string.group_shoulders);
        if (chipId == R.id.chipArms) return getString(R.string.group_arms);
        return "";
    }

    private void observeViewModel() {
        viewModel.getTodaySetsCount().observe(this, count -> {
            int safeCount = count == null ? 0 : count;
            tvTodaySetsCount.setText(String.valueOf(safeCount));
        });

        viewModel.getTodayVolumeKg().observe(this, volume -> {
            float safeVol = volume == null ? 0f : volume;
            tvTodayVolume.setText(getString(R.string.kpi_volume_value, DateUtils.formatNumber(safeVol)));
        });

        viewModel.getBestOverallRecord().observe(this, best -> {
            if (best == null) {
                tvBestRecord.setText(R.string.kpi_record_empty);
                tvBestRecordName.setText("");
            } else {
                tvBestRecord.setText(DateUtils.formatNumber(best.weightKg) + " кг × " + best.reps);
                tvBestRecordName.setText(best.equipmentName);
            }
        });

        viewModel.getWeeklyVolume().observe(this, points -> progressChart.setData(points));

        viewModel.getPrBannerMessage().observe(this, message -> {
            if (message == null || message.isEmpty()) {
                cardPrBanner.setVisibility(View.GONE);
            } else {
                tvPrBannerDetails.setText(message);
                cardPrBanner.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getEquipmentList().observe(this, this::renderEquipmentList);
    }

    private void renderEquipmentList(List<EquipmentWithMaxSet> items) {
        adapter.submitList(items);

        boolean empty = items == null || items.isEmpty();
        if (!empty) {
            rvEquipment.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            return;
        }

        rvEquipment.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);

        if (viewModel.hasActiveFilters()) {
            tvEmptyTitle.setText(R.string.empty_filter_title);
            tvEmptySubtitle.setText(R.string.empty_filter_subtitle);
            btnResetFilters.setVisibility(View.VISIBLE);
        } else {
            tvEmptyTitle.setText(R.string.empty_all_title);
            tvEmptySubtitle.setText(R.string.empty_all_subtitle);
            btnResetFilters.setVisibility(View.GONE);
        }
    }

    /**
     * Быстрый ввод подхода за два нажатия: заранее подставляю вес и повторения последнего подхода.
     */
    private void showQuickAddSetDialog(EquipmentWithMaxSet item) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_set, null);
        TextView tvPrefillNote = view.findViewById(R.id.tvPrefillNote);
        TextInputEditText etWeight = view.findViewById(R.id.etWeight);
        TextInputEditText etReps = view.findViewById(R.id.etReps);

        if (item.totalSets > 0 && item.lastWeightKg > 0f && item.lastReps > 0) {
            etWeight.setText(DateUtils.formatNumber(item.lastWeightKg));
            etReps.setText(String.valueOf(item.lastReps));
            tvPrefillNote.setVisibility(View.VISIBLE);
        } else {
            etWeight.setText("20");
            etReps.setText("10");
            tvPrefillNote.setVisibility(View.GONE);
        }

        wireSetSteppers(view, etWeight, etReps);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(item.name)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            float weight = parsePositiveFloat(etWeight);
            int reps = parsePositiveInt(etReps);
            if (weight <= 0f || reps <= 0) {
                Toast.makeText(this, R.string.error_invalid_set, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.addQuickSet(item.id, item.name, weight, reps);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void showAddEquipmentDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_equipment, null);
        TextInputLayout tilName = view.findViewById(R.id.tilEquipmentName);
        TextInputEditText etName = view.findViewById(R.id.etEquipmentName);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupAddMuscle);
        TextInputEditText etNotes = view.findViewById(R.id.etEquipmentNotes);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_equipment_title)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_add, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (name.isEmpty()) {
                tilName.setError(getString(R.string.error_empty_name));
                return;
            }
            tilName.setError(null);
            int checkedId = chipGroup.getCheckedChipId();
            Chip selectedChip = view.findViewById(checkedId);
            String muscleGroup = selectedChip != null
                    ? selectedChip.getText().toString()
                    : getString(R.string.group_chest);
            String notes = etNotes.getText() == null ? "" : etNotes.getText().toString().trim();

            viewModel.addEquipment(name, muscleGroup, notes);
            dialog.dismiss();
        }));
        dialog.show();
    }

    /**
     * Диалог настроек автоочистки: показываю живой счётчик устаревших записей из SQL
     * и даю возможность безопасно запустить очистку вручную после подтверждения.
     */
    private void showCleanupSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cleanup_settings, null);
        ChipGroup chipGroupRetention = view.findViewById(R.id.chipGroupRetention);
        TextView tvOldPreview = view.findViewById(R.id.tvOldRecordsPreview);
        TextView tvLastCleanup = view.findViewById(R.id.tvLastCleanupInfo);
        MaterialButton btnCleanupNow = view.findViewById(R.id.btnCleanupNow);

        int savedDays = CleanupPrefs.getRetentionDays(this);
        final int[] selectedDays = {savedDays};
        final int[] latestOldCount = {0};

        chipGroupRetention.check(chipIdForRetention(savedDays));
        updateLastCleanupLabel(tvLastCleanup);

        Runnable refreshPreview = () -> {
            tvOldPreview.setText(R.string.cleanup_preview_loading);
            viewModel.countOldSets(selectedDays[0], count -> {
                latestOldCount[0] = count;
                if (count > 0) {
                    tvOldPreview.setText(getString(R.string.cleanup_preview_found, selectedDays[0], count));
                } else {
                    tvOldPreview.setText(getString(R.string.cleanup_preview_none, selectedDays[0]));
                }
            });
        };
        refreshPreview.run();

        chipGroupRetention.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            selectedDays[0] = retentionDaysForChipId(checkedIds.get(0));
            refreshPreview.run();
        });

        btnCleanupNow.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cleanup_confirm_title)
                .setMessage(getString(R.string.cleanup_confirm_message, selectedDays[0], latestOldCount[0]))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_cleanup_now, (d, which) -> {
                    CleanupPrefs.setRetentionDays(this, selectedDays[0]);
                    viewModel.runManualCleanup(selectedDays[0], result -> {
                        Toast.makeText(
                                this,
                                getString(R.string.cleanup_done_toast, result.deletedSets, result.deletedFiles),
                                Toast.LENGTH_LONG
                        ).show();
                        updateLastCleanupLabel(tvLastCleanup);
                        refreshPreview.run();
                    });
                })
                .show());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cleanup_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, (d, which) -> {
                    CleanupPrefs.setRetentionDays(this, selectedDays[0]);
                    Toast.makeText(
                            this,
                            getString(R.string.cleanup_prefs_saved, selectedDays[0]),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .show();
    }

    private void updateLastCleanupLabel(TextView tv) {
        long lastMs = CleanupPrefs.getLastCleanupMs(this);
        if (lastMs <= 0L) {
            tv.setText(R.string.cleanup_never_run);
        } else {
            int deleted = CleanupPrefs.getLastDeletedCount(this);
            tv.setText(getString(R.string.cleanup_last_run_format, DateUtils.formatDateTime(lastMs), deleted));
        }
    }

    private int chipIdForRetention(int days) {
        if (days == 7) return R.id.chipRetention7;
        if (days == 90) return R.id.chipRetention90;
        if (days == 180) return R.id.chipRetention180;
        return R.id.chipRetention30;
    }

    private int retentionDaysForChipId(int chipId) {
        if (chipId == R.id.chipRetention7) return 7;
        if (chipId == R.id.chipRetention90) return 90;
        if (chipId == R.id.chipRetention180) return 180;
        return 30;
    }

    /**
     * Выгружаю историю в CSV на фоновом потоке и открываю системное меню «Поделиться» через FileProvider.
     */
    private void exportHistoryCsv() {
        viewModel.exportCsv(new WorkoutRepository.OnCsvExportCallback() {
            @Override
            public void onSuccess(File csvFile, int rowCount) {
                if (rowCount <= 0) {
                    Toast.makeText(MainActivity.this, R.string.csv_export_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".fileprovider",
                        csvFile
                );
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/csv");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.csv_share_chooser)));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.csv_export_error, message),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
            if (status != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    public static void wireSetSteppers(View dialogView, TextInputEditText etWeight, TextInputEditText etReps) {
        dialogView.findViewById(R.id.btnWeightMinus).setOnClickListener(v -> {
            float current = parsePositiveFloat(etWeight);
            float next = Math.max(2.5f, current - 2.5f);
            etWeight.setText(DateUtils.formatNumber(next));
        });
        dialogView.findViewById(R.id.btnWeightPlus).setOnClickListener(v -> {
            float current = Math.max(0f, parsePositiveFloat(etWeight));
            etWeight.setText(DateUtils.formatNumber(current + 2.5f));
        });
        dialogView.findViewById(R.id.btnRepsMinus).setOnClickListener(v -> {
            int current = parsePositiveInt(etReps);
            etReps.setText(String.valueOf(Math.max(1, current - 1)));
        });
        dialogView.findViewById(R.id.btnRepsPlus).setOnClickListener(v -> {
            int current = Math.max(0, parsePositiveInt(etReps));
            etReps.setText(String.valueOf(current + 1));
        });
    }

    public static float parsePositiveFloat(TextInputEditText input) {
        if (input.getText() == null) {
            return -1f;
        }
        String raw = input.getText().toString().trim().replace(',', '.');
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            return -1f;
        }
    }

    public static int parsePositiveInt(TextInputEditText input) {
        if (input.getText() == null) {
            return -1;
        }
        String raw = input.getText().toString().trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
