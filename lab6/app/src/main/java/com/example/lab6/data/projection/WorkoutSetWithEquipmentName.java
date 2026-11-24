package com.example.lab6.data.projection;

import androidx.room.Ignore;

/**
 * Проекция подхода с названием тренажёра и группой мышц из INNER JOIN.
 * Использую для отображения лучшего общего рекорда и выгрузки полной истории в CSV.
 */
public class WorkoutSetWithEquipmentName {

    public int id;
    public int equipmentId;
    public float weightKg;
    public int reps;
    public long dateMs;
    public String equipmentName;
    public String muscleGroup;

    public WorkoutSetWithEquipmentName() {
    }

    @Ignore
    public WorkoutSetWithEquipmentName(
            int id,
            int equipmentId,
            float weightKg,
            int reps,
            long dateMs,
            String equipmentName,
            String muscleGroup
    ) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.weightKg = weightKg;
        this.reps = reps;
        this.dateMs = dateMs;
        this.equipmentName = equipmentName;
        this.muscleGroup = muscleGroup;
    }
}
