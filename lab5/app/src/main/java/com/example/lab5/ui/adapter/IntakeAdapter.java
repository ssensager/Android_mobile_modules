package com.example.lab5.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab5.R;
import com.example.lab5.data.entity.Intake;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Показываю список запланированных и выполненных приёмов препарата с быстрой отметкой «Принято».
public class IntakeAdapter extends RecyclerView.Adapter<IntakeAdapter.IntakeViewHolder> {

    public interface Listener {
        void onToggleTaken(@NonNull Intake intake);

        void onDeleteIntake(@NonNull Intake intake);
    }

    private final Listener listener;
    private final List<Intake> items = new ArrayList<>();
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd MMM, HH:mm", new Locale("ru"));

    public IntakeAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Intake> newItems) {
        items.clear();
        items.addAll(newItems);
        // Сортирую свежие и ближайшие приёмы наверх списка.
        Collections.sort(items, (a, b) -> Long.compare(b.scheduledAtMs, a.scheduledAtMs));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IntakeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_intake, parent, false);
        return new IntakeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IntakeViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class IntakeViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTime;
        private final TextView tvStatus;
        private final MaterialButton btnToggle;
        private final ImageButton btnDelete;

        IntakeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvIntakeTime);
            tvStatus = itemView.findViewById(R.id.tvIntakeStatus);
            btnToggle = itemView.findViewById(R.id.btnToggleTaken);
            btnDelete = itemView.findViewById(R.id.btnDeleteIntake);
        }

        void bind(@NonNull Intake intake) {
            Context context = itemView.getContext();
            tvTime.setText(formatter.format(new Date(intake.scheduledAtMs)));

            if (intake.isTaken) {
                tvStatus.setText(R.string.status_taken);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.risk_safe_fg));
                ViewCompat.setBackgroundTintList(
                        tvStatus,
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.risk_safe_bg))
                );
                btnToggle.setText(R.string.action_mark_pending);
            } else {
                tvStatus.setText(R.string.status_planned);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.risk_medium_fg));
                ViewCompat.setBackgroundTintList(
                        tvStatus,
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.risk_medium_bg))
                );
                btnToggle.setText(R.string.action_mark_taken);
            }

            btnToggle.setOnClickListener(v -> listener.onToggleTaken(intake));
            btnDelete.setOnClickListener(v -> listener.onDeleteIntake(intake));
        }
    }
}
