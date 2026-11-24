package com.example.lab6.data.projection;

import androidx.room.Ignore;

/**
 * Проекция дневного объёма тренировок, сгруппированного в SQL через GROUP BY.
 * Передаю список таких точек напрямую в график прогресса за последние 7 дней.
 */
public class DailyVolumePoint {

    public String dayKey;
    public float totalVolume;
    public int setsCount;

    public DailyVolumePoint() {
    }

    @Ignore
    public DailyVolumePoint(String dayKey, float totalVolume, int setsCount) {
        this.dayKey = dayKey;
        this.totalVolume = totalVolume;
        this.setsCount = setsCount;
    }
}
