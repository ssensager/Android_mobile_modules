package com.example.lab5.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// Храню правила несовместимости как данные в таблице, чтобы справочник можно было расширять без пересборки кода.
@Entity(
        tableName = "conflict_rule",
        foreignKeys = {
                @ForeignKey(
                        entity = ActiveSubstance.class,
                        parentColumns = "id",
                        childColumns = "substanceAId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = ActiveSubstance.class,
                        parentColumns = "id",
                        childColumns = "substanceBId",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class ConflictRule {

    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_LOW = "LOW";

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Индексирую оба внешних ключа для быстрого поиска конфликтов по веществам.
    @ColumnInfo(index = true)
    public int substanceAId;

    @ColumnInfo(index = true)
    public int substanceBId;

    @NonNull
    public String severity;

    @NonNull
    public String title;

    @NonNull
    public String recommendation;

    public ConflictRule(
            int substanceAId,
            int substanceBId,
            @NonNull String severity,
            @NonNull String title,
            @NonNull String recommendation
    ) {
        this.substanceAId = substanceAId;
        this.substanceBId = substanceBId;
        this.severity = severity;
        this.title = title;
        this.recommendation = recommendation;
    }

    @Ignore
    public ConflictRule(
            int id,
            int substanceAId,
            int substanceBId,
            @NonNull String severity,
            @NonNull String title,
            @NonNull String recommendation
    ) {
        this.id = id;
        this.substanceAId = substanceAId;
        this.substanceBId = substanceBId;
        this.severity = severity;
        this.title = title;
        this.recommendation = recommendation;
    }
}
