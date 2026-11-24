package com.example.lab6.util;

/**
 * Проверка нового личного рекорда по тренажёру.
 * Вынес сравнение в чистый класс без зависимостей от Android, чтобы легко покрыть юнит-тестами.
 */
public final class PrDetector {

    private PrDetector() {
    }

    /**
     * Возвращаю true, если новый вес больше прежнего рекорда (или это первый подход на тренажёре).
     */
    public static boolean isNewRecord(Float previousMaxKg, float newWeightKg) {
        if (newWeightKg <= 0f) {
            return false;
        }
        if (previousMaxKg == null || previousMaxKg <= 0f) {
            return true;
        }
        return newWeightKg > previousMaxKg;
    }

    /**
     * Считаю прибавку к прошлому рекорду в килограммах.
     */
    public static float getImprovementKg(Float previousMaxKg, float newWeightKg) {
        if (previousMaxKg == null || previousMaxKg <= 0f || newWeightKg <= previousMaxKg) {
            return 0f;
        }
        return newWeightKg - previousMaxKg;
    }
}
