package com.example.lab5.domain;

import androidx.annotation.NonNull;

import com.example.lab5.data.entity.ConflictRule;

// Описываю найденный конфликт между двумя препаратами пользователя в удобном для экрана виде.
public class DetectedConflict {

    @NonNull
    public final ConflictRule rule;

    @NonNull
    public final String medicineAName;

    @NonNull
    public final String medicineBName;

    @NonNull
    public final String substanceAName;

    @NonNull
    public final String substanceBName;

    @NonNull
    public final String severity;

    public final int severityRank;

    public DetectedConflict(
            @NonNull ConflictRule rule,
            @NonNull String medicineAName,
            @NonNull String medicineBName,
            @NonNull String substanceAName,
            @NonNull String substanceBName,
            @NonNull String severity,
            int severityRank
    ) {
        this.rule = rule;
        this.medicineAName = medicineAName;
        this.medicineBName = medicineBName;
        this.substanceAName = substanceAName;
        this.substanceBName = substanceBName;
        this.severity = severity;
        this.severityRank = severityRank;
    }

    public boolean involvesSubstance(int substanceId) {
        return rule.substanceAId == substanceId || rule.substanceBId == substanceId;
    }
}
