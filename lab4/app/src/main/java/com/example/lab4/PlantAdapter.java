package com.example.lab4;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * Адаптер каталога растений на базе ListAdapter и DiffUtil.
 * Поддерживаю два режима отображения (список и сетка) с переиспользованием карточек и загрузкой фото через Glide.
 */
public class PlantAdapter extends ListAdapter<PlantItem, PlantAdapter.PlantViewHolder> {

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    public interface OnPlantActionListener {
        void onPlantClick(PlantItem plant);

        void onPlantDeleteClick(PlantItem plant);
    }

    private final OnPlantActionListener listener;
    private boolean gridMode;

    public PlantAdapter(OnPlantActionListener listener, boolean gridMode) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.gridMode = gridMode;
    }

    public void setGridMode(boolean gridMode) {
        this.gridMode = gridMode;
    }

    @Override
    public int getItemViewType(int position) {
        return gridMode ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == VIEW_TYPE_GRID)
                ? R.layout.item_plant_grid
                : R.layout.item_plant_list;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        PlantItem plant = getItem(position);
        holder.bind(plant, listener);
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {

        final ImageView ivPhoto;
        final TextView tvName;
        final TextView tvSpecies;
        final TextView tvWatering;
        final TextView tvLight;
        final TextView tvFileSize;
        final View badgeCalendar;
        final ImageButton btnDelete;

        PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ищу элементы разметки один раз при создании ViewHolder, чтобы прокрутка была плавной
            ivPhoto = itemView.findViewById(R.id.ivPlantPhoto);
            tvName = itemView.findViewById(R.id.tvPlantName);
            tvSpecies = itemView.findViewById(R.id.tvPlantSpecies);
            tvWatering = itemView.findViewById(R.id.tvWateringBadge);
            tvLight = itemView.findViewById(R.id.tvLightBadge);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            badgeCalendar = itemView.findViewById(R.id.badgeCalendar);
            btnDelete = itemView.findViewById(R.id.btnDeletePlant);
        }

        void bind(PlantItem plant, OnPlantActionListener listener) {
            Context ctx = itemView.getContext();
            tvName.setText(plant.getName());
            tvSpecies.setText(plant.getSpecies().isEmpty() ? "Комнатное растение" : plant.getSpecies());
            tvWatering.setText(ctx.getString(R.string.badge_watering_format, plant.getWateringIntervalDays()));
            tvLight.setText(plant.getLightNeeds());

            if (plant.getFileSizeKb() > 0L) {
                tvFileSize.setVisibility(View.VISIBLE);
                tvFileSize.setText(ctx.getString(R.string.badge_size_format, plant.getFileSizeKb()));
            } else {
                tvFileSize.setVisibility(View.GONE);
            }

            badgeCalendar.setVisibility(plant.hasCalendarEvent() ? View.VISIBLE : View.GONE);

            // Загружаю локальную копию изображения с ограничением размера и заглушкой
            if (plant.hasImage()) {
                Glide.with(ivPhoto)
                        .load(new File(plant.getImagePath()))
                        .override(320, 320)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_plant)
                        .error(R.drawable.placeholder_plant)
                        .into(ivPhoto);
            } else {
                Glide.with(ivPhoto).clear(ivPhoto);
                ivPhoto.setImageResource(R.drawable.placeholder_plant);
            }

            // Делаю кликабельной всю карточку целиком
            itemView.setOnClickListener(v -> listener.onPlantClick(plant));
            btnDelete.setOnClickListener(v -> listener.onPlantDeleteClick(plant));
        }
    }

    private static final DiffUtil.ItemCallback<PlantItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull PlantItem oldItem, @NonNull PlantItem newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PlantItem oldItem, @NonNull PlantItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
