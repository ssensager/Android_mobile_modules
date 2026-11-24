package com.example.lab6.data.projection;

import java.util.Objects;

/**
 * Проекция тренажёра для главного списка с вычисленным в SQL рекордом и последним подходом.
 * Намеренно не включаю сюда notes и photoPath, чтобы не тянуть лишние поля в каждой строке списка.
 */
public class EquipmentWithMaxSet {

    public int id;
    public String name;
    public String muscleGroup;
    public float maxWeightKg;
    public int maxWeightReps;
    public int totalSets;
    public float lastWeightKg;
    public int lastReps;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EquipmentWithMaxSet)) return false;
        EquipmentWithMaxSet that = (EquipmentWithMaxSet) o;
        return id == that.id
                && Float.compare(that.maxWeightKg, maxWeightKg) == 0
                && maxWeightReps == that.maxWeightReps
                && totalSets == that.totalSets
                && Float.compare(that.lastWeightKg, lastWeightKg) == 0
                && lastReps == that.lastReps
                && Objects.equals(name, that.name)
                && Objects.equals(muscleGroup, that.muscleGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, muscleGroup, maxWeightKg, maxWeightReps, totalSets, lastWeightKg, lastReps);
    }
}
