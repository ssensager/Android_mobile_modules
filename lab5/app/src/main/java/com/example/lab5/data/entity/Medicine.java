package com.example.lab5.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// Храню препарат с ссылкой на действующее вещество, чтобы проверять конфликты без привязки к торговому названию.
@Entity(
        tableName = "medicine",
        foreignKeys = @ForeignKey(
                entity = ActiveSubstance.class,
                parentColumns = "id",
                childColumns = "substanceId",
                onDelete = ForeignKey.CASCADE
        )
)
public class Medicine {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    @NonNull
    public String dosage;

    // Сделал индекс на внешнем ключе, чтобы соединения таблиц не тормозили при росте базы.
    @ColumnInfo(index = true)
    public int substanceId;

    @NonNull
    public String scheduleTime;

    @NonNull
    public String notes;

    public Medicine(
            @NonNull String name,
            @NonNull String dosage,
            int substanceId,
            @NonNull String scheduleTime,
            @NonNull String notes
    ) {
        this.name = name;
        this.dosage = dosage;
        this.substanceId = substanceId;
        this.scheduleTime = scheduleTime;
        this.notes = notes;
    }

    @Ignore
    public Medicine(
            int id,
            @NonNull String name,
            @NonNull String dosage,
            int substanceId,
            @NonNull String scheduleTime,
            @NonNull String notes
    ) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.substanceId = substanceId;
        this.scheduleTime = scheduleTime;
        this.notes = notes;
    }
}
