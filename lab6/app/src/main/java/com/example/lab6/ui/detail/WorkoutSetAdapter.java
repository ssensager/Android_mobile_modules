package com.example.lab6.ui.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6.R;
import com.example.lab6.data.entity.WorkoutSet;
import com.example.lab6.util.DateUtils;

/**
 * Адаптер истории подходов конкретного тренажёра.
 * Подсвечиваю рекордный подход бейджем, чтобы прогресс читался с первого взгляда.
 */
public class WorkoutSetAdapter extends ListAdapter<WorkoutSet, WorkoutSetAdapter.ViewHolder> {

    public interface OnDeleteSetListener {
        void onDeleteClick(WorkoutSet set);
    }

    private static final DiffUtil.ItemCallback<WorkoutSet> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WorkoutSet>() {
                @Override
                public boolean areItemsTheSame(@NonNull WorkoutSet oldItem, @NonNull WorkoutSet newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull WorkoutSet oldItem, @NonNull WorkoutSet newItem) {
                    return oldItem.id == newItem.id
                            && Float.compare(oldItem.weightKg, newItem.weightKg) == 0
                            && oldItem.reps == newItem.reps
                            && oldItem.dateMs == newItem.dateMs;
                }
            };

    private final OnDeleteSetListener deleteListener;
    private int bestSetId = -1;

    public WorkoutSetAdapter(OnDeleteSetListener deleteListener) {
        super(DIFF_CALLBACK);
        this.deleteListener = deleteListener;
    }

    public void setBestSetId(int bestSetId) {
        if (this.bestSetId != bestSetId) {
            this.bestSetId = bestSetId;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_set, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutSet item = getItem(position);
        holder.bind(item, item.id == bestSetId, deleteListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSetMain;
        private final TextView tvPrBadge;
        private final TextView tvSetMeta;
        private final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSetMain = itemView.findViewById(R.id.tvSetMain);
            tvPrBadge = itemView.findViewById(R.id.tvPrBadge);
            tvSetMeta = itemView.findViewById(R.id.tvSetMeta);
            btnDelete = itemView.findViewById(R.id.btnDeleteSet);
        }

        void bind(WorkoutSet set, boolean isBest, OnDeleteSetListener listener) {
            Context ctx = itemView.getContext();
            String weightStr = DateUtils.formatNumber(set.weightKg);
            String volumeStr = DateUtils.formatNumber(set.weightKg * set.reps);

            tvSetMain.setText(ctx.getString(R.string.detail_set_main, weightStr, set.reps));
            tvPrBadge.setVisibility(isBest ? View.VISIBLE : View.GONE);

            String volText = ctx.getString(R.string.detail_set_volume, volumeStr);
            String dateText = DateUtils.formatDateTime(set.dateMs);
            tvSetMeta.setText(volText + "  •  " + dateText);

            btnDelete.setOnClickListener(v -> listener.onDeleteClick(set));
        }
    }
}
