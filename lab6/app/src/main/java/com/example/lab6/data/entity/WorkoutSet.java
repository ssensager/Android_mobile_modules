package com.example.lab6.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Сущность одного выполненного подхода.
 * Связал с Equipment через CASCADE: если удалить тренажёр, его подходы удалятся автоматически.
 * Индекс по equipmentId ускоряет JOIN, а индекс по dateMs — выборки за сегодня, график и автоочистку.
 */
@Entity(
        tableName = "workout_sets",
        foreignKeys = @ForeignKey(
                entity = Equipment.class,
                parentColumns = "id",
                childColumns = "equipmentId",
                onDelete = ForeignKey.CASCADE
        )
)
public class WorkoutSet {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(index = true)
    public int equipmentId;

    public float weightKg;

    public int reps;

    @ColumnInfo(index = true)
    public long dateMs;

    public WorkoutSet() {
    }

    @Ignore
    public WorkoutSet(int equipmentId, float weightKg, int reps, long dateMs) {
        this.equipmentId = equipmentId;
        this.weightKg = weightKg;
        this.reps = reps;
        this.dateMs = dateMs;
    }
}
