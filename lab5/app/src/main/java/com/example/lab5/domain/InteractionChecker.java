package com.example.lab5.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lab5.data.entity.ConflictRule;
import com.example.lab5.data.entity.MedicineWithDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Вынес проверку совместимости препаратов в чистый Java-класс вне Activity и Room, чтобы спокойно покрыть его тестами.
public class InteractionChecker {

    private static final int WEIGHT_HIGH = 45;
    private static final int WEIGHT_MEDIUM = 22;
    private static final int WEIGHT_LOW = 10;

    // Проверяю все правила из базы независимо от порядка веществ (A+B или B+A), убираю дубли и сортирую по опасности.
    @NonNull
    public RiskSummary evaluate(
            @Nullable List<MedicineWithDetails> medicines,
            @Nullable List<ConflictRule> rules
    ) {
        if (medicines == null || medicines.isEmpty() || rules == null || rules.isEmpty()) {
            return RiskSummary.empty();
        }

        // Группирую препараты по идентификатору действующего вещества.
        Map<Integer, List<MedicineWithDetails>> bySubstance = new HashMap<>();
        for (MedicineWithDetails item : medicines) {
            if (item == null || item.medicine == null) {
                continue;
            }
            int substanceId = item.medicine.substanceId;
            List<MedicineWithDetails> group = bySubstance.get(substanceId);
            if (group == null) {
                group = new ArrayList<>();
                bySubstance.put(substanceId, group);
            }
            group.add(item);
        }

        // Дедуплицирую конфликты по нормализованной паре веществ, оставляя более серьёзное правило.
        Map<String, DetectedConflict> uniqueByPair = new LinkedHashMap<>();

        for (ConflictRule rule : rules) {
            if (rule == null) {
                continue;
            }
            List<MedicineWithDetails> groupA = bySubstance.get(rule.substanceAId);
            List<MedicineWithDetails> groupB = bySubstance.get(rule.substanceBId);
            if (groupA == null || groupB == null) {
                continue;
            }

            // Если правило проверяет одно и то же вещество, нужно минимум два разных препарата.
            if (rule.substanceAId == rule.substanceBId && groupA.size() < 2) {
                continue;
            }

            MedicineWithDetails medA = groupA.get(0);
            MedicineWithDetails medB = (rule.substanceAId == rule.substanceBId) ? groupA.get(1) : groupB.get(0);

            String normalizedSeverity = normalizeSeverity(rule.severity);
            int rank = severityRank(normalizedSeverity);

            int minId = Math.min(rule.substanceAId, rule.substanceBId);
            int maxId = Math.max(rule.substanceAId, rule.substanceBId);
            String pairKey = minId + ":" + maxId;

            DetectedConflict candidate = new DetectedConflict(
                    rule,
                    joinMedicineNames(groupA, rule.substanceAId == rule.substanceBId ? medA : null),
                    joinMedicineNames(groupB, rule.substanceAId == rule.substanceBId ? medB : null),
                    resolveSubstanceName(medA),
                    resolveSubstanceName(medB),
                    normalizedSeverity,
                    rank
            );

            DetectedConflict existing = uniqueByPair.get(pairKey);
            if (existing == null || candidate.severityRank > existing.severityRank) {
                uniqueByPair.put(pairKey, candidate);
            }
        }

        List<DetectedConflict> conflicts = new ArrayList<>(uniqueByPair.values());
        Collections.sort(conflicts, new Comparator<DetectedConflict>() {
            @Override
            public int compare(DetectedConflict first, DetectedConflict second) {
                int rankCompare = Integer.compare(second.severityRank, first.severityRank);
                if (rankCompare != 0) {
                    return rankCompare;
                }
                return first.rule.title.compareToIgnoreCase(second.rule.title);
            }
        });

        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        int rawScore = 0;

        for (DetectedConflict conflict : conflicts) {
            if (ConflictRule.SEVERITY_HIGH.equals(conflict.severity)) {
                highCount++;
                rawScore += WEIGHT_HIGH;
            } else if (ConflictRule.SEVERITY_MEDIUM.equals(conflict.severity)) {
                mediumCount++;
                rawScore += WEIGHT_MEDIUM;
            } else {
                lowCount++;
                rawScore += WEIGHT_LOW;
            }
        }

        int clampedScore = Math.min(100, rawScore);
        String overallLevel;
        if (highCount > 0) {
            overallLevel = RiskSummary.LEVEL_HIGH;
        } else if (mediumCount > 0) {
            overallLevel = RiskSummary.LEVEL_MEDIUM;
        } else if (lowCount > 0) {
            overallLevel = RiskSummary.LEVEL_LOW;
        } else {
            overallLevel = RiskSummary.LEVEL_NONE;
        }

        return new RiskSummary(conflicts, highCount, mediumCount, lowCount, clampedScore, overallLevel);
    }

    // Возвращаю наивысший уровень риска для конкретного препарата, чтобы подсветить его карточку в списке.
    @NonNull
    public String getHighestSeverityForSubstance(int substanceId, @Nullable List<DetectedConflict> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return RiskSummary.LEVEL_NONE;
        }
        String bestLevel = RiskSummary.LEVEL_NONE;
        int bestRank = 0;
        for (DetectedConflict conflict : conflicts) {
            if (conflict.involvesSubstance(substanceId) && conflict.severityRank > bestRank) {
                bestRank = conflict.severityRank;
                bestLevel = conflict.severity;
            }
        }
        return bestLevel;
    }

    // Отбираю конфликты, которые касаются выбранного препарата на экране деталей.
    @NonNull
    public List<DetectedConflict> getConflictsForSubstance(int substanceId, @Nullable List<DetectedConflict> conflicts) {
        List<DetectedConflict> matched = new ArrayList<>();
        if (conflicts == null) {
            return matched;
        }
        for (DetectedConflict conflict : conflicts) {
            if (conflict.involvesSubstance(substanceId)) {
                matched.add(conflict);
            }
        }
        return matched;
    }

    public static int severityRank(@Nullable String severity) {
        String normalized = normalizeSeverity(severity);
        if (ConflictRule.SEVERITY_HIGH.equals(normalized)) {
            return 3;
        }
        if (ConflictRule.SEVERITY_MEDIUM.equals(normalized)) {
            return 2;
        }
        return 1;
    }

    @NonNull
    private static String normalizeSeverity(@Nullable String severity) {
        if (severity == null) {
            return ConflictRule.SEVERITY_LOW;
        }
        String upper = severity.trim().toUpperCase(Locale.ROOT);
        if (ConflictRule.SEVERITY_HIGH.equals(upper)) {
            return ConflictRule.SEVERITY_HIGH;
        }
        if (ConflictRule.SEVERITY_MEDIUM.equals(upper)) {
            return ConflictRule.SEVERITY_MEDIUM;
        }
        return ConflictRule.SEVERITY_LOW;
    }

    @NonNull
    private static String resolveSubstanceName(@NonNull MedicineWithDetails details) {
        if (details.substance != null && details.substance.name != null && !details.substance.name.isEmpty()) {
            return details.substance.name;
        }
        return "Вещество #" + details.medicine.substanceId;
    }

    @NonNull
    private static String joinMedicineNames(
            @NonNull List<MedicineWithDetails> group,
            @Nullable MedicineWithDetails singleOverride
    ) {
        if (singleOverride != null) {
            return singleOverride.medicine.name;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < group.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(group.get(i).medicine.name);
        }
        return builder.toString();
    }
}
