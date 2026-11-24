package com.example.lab5.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// Приёмы привязаны к препарату с каскадным удалением, чтобы в базе не оставались записи-сироты.
@Entity(
        tableName = "intake",
        foreignKeys = @ForeignKey(
                entity = Medicine.class,
                parentColumns = "id",
                childColumns = "medicineId",
                onDelete = ForeignKey.CASCADE
        )
)
public class Intake {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Сделал индекс на внешнем ключе, чтобы выборка всех приёмов препарата работала быстро.
    @ColumnInfo(index = true)
    public int medicineId;

    public long scheduledAtMs;

    public boolean isTaken;

    public Intake(int medicineId, long scheduledAtMs, boolean isTaken) {
        this.medicineId = medicineId;
        this.scheduledAtMs = scheduledAtMs;
        this.isTaken = isTaken;
    }

    @Ignore
    public Intake(int id, int medicineId, long scheduledAtMs, boolean isTaken) {
        this.id = id;
        this.medicineId = medicineId;
        this.scheduledAtMs = scheduledAtMs;
        this.isTaken = isTaken;
    }
}
