package com.example.lab6.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Утилита для расчёта границ дат, форматирования времени и весов.
 * Вынес всю работу с календарём и миллисекундами в один класс, чтобы не дублировать расчёты в экранах.
 */
public final class DateUtils {

    private static final Locale RU_LOCALE = new Locale("ru", "RU");

    private DateUtils() {
    }

    /**
     * Возвращаю метку начала сегодняшнего дня (00:00:00.000) в миллисекундах.
     */
    public static long getStartOfTodayMs() {
        return getDaysAgoStartMs(0);
    }

    /**
     * Возвращаю метку начала дня N дней назад (00:00:00.000).
     */
    public static long getDaysAgoStartMs(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Вычисляю пороговую метку времени для автоочистки по выбранному сроку хранения.
     */
    public static long calculateCutoffMs(long nowMs, int retentionDays) {
        int safeDays = Math.max(1, retentionDays);
        return nowMs - TimeUnit.DAYS.toMillis(safeDays);
    }

    /**
     * Формирую список ключей последних 7 дней в формате yyyy-MM-dd для оси X графика.
     */
    public static List<String> buildLast7DayKeys(long nowMs) {
        List<String> keys = new ArrayList<>(7);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMs);
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            keys.add(fmt.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return keys;
    }

    /**
     * Преобразую ключ yyyy-MM-dd в короткую подпись ДД.ММ для столбца графика.
     */
    public static String formatDayShort(String dayKey) {
        if (dayKey == null || dayKey.length() < 10) {
            return "";
        }
        String month = dayKey.substring(5, 7);
        String day = dayKey.substring(8, 10);
        return day + "." + month;
    }

    /**
     * Форматирую дату и время подхода в привычный вид ДД.ММ.ГГГГ ЧЧ:ММ.
     */
    public static String formatDateTime(long timeMs) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", RU_LOCALE);
        return fmt.format(new Date(timeMs));
    }

    /**
     * Убираю лишний .0 у целых весов, чтобы 80.0 отображалось аккуратно как 80.
     */
    public static String formatNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.01f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    /**
     * Короткая подпись объёма для графика: до 1000 в кг, от 1000 — в тоннах (например, 2.4т).
     */
    public static String formatCompactVolume(float volumeKg) {
        if (volumeKg <= 0f) {
            return "0";
        }
        if (volumeKg >= 1000f) {
            return String.format(Locale.US, "%.1fт", volumeKg / 1000f);
        }
        return String.valueOf(Math.round(volumeKg));
    }
}
