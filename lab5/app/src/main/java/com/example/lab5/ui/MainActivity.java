package com.example.lab5.ui;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab5.R;
import com.example.lab5.data.entity.ActiveSubstance;
import com.example.lab5.data.entity.ConflictRule;
import com.example.lab5.data.entity.Intake;
import com.example.lab5.data.entity.Medicine;
import com.example.lab5.data.entity.MedicineWithDetails;
import com.example.lab5.domain.RiskSummary;
import com.example.lab5.notification.NotificationHelper;
import com.example.lab5.ui.adapter.MedicineAdapter;
import com.example.lab5.ui.view.RiskGaugeView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Главный экран аптечки: постоянный баннер конфликтов, шкала риска на Canvas и список препаратов.
public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private MedicineAdapter adapter;

    private View rootCoordinator;
    private MaterialCardView cardPermissionHint;
    private MaterialCardView cardRiskBanner;
    private ImageView ivBannerIcon;
    private TextView tvBannerTitle;
    private TextView tvBannerSubtitle;
    private LinearLayout layoutConflictList;
    private RiskGaugeView riskGaugeView;
    private TextView tvMedicineCount;
    private RecyclerView recyclerMedicines;
    private View layoutEmptyState;

    private List<ActiveSubstance> cachedSubstances = Collections.emptyList();
    private List<ConflictRule> cachedRules = Collections.emptyList();
    private List<MedicineWithDetails> cachedMedicines = Collections.emptyList();
    private RiskSummary cachedRisk = RiskSummary.empty();

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                updatePermissionHintVisibility();
                if (granted) {
                    Toast.makeText(this, R.string.permission_granted_toast, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Создаю канал уведомлений один раз при старте приложения.
        NotificationHelper.createChannel(this);

        bindViews();
        applyEdgeToEdgeInsets();
        setupToolbar();
        setupRecycler();
        setupViewModel();
        checkNotificationPermissionOnLaunch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionHintVisibility();
    }

    private void bindViews() {
        rootCoordinator = findViewById(R.id.rootCoordinator);
        cardPermissionHint = findViewById(R.id.cardPermissionHint);
        cardRiskBanner = findViewById(R.id.cardRiskBanner);
        ivBannerIcon = findViewById(R.id.ivBannerIcon);
        tvBannerTitle = findViewById(R.id.tvBannerTitle);
        tvBannerSubtitle = findViewById(R.id.tvBannerSubtitle);
        layoutConflictList = findViewById(R.id.layoutConflictList);
        riskGaugeView = findViewById(R.id.riskGaugeView);
        tvMedicineCount = findViewById(R.id.tvMedicineCount);
        recyclerMedicines = findViewById(R.id.recyclerMedicines);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        MaterialButton btnEnableNotifications = findViewById(R.id.btnEnableNotifications);
        btnEnableNotifications.setOnClickListener(v -> requestNotificationsWithRationale());

        MaterialButton btnEmptyAdd = findViewById(R.id.btnEmptyAdd);
        btnEmptyAdd.setOnClickListener(v -> openMedicineDialog(null));

        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fabAddMedicine);
        fabAdd.setOnClickListener(v -> openMedicineDialog(null));
    }

    private void applyEdgeToEdgeInsets() {
        AppBarLayout appBar = findViewById(R.id.appBarLayout);
        View scrollContent = findViewById(R.id.scrollContent);
        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fabAddMedicine);

        int baseScrollBottom = scrollContent.getPaddingBottom();
        int baseFabMargin = Math.round(16f * getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(rootCoordinator, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(0, systemBars.top, 0, 0);
            scrollContent.setPadding(
                    scrollContent.getPaddingLeft(),
                    scrollContent.getPaddingTop(),
                    scrollContent.getPaddingRight(),
                    baseScrollBottom + systemBars.bottom
            );
            ViewGroupMarginHelper.setBottomMargin(fabAdd, baseFabMargin + systemBars.bottom);
            return insets;
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_rules) {
                showRulesReferenceDialog();
                return true;
            }
            if (id == R.id.action_notifications) {
                if (NotificationHelper.hasNotificationPermission(this)) {
                    Toast.makeText(this, R.string.permission_granted_toast, Toast.LENGTH_SHORT).show();
                } else {
                    requestNotificationsWithRationale();
                }
                return true;
            }
            return false;
        });
    }

    private void setupRecycler() {
        adapter = new MedicineAdapter(new MedicineAdapter.Listener() {
            @Override
            public void onMedicineClick(@NonNull MedicineWithDetails item) {
                Intent intent = new Intent(MainActivity.this, MedicineDetailActivity.class);
                intent.putExtra(MedicineDetailActivity.EXTRA_MEDICINE_ID, item.medicine.id);
                startActivity(intent);
            }

            @Override
            public void onEditClick(@NonNull MedicineWithDetails item) {
                openMedicineDialog(item.medicine);
            }

            @Override
            public void onDeleteClick(@NonNull MedicineWithDetails item) {
                confirmDeleteMedicine(item);
            }
        });
        recyclerMedicines.setLayoutManager(new LinearLayoutManager(this));
        recyclerMedicines.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getSubstances().observe(this, list -> {
            cachedSubstances = list != null ? list : Collections.emptyList();
        });

        viewModel.getConflictRules().observe(this, rules -> {
            cachedRules = rules != null ? rules : Collections.emptyList();
        });

        viewModel.getMedicinesWithDetails().observe(this, medicines -> {
            cachedMedicines = medicines != null ? medicines : Collections.emptyList();
            renderMedicinesList();
        });

        viewModel.getRiskSummary().observe(this, summary -> {
            cachedRisk = summary != null ? summary : RiskSummary.empty();
            renderRiskBanner(cachedRisk);
            riskGaugeView.setRiskSummary(cachedRisk);
            renderMedicinesList();
        });
    }

    private void renderMedicinesList() {
        boolean isEmpty = cachedMedicines.isEmpty();
        tvMedicineCount.setText(cachedMedicines.size() + " шт.");
        recyclerMedicines.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        adapter.submitData(cachedMedicines, cachedRisk.conflicts);
    }

    private void renderRiskBanner(@NonNull RiskSummary summary) {
        int fg = ConflictUiHelper.foregroundColor(this, summary.overallLevel);
        int bg = ConflictUiHelper.backgroundColor(this, summary.overallLevel);
        int border = ConflictUiHelper.borderColor(this, summary.overallLevel);

        cardRiskBanner.setCardBackgroundColor(bg);
        cardRiskBanner.setStrokeColor(border);

        ivBannerIcon.setImageResource(ConflictUiHelper.iconRes(summary.overallLevel));
        ivBannerIcon.setImageTintList(ColorStateList.valueOf(fg));

        tvBannerTitle.setText(ConflictUiHelper.bannerTitle(this, summary.overallLevel));
        tvBannerTitle.setTextColor(fg);

        if (summary.hasConflicts()) {
            tvBannerSubtitle.setText("Найдено взаимодействий: " + summary.conflicts.size()
                    + " · индекс риска " + summary.riskScore + "/100");
        } else {
            tvBannerSubtitle.setText(R.string.banner_safe_desc);
        }

        ConflictUiHelper.populateConflictRows(this, layoutConflictList, summary.conflicts);
    }

    private void openMedicineDialog(@Nullable Medicine existing) {
        if (cachedSubstances.isEmpty()) {
            return;
        }
        MedicineDialogHelper.show(this, cachedSubstances, existing, (medicine, isEdit) -> {
            if (isEdit) {
                viewModel.updateMedicine(medicine);
            } else {
                viewModel.insertMedicine(medicine, null);
            }
        });
    }

    private void confirmDeleteMedicine(@NonNull MedicineWithDetails item) {
        int intakeCount = item.getTotalIntakesCount();
        String message = getString(R.string.delete_dialog_message, item.medicine.name, intakeCount);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    // Делаю снимок препарата и его приёмов до каскадного удаления, чтобы работала отмена.
                    Medicine snapshotMedicine = item.medicine;
                    List<Intake> snapshotIntakes = item.intakes != null
                            ? new ArrayList<>(item.intakes)
                            : Collections.emptyList();

                    viewModel.deleteMedicine(snapshotMedicine);

                    Snackbar.make(
                                    rootCoordinator,
                                    getString(R.string.snackbar_deleted, snapshotMedicine.name),
                                    Snackbar.LENGTH_LONG
                            )
                            .setAction(R.string.action_undo, v -> {
                                viewModel.restoreMedicineWithIntakes(snapshotMedicine, snapshotIntakes);
                                Toast.makeText(this, R.string.snackbar_restored, Toast.LENGTH_SHORT).show();
                            })
                            .show();
                })
                .show();
    }

    private void showRulesReferenceDialog() {
        Map<Integer, String> substanceNames = new HashMap<>();
        for (ActiveSubstance sub : cachedSubstances) {
            substanceNames.put(sub.id, sub.name);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cachedRules.size(); i++) {
            ConflictRule rule = cachedRules.get(i);
            String subA = substanceNames.getOrDefault(rule.substanceAId, "#" + rule.substanceAId);
            String subB = substanceNames.getOrDefault(rule.substanceBId, "#" + rule.substanceBId);
            String level = ConflictUiHelper.severityLabel(this, rule.severity);

            if (i > 0) {
                builder.append("\n\n");
            }
            builder.append("[").append(level).append("] ")
                    .append(subA).append(" + ").append(subB)
                    .append("\n").append(rule.title)
                    .append("\n").append(rule.recommendation);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rules_dialog_title)
                .setMessage(builder.length() > 0 ? builder.toString() : "Справочник загружается...")
                .setPositiveButton(R.string.rules_dialog_close, null)
                .show();
    }

    private void checkNotificationPermissionOnLaunch() {
        updatePermissionHintVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationHelper.hasNotificationPermission(this)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                requestNotificationsWithRationale();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void requestNotificationsWithRationale() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_rationale_title)
                .setMessage(R.string.permission_rationale_message)
                .setNegativeButton(R.string.action_later, null)
                .setPositiveButton(R.string.action_allow_notifications, (d, w) ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                .show();
    }

    private void updatePermissionHintVisibility() {
        boolean denied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationHelper.hasNotificationPermission(this);
        cardPermissionHint.setVisibility(denied ? View.VISIBLE : View.GONE);
    }

    // Аккуратно обновляю нижний отступ FAB под навигационную панель при EdgeToEdge.
    private static final class ViewGroupMarginHelper {
        static void setBottomMargin(@NonNull View view, int bottomMarginPx) {
            if (view.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams params =
                        (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
                params.bottomMargin = bottomMarginPx;
                view.setLayoutParams(params);
            }
        }
    }
}
