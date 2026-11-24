package com.example.lab6.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6.R;
import com.example.lab6.data.entity.Equipment;
import com.example.lab6.data.entity.WorkoutSet;
import com.example.lab6.ui.main.MainActivity;
import com.example.lab6.util.DateUtils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * Экран деталей тренажёра: история всех подходов от новых к старым,
 * быстрое добавление нового подхода с предзаполнением и безопасное удаление с подтверждением.
 */
public class EquipmentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EQUIPMENT_ID = "extra_equipment_id";

    private EquipmentDetailViewModel viewModel;
    private WorkoutSetAdapter adapter;

    private MaterialToolbar toolbar;
    private TextView tvDetailMuscle;
    private TextView tvDetailSetsTotal;
    private TextView tvDetailPr;
    private TextView tvDetailNotes;
    private MaterialCardView cardDetailPrBanner;
    private TextView tvDetailPrMessage;
    private RecyclerView rvSets;
    private TextView tvEmptySets;

    private Equipment currentEquipment;
    private WorkoutSet latestSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_detail);

        int equipmentId = getIntent().getIntExtra(EXTRA_EQUIPMENT_ID, -1);
        if (equipmentId == -1) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(EquipmentDetailViewModel.class);
        viewModel.init(equipmentId);

        bindViews();
        applyEdgeInsets();
        setupToolbar();
        setupRecyclerView();
        observeViewModel();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarDetail);
        tvDetailMuscle = findViewById(R.id.tvDetailMuscle);
        tvDetailSetsTotal = findViewById(R.id.tvDetailSetsTotal);
        tvDetailPr = findViewById(R.id.tvDetailPr);
        tvDetailNotes = findViewById(R.id.tvDetailNotes);
        cardDetailPrBanner = findViewById(R.id.cardDetailPrBanner);
        tvDetailPrMessage = findViewById(R.id.tvDetailPrMessage);
        rvSets = findViewById(R.id.rvSets);
        tvEmptySets = findViewById(R.id.tvEmptySets);

        ExtendedFloatingActionButton fabAddSet = findViewById(R.id.fabAddSet);
        fabAddSet.setOnClickListener(v -> showAddSetDialog());
    }

    private void applyEdgeInsets() {
        View root = findViewById(R.id.rootDetail);
        AppBarLayout appBar = findViewById(R.id.appBarDetail);
        ExtendedFloatingActionButton fab = findViewById(R.id.fabAddSet);

        final int baseFabBottom = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin;
        final int baseRvBottom = rvSets.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            rvSets.setPadding(
                    rvSets.getPaddingLeft(),
                    rvSets.getPaddingTop(),
                    rvSets.getPaddingRight(),
                    baseRvBottom + bars.bottom
            );
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            lp.bottomMargin = baseFabBottom + bars.bottom;
            fab.setLayoutParams(lp);
            return insets;
        });
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_equipment) {
                confirmDeleteEquipment();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new WorkoutSetAdapter(this::confirmDeleteSet);
        rvSets.setLayoutManager(new LinearLayoutManager(this));
        rvSets.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getEquipment().observe(this, eq -> {
            currentEquipment = eq;
            if (eq == null) {
                return;
            }
            toolbar.setTitle(eq.name);
            tvDetailMuscle.setText(eq.muscleGroup);
            if (eq.notes != null && !eq.notes.trim().isEmpty()) {
                tvDetailNotes.setText(eq.notes);
                tvDetailNotes.setVisibility(View.VISIBLE);
            } else {
                tvDetailNotes.setVisibility(View.GONE);
            }
        });

        viewModel.getWorkoutSets().observe(this, this::renderSets);

        viewModel.getPrMessage().observe(this, message -> {
            if (message == null || message.isEmpty()) {
                cardDetailPrBanner.setVisibility(View.GONE);
            } else {
                tvDetailPrMessage.setText(message);
                cardDetailPrBanner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderSets(List<WorkoutSet> sets) {
        int count = sets == null ? 0 : sets.size();
        tvDetailSetsTotal.setText(getString(R.string.equipment_sets_count, count));

        if (sets == null || sets.isEmpty()) {
            latestSet = null;
            adapter.setBestSetId(-1);
            adapter.submitList(null);
            tvDetailPr.setText(R.string.equipment_no_sets);
            rvSets.setVisibility(View.GONE);
            tvEmptySets.setVisibility(View.VISIBLE);
            return;
        }

        latestSet = sets.get(0);
        WorkoutSet best = findBestSet(sets);
        adapter.setBestSetId(best != null ? best.id : -1);
        adapter.submitList(sets);

        if (best != null) {
            tvDetailPr.setText(getString(
                    R.string.equipment_pr_format,
                    DateUtils.formatNumber(best.weightKg),
                    best.reps
            ));
        }

        rvSets.setVisibility(View.VISIBLE);
        tvEmptySets.setVisibility(View.GONE);
    }

    private WorkoutSet findBestSet(List<WorkoutSet> sets) {
        WorkoutSet best = null;
        for (WorkoutSet s : sets) {
            if (best == null
                    || s.weightKg > best.weightKg
                    || (Float.compare(s.weightKg, best.weightKg) == 0 && s.reps > best.reps)) {
                best = s;
            }
        }
        return best;
    }

    private void showAddSetDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_set, null);
        TextView tvPrefillNote = view.findViewById(R.id.tvPrefillNote);
        TextInputEditText etWeight = view.findViewById(R.id.etWeight);
        TextInputEditText etReps = view.findViewById(R.id.etReps);

        if (latestSet != null) {
            etWeight.setText(DateUtils.formatNumber(latestSet.weightKg));
            etReps.setText(String.valueOf(latestSet.reps));
            tvPrefillNote.setVisibility(View.VISIBLE);
        } else {
            etWeight.setText("20");
            etReps.setText("10");
            tvPrefillNote.setVisibility(View.GONE);
        }

        MainActivity.wireSetSteppers(view, etWeight, etReps);

        String title = currentEquipment != null
                ? currentEquipment.name
                : getString(R.string.dialog_add_set_title);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            float weight = MainActivity.parsePositiveFloat(etWeight);
            int reps = MainActivity.parsePositiveInt(etReps);
            if (weight <= 0f || reps <= 0) {
                Toast.makeText(this, R.string.error_invalid_set, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.addSet(weight, reps);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void confirmDeleteSet(WorkoutSet set) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.detail_delete_set_title)
                .setMessage(getString(
                        R.string.detail_delete_set_message,
                        DateUtils.formatNumber(set.weightKg),
                        set.reps
                ))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, which) -> viewModel.deleteSet(set))
                .show();
    }

    private void confirmDeleteEquipment() {
        String name = currentEquipment != null ? currentEquipment.name : "";
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.detail_delete_equipment_title)
                .setMessage(getString(R.string.detail_delete_equipment_message, name))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, which) -> viewModel.deleteEquipment(this::finish))
                .show();
    }
}
