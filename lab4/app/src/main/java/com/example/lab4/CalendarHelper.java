package com.example.lab4;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Помощник для прямой записи повторяющихся событий полива в системный календарь.
 * Собрал здесь поиск доступного для записи календаря, проверку наложений, вставку RRULE и удаление по ID.
 */
public final class CalendarHelper {

    public static final int REMINDER_MINUTES = 60;
    public static final long EVENT_DURATION_MS = 30L * 60L * 1000L;
    public static final String EVENT_DURATION_RFC = "PT30M";

    private CalendarHelper() {
    }

    public static final class CalendarOverlap {
        public final String title;
        public final long startMs;
        public final long endMs;

        public CalendarOverlap(String title, long startMs, long endMs) {
            this.title = title;
            this.startMs = startMs;
            this.endMs = endMs;
        }
    }

    // Формирую правило повторения по стандарту iCalendar (RFC 5545)
    public static String buildRRule(int intervalDays) {
        int safeDays = Math.max(1, intervalDays);
        return "FREQ=DAILY;INTERVAL=" + safeDays;
    }

    public static boolean hasCalendarPermissions(Context context) {
        boolean readGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED;
        boolean writeGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED;
        return readGranted && writeGranted;
    }

    // Назначаю первый полив на завтра в 09:00 по локальному времени устройства
    public static long computeNextMorningWateringMs() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Ищу календарь, в который разрешена запись, отдавая приоритет основному календарю пользователя
    public static long findWritableCalendarId(Context context) {
        if (!hasCalendarPermissions(context)) {
            return -1L;
        }
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.IS_PRIMARY
        };
        String selection = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= ? AND "
                + CalendarContract.Calendars.VISIBLE + " = 1";
        String[] args = {String.valueOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)};

        long fallbackId = -1L;
        try (Cursor cursor = resolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection, selection, args, null
        )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    int isPrimary = cursor.getInt(1);
                    if (fallbackId == -1L) {
                        fallbackId = id;
                    }
                    if (isPrimary == 1) {
                        return id;
                    }
                }
            }
        } catch (SecurityException ignored) {
            return -1L;
        }
        return fallbackId;
    }

    // Проверяю занятость календаря вокруг планируемого слота, чтобы заранее предупредить о наложении
    public static List<CalendarOverlap> findOverlappingEvents(
            Context context, long startMs, long durationMs
    ) {
        List<CalendarOverlap> overlaps = new ArrayList<>();
        if (!hasCalendarPermissions(context)) {
            return overlaps;
        }
        long endMs = startMs + durationMs;
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMs);
        ContentUris.appendId(builder, endMs);

        String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
        };

        try (Cursor cursor = context.getContentResolver().query(
                builder.build(), projection, null, null, CalendarContract.Instances.BEGIN + " ASC"
        )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String title = cursor.getString(0);
                    long begin = cursor.getLong(1);
                    long end = cursor.getLong(2);
                    if (title == null || title.trim().isEmpty()) {
                        title = "Без названия";
                    }
                    overlaps.add(new CalendarOverlap(title, begin, end));
                }
            }
        } catch (SecurityException ignored) {
        }
        return overlaps;
    }

    // Записываю серию событий полива с RRULE и напоминанием за 60 минут, возвращая eventId
    public static long createWateringEvent(Context context, PlantItem plant, long startMs) {
        if (!hasCalendarPermissions(context)) {
            return -1L;
        }
        long calendarId = findWritableCalendarId(context);
        if (calendarId <= 0L) {
            return -1L;
        }

        ContentResolver resolver = context.getContentResolver();
        ContentValues eventValues = new ContentValues();
        eventValues.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        eventValues.put(CalendarContract.Events.TITLE, "Полить: " + plant.getName());
        eventValues.put(
                CalendarContract.Events.DESCRIPTION,
                "ФлораГид · уход за растением (" + plant.getLightNeeds() + ", " + plant.getTemperature() + ")"
        );
        eventValues.put(CalendarContract.Events.DTSTART, startMs);
        eventValues.put(CalendarContract.Events.DURATION, EVENT_DURATION_RFC);
        eventValues.put(CalendarContract.Events.RRULE, buildRRule(plant.getWateringIntervalDays()));
        eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

        try {
            Uri inserted = resolver.insert(CalendarContract.Events.CONTENT_URI, eventValues);
            if (inserted == null || inserted.getLastPathSegment() == null) {
                return -1L;
            }
            long eventId = Long.parseLong(inserted.getLastPathSegment());

            // Добавляю напоминание за час до полива
            ContentValues reminderValues = new ContentValues();
            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
            reminderValues.put(CalendarContract.Reminders.MINUTES, REMINDER_MINUTES);
            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);

            return eventId;
        } catch (SecurityException | NumberFormatException ignored) {
            return -1L;
        }
    }

    // Удаляю ранее созданную серию событий по сохранённому идентификатору
    public static boolean deleteEvent(Context context, long eventId) {
        if (eventId <= 0L || !hasCalendarPermissions(context)) {
            return false;
        }
        try {
            Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
            int rows = context.getContentResolver().delete(deleteUri, null, null);
            return rows > 0;
        } catch (SecurityException ignored) {
            return false;
        }
    }
}
