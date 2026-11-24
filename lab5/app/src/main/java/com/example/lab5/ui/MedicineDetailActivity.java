package com.example.lab5.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab5.R;
import com.example.lab5.data.entity.ActiveSubstance;
import com.example.lab5.data.entity.Intake;
import com.example.lab5.data.entity.MedicineWithDetails;
import com.example.lab5.domain.DetectedConflict;
import com.example.lab5.domain.RiskSummary;
import com.example.lab5.notification.NotificationHelper;
import com.example.lab5.ui.adapter.IntakeAdapter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.List;

// Экран деталей препарата: конфликты, планирование напоминаний WorkManager и история приёмов.
public class MedicineDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MEDICINE_ID = "extra_medicine_id";

    private MainViewModel viewModel;
    private IntakeAdapter intakeAdapter;
    private int medicineId = -1;

    private TextView tvDetailName;
    private TextView tvDetailSubstance;
    private TextView tvDetailDosage;
    private TextView tvDetailSubstanceDesc;
    private TextView tvDetailSchedule;
    private TextView tvDetailNotes;

    private MaterialCardView cardDetailConflict;
    private ImageView ivDetailConflictIcon;
    private TextView tvDetailConflictTitle;
    private TextView tvDetailConflictSafe;
    private LinearLayout layoutDetailConflicts;

    private ChipGroup chipGroupDelay;
    private TextInputEditText etCustomMinutes;
    private RecyclerView recyclerIntakes;
    private TextView tvEmptyIntakes;

    private MedicineWithDetails currentMedicine;
    private List<ActiveSubstance> cachedSubstances = Collections.emptyList();
    private RiskSummary cachedRisk = RiskSummary.empty();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_detail);

        medicineId = getIntent().getIntExtra(EXTRA_MEDICINE_ID, -1);
        if (medicineId <= 0) {
            finish();
            return;
        }

        NotificationHelper.createChannel(this);

        bindViews();
        applyInsets();
        setupToolbar();
        setupRecycler();
        setupActions();
        setupViewModel();
    }

    private void bindViews() {
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailSubstance = findViewById(R.id.tvDetailSubstance);
        tvDetailDosage = findViewById(R.id.tvDetailDosage);
        tvDetailSubstanceDesc = findViewById(R.id.tvDetailSubstanceDesc);
        tvDetailSchedule = findViewById(R.id.tvDetailSchedule);
        tvDetailNotes = findViewById(R.id.tvDetailNotes);

        cardDetailConflict = findViewById(R.id.cardDetailConflict);
        ivDetailConflictIcon = findViewById(R.id.ivDetailConflictIcon);
        tvDetailConflictTitle = findViewById(R.id.tvDetailConflictTitle);
        tvDetailConflictSafe = findViewById(R.id.tvDetailConflictSafe);
        layoutDetailConflicts = findViewById(R.id.layoutDetailConflicts);

        chipGroupDelay = findViewById(R.id.chipGroupDelay);
        etCustomMinutes = findViewById(R.id.etCustomMinutes);
        recyclerIntakes = findViewById(R.id.recyclerIntakes);
        tvEmptyIntakes = findViewById(R.id.tvEmptyIntakes);
    }

    private void applyInsets() {
        View root = findViewById(R.id.detailRoot);
        AppBarLayout appBar = findViewById(R.id.detailAppBar);
        View scroll = findViewById(R.id.detailScroll);
        int baseBottom = scroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(0, systemBars.top, 0, 0);
            scroll.setPadding(
                    scroll.getPaddingLeft(),
                    scroll.getPaddingTop(),
                    scroll.getPaddingRight(),
                    baseBottom + systemBars.bottom
            );
            return insets;
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.detailToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        intakeAdapter = new IntakeAdapter(new IntakeAdapter.Listener() {
            @Override
            public void onToggleTaken(@NonNull Intake intake) {
                viewModel.markIntakeTaken(intake.id, !intake.isTaken);
            }

            @Override
            public void onDeleteIntake(@NonNull Intake intake) {
                viewModel.deleteIntake(intake);
            }
        });
        recyclerIntakes.setLayoutManager(new LinearLayoutManager(this));
        recyclerIntakes.setAdapter(intakeAdapter);
    }

    private void setupActions() {
        MaterialButton btnEdit = findViewById(R.id.btnDetailEdit);
        btnEdit.setOnClickListener(v -> {
            if (currentMedicine != null && !cachedSubstances.isEmpty()) {
                MedicineDialogHelper.show(this, cachedSubstances, currentMedicine.medicine, (med, isEdit) ->
                        viewModel.updateMedicine(med));
            }
        });

        MaterialButton btnDelete = findViewById(R.id.btnDetailDelete);
        btnDelete.setOnClickListener(v -> confirmDelete());

        MaterialButton btnSchedule = findViewById(R.id.btnScheduleReminder);
        btnSchedule.setOnClickListener(v -> scheduleCustomReminder());

        MaterialButton btnTest5s = findViewById(R.id.btnTest5sReminder);
        btnTest5s.setOnClickListener(v -> scheduleIntakeAndReminder(5L, "5 сек"));

        MaterialButton btnAddNow = findViewById(R.id.btnAddIntakeNow);
        btnAddNow.setOnClickListener(v -> {
            if (currentMedicine == null) {
                return;
            }
            Intake intake = new Intake(currentMedicine.medicine.id, System.currentTimeMillis(), true);
            viewModel.insertIntake(intake, null);
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getSubstances().observe(this, list ->
                cachedSubstances = list != null ? list : Collections.emptyList());

        viewModel.getMedicineWithDetails(medicineId).observe(this, details -> {
            if (details == null || details.medicine == null) {
                finish();
                return;
            }
            currentMedicine = details;
            renderMedicine(details);
            renderConflictsForCurrent();
        });

        viewModel.getRiskSummary().observe(this, summary -> {
            cachedRisk = summary != null ? summary : RiskSummary.empty();
            renderConflictsForCurrent();
        });
    }

    private void renderMedicine(@NonNull MedicineWithDetails details) {
        tvDetailName.setText(details.medicine.name);
        tvDetailDosage.setText(details.medicine.dosage);

        String subName = details.substance != null ? details.substance.name : "—";
        String subDesc = details.substance != null ? details.substance.description : "";
        tvDetailSubstance.setText(subName);
        ViewCompat.setBackgroundTintList(
                tvDetailSubstance,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary_container))
        );
        tvDetailSubstanceDesc.setText(subDesc);
        tvDetailSubstanceDesc.setVisibility(subDesc.isEmpty() ? View.GONE : View.VISIBLE);

        String schedule = details.medicine.scheduleTime.trim().isEmpty()
                ? getString(R.string.schedule_empty)
                : "Расписание: " + details.medicine.scheduleTime;
        tvDetailSchedule.setText(schedule);

        String notes = details.medicine.notes.trim();
        tvDetailNotes.setText(notes);
        tvDetailNotes.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);

        List<Intake> intakes = details.intakes != null ? details.intakes : Collections.emptyList();
        intakeAdapter.submitList(intakes);
        tvEmptyIntakes.setVisibility(intakes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderConflictsForCurrent() {
        if (currentMedicine == null) {
            return;
        }
        int substanceId = currentMedicine.medicine.substanceId;
        List<DetectedConflict> related = viewModel.getInteractionChecker()
                .getConflictsForSubstance(substanceId, cachedRisk.conflicts);
        String highestSeverity = viewModel.getInteractionChecker()
                .getHighestSeverityForSubstance(substanceId, cachedRisk.conflicts);

        int fg = ConflictUiHelper.foregroundColor(this, highestSeverity);
        int bg = ConflictUiHelper.backgroundColor(this, highestSeverity);
        int border = ConflictUiHelper.borderColor(this, highestSeverity);

        cardDetailConflict.setCardBackgroundColor(bg);
        cardDetailConflict.setStrokeColor(border);
        ivDetailConflictIcon.setImageResource(ConflictUiHelper.iconRes(highestSeverity));
        ivDetailConflictIcon.setImageTintList(ColorStateList.valueOf(fg));
        tvDetailConflictTitle.setTextColor(fg);

        if (related.isEmpty()) {
            tvDetailConflictTitle.setText(R.string.banner_safe_title);
            tvDetailConflictSafe.setVisibility(View.VISIBLE);
            layoutDetailConflicts.setVisibility(View.GONE);
        } else {
            tvDetailConflictTitle.setText(ConflictUiHelper.bannerTitle(this, highestSeverity));
            tvDetailConflictSafe.setVisibility(View.GONE);
            ConflictUiHelper.populateConflictRows(this, layoutDetailConflicts, related);
        }
    }

    private void scheduleCustomReminder() {
        long delayMinutes = 1L;
        String customRaw = etCustomMinutes.getText() != null ? etCustomMinutes.getText().toString().trim() : "";

        if (!customRaw.isEmpty()) {
            try {
                long parsed = Long.parseLong(customRaw);
                if (parsed > 0 && parsed <= 10080) {
                    delayMinutes = parsed;
                }
            } catch (NumberFormatException ignored) {
                delayMinutes = 1L;
            }
        } else {
            int checkedId = chipGroupDelay.getCheckedChipId();
            if (checkedId == R.id.chipDelay15m) {
                delayMinutes = 15L;
            } else if (checkedId == R.id.chipDelay60m) {
                delayMinutes = 60L;
            } else {
                delayMinutes = 1L;
            }
        }

        scheduleIntakeAndReminder(delayMinutes * 60L, delayMinutes + " мин");
    }

    // Сначала записываю запланированный приём в Room, а затем ставлю задачу WorkManager с этим intakeId.
    private void scheduleIntakeAndReminder(long delaySeconds, @NonNull String readableDelay) {
        if (currentMedicine == null) {
            return;
        }
        long scheduledAt = System.currentTimeMillis() + delaySeconds * 1000L;
        Intake planned = new Intake(currentMedicine.medicine.id, scheduledAt, false);

        String medName = currentMedicine.medicine.name;
        String dosage = currentMedicine.medicine.dosage;
        String subName = currentMedicine.substance != null ? currentMedicine.substance.name : "";

        viewModel.insertIntake(planned, intakeId -> {
            NotificationHelper.scheduleReminder(
                    getApplicationContext(),
                    currentMedicine.medicine.id,
                    intakeId,
                    medName,
                    dosage,
                    subName,
                    delaySeconds
            );
            runOnUiThread(() -> {
                if (NotificationHelper.hasNotificationPermission(this)) {
                    Toast.makeText(
                            this,
                            getString(R.string.reminder_scheduled_format, readableDelay),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(this, R.string.reminder_saved_without_perm, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void confirmDelete() {
        if (currentMedicine == null) {
            return;
        }
        String message = getString(
                R.string.delete_dialog_message,
                currentMedicine.medicine.name,
                currentMedicine.getTotalIntakesCount()
        );
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    viewModel.deleteMedicine(currentMedicine.medicine);
                    finish();
                })
                .show();
    }
}
