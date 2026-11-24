package com.example.lab6.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Хранение настроек автоочистки в SharedPreferences.
 * Сохраняю выбранный пользователем срок хранения и отметку о последнем запуске очистки.
 */
public final class CleanupPrefs {

    private static final String PREFS_NAME = "progress_zal_cleanup_prefs";
    private static final String KEY_RETENTION_DAYS = "retention_days";
    private static final String KEY_LAST_CLEANUP_MS = "last_cleanup_ms";
    private static final String KEY_LAST_DELETED_COUNT = "last_deleted_count";

    public static final int DEFAULT_RETENTION_DAYS = 30;
    public static final int[] RETENTION_OPTIONS = {7, 30, 90, 180};

    private CleanupPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getRetentionDays(Context context) {
        return prefs(context).getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS);
    }

    public static void setRetentionDays(Context context, int days) {
        prefs(context).edit().putInt(KEY_RETENTION_DAYS, days).apply();
    }

    public static long getLastCleanupMs(Context context) {
        return prefs(context).getLong(KEY_LAST_CLEANUP_MS, 0L);
    }

    public static int getLastDeletedCount(Context context) {
        return prefs(context).getInt(KEY_LAST_DELETED_COUNT, 0);
    }

    public static void saveCleanupRun(Context context, long timeMs, int deletedSets) {
        prefs(context).edit()
                .putLong(KEY_LAST_CLEANUP_MS, timeMs)
                .putInt(KEY_LAST_DELETED_COUNT, deletedSets)
                .apply();
    }
}
