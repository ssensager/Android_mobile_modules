package com.example.lab6.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6.R;
import com.example.lab6.data.projection.EquipmentWithMaxSet;
import com.example.lab6.util.DateUtils;
import com.google.android.material.button.MaterialButton;

/**
 * Адаптер списка тренажёров на базе ListAdapter + DiffUtil.
 * Обновляю только изменившиеся карточки без мерцания всего RecyclerView.
 */
public class EquipmentAdapter extends ListAdapter<EquipmentWithMaxSet, EquipmentAdapter.ViewHolder> {

    public interface OnEquipmentInteractionListener {
        void onEquipmentClick(EquipmentWithMaxSet item);

        void onQuickAddSetClick(EquipmentWithMaxSet item);
    }

    private static final DiffUtil.ItemCallback<EquipmentWithMaxSet> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<EquipmentWithMaxSet>() {
                @Override
                public boolean areItemsTheSame(@NonNull EquipmentWithMaxSet oldItem, @NonNull EquipmentWithMaxSet newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull EquipmentWithMaxSet oldItem, @NonNull EquipmentWithMaxSet newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnEquipmentInteractionListener listener;

    public EquipmentAdapter(OnEquipmentInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvMuscleBadge;
        private final TextView tvRecord;
        private final TextView tvMeta;
        private final MaterialButton btnQuickAdd;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEquipmentName);
            tvMuscleBadge = itemView.findViewById(R.id.tvMuscleBadge);
            tvRecord = itemView.findViewById(R.id.tvRecord);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnQuickAdd = itemView.findViewById(R.id.btnQuickAddSet);
        }

        void bind(EquipmentWithMaxSet item, OnEquipmentInteractionListener listener) {
            Context ctx = itemView.getContext();
            tvName.setText(item.name);
            tvMuscleBadge.setText(item.muscleGroup);

            if (item.totalSets > 0 && item.maxWeightKg > 0f) {
                tvRecord.setText(ctx.getString(
                        R.string.equipment_pr_format,
                        DateUtils.formatNumber(item.maxWeightKg),
                        item.maxWeightReps
                ));
                String setsText = ctx.getString(R.string.equipment_sets_count, item.totalSets);
                String lastText = ctx.getString(
                        R.string.equipment_last_set,
                        DateUtils.formatNumber(item.lastWeightKg),
                        item.lastReps
                );
                tvMeta.setText(setsText + "  •  " + lastText);
            } else {
                tvRecord.setText(R.string.equipment_no_sets);
                tvMeta.setText(ctx.getString(R.string.equipment_sets_count, 0));
            }

            itemView.setOnClickListener(v -> listener.onEquipmentClick(item));
            btnQuickAdd.setOnClickListener(v -> listener.onQuickAddSetClick(item));
        }
    }
}
