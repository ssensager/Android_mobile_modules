package com.example.lab5.domain;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

// Храню итоговую сводку по рискам аптечки, чтобы и баннер, и шкала риска читали один источник.
public class RiskSummary {

    public static final String LEVEL_NONE = "NONE";
    public static final String LEVEL_LOW = "LOW";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_HIGH = "HIGH";

    @NonNull
    public final List<DetectedConflict> conflicts;

    public final int highCount;
    public final int mediumCount;
    public final int lowCount;
    public final int riskScore;

    @NonNull
    public final String overallLevel;

    public RiskSummary(
            @NonNull List<DetectedConflict> conflicts,
            int highCount,
            int mediumCount,
            int lowCount,
            int riskScore,
            @NonNull String overallLevel
    ) {
        this.conflicts = Collections.unmodifiableList(conflicts);
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.lowCount = lowCount;
        this.riskScore = riskScore;
        this.overallLevel = overallLevel;
    }

    public static RiskSummary empty() {
        return new RiskSummary(Collections.emptyList(), 0, 0, 0, 0, LEVEL_NONE);
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }
}
