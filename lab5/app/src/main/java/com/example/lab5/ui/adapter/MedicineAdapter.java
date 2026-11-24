package com.example.lab5.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab5.R;
import com.example.lab5.data.entity.MedicineWithDetails;
import com.example.lab5.domain.DetectedConflict;
import com.example.lab5.domain.InteractionChecker;
import com.example.lab5.ui.ConflictUiHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Адаптер списка препаратов показывает вещество, дозировку, прогресс приёмов и персональный бейдж риска.
public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    public interface Listener {
        void onMedicineClick(@NonNull MedicineWithDetails item);

        void onEditClick(@NonNull MedicineWithDetails item);

        void onDeleteClick(@NonNull MedicineWithDetails item);
    }

    private final Listener listener;
    private final InteractionChecker checker = new InteractionChecker();
    private final List<MedicineWithDetails> items = new ArrayList<>();
    private List<DetectedConflict> conflicts = Collections.emptyList();

    public MedicineAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitData(
            @NonNull List<MedicineWithDetails> newItems,
            @NonNull List<DetectedConflict> newConflicts
    ) {
        items.clear();
        items.addAll(newItems);
        conflicts = newConflicts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class MedicineViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView tvName;
        private final TextView tvDosage;
        private final TextView tvSubstanceBadge;
        private final LinearLayout layoutRiskPill;
        private final ImageView ivRiskPillIcon;
        private final TextView tvRiskPillText;
        private final TextView tvScheduleInfo;
        private final TextView tvIntakeCount;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardMedicine);
            tvName = itemView.findViewById(R.id.tvMedicineName);
            tvDosage = itemView.findViewById(R.id.tvMedicineDosage);
            tvSubstanceBadge = itemView.findViewById(R.id.tvSubstanceBadge);
            layoutRiskPill = itemView.findViewById(R.id.layoutRiskPill);
            ivRiskPillIcon = itemView.findViewById(R.id.ivRiskPillIcon);
            tvRiskPillText = itemView.findViewById(R.id.tvRiskPillText);
            tvScheduleInfo = itemView.findViewById(R.id.tvScheduleInfo);
            tvIntakeCount = itemView.findViewById(R.id.tvIntakeCount);
            btnEdit = itemView.findViewById(R.id.btnEditMedicine);
            btnDelete = itemView.findViewById(R.id.btnDeleteMedicine);
        }

        void bind(@NonNull MedicineWithDetails item) {
            Context context = itemView.getContext();
            tvName.setText(item.medicine.name);
            tvDosage.setText(item.medicine.dosage);

            String substanceName = item.substance != null ? item.substance.name : "—";
            tvSubstanceBadge.setText(substanceName);
            ViewCompat.setBackgroundTintList(
                    tvSubstanceBadge,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary_container))
            );

            String severity = checker.getHighestSeverityForSubstance(item.medicine.substanceId, conflicts);
            int fg = ConflictUiHelper.foregroundColor(context, severity);
            int bg = ConflictUiHelper.backgroundColor(context, severity);

            ivRiskPillIcon.setImageResource(ConflictUiHelper.iconRes(severity));
            ivRiskPillIcon.setImageTintList(ColorStateList.valueOf(fg));
            tvRiskPillText.setText(ConflictUiHelper.severityLabel(context, severity));
            tvRiskPillText.setTextColor(fg);
            ViewCompat.setBackgroundTintList(layoutRiskPill, ColorStateList.valueOf(bg));

            String schedule = item.medicine.scheduleTime.trim().isEmpty()
                    ? context.getString(R.string.schedule_empty)
                    : "График: " + item.medicine.scheduleTime;
            tvScheduleInfo.setText(schedule);

            tvIntakeCount.setText(context.getString(
                    R.string.intake_summary_format,
                    item.getTakenCount(),
                    item.getTotalIntakesCount()
            ));

            card.setOnClickListener(v -> listener.onMedicineClick(item));
            btnEdit.setOnClickListener(v -> listener.onEditClick(item));
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
        }
    }
}
