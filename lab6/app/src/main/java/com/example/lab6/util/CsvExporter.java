package com.example.lab6.util;

import android.content.Context;

import com.example.lab6.data.projection.WorkoutSetWithEquipmentName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Генератор CSV-файла для экспорта всей истории тренировок.
 * Добавляю UTF-8 BOM в начало файла, чтобы Excel на Windows сразу корректно читал кириллицу.
 */
public final class CsvExporter {

    public static final String UTF8_BOM = "\uFEFF";
    public static final String EXPORT_FILE_NAME = "workout_history.csv";

    private CsvExporter() {
    }

    /**
     * Собираю текст CSV с разделителем «;» (удобно для русской локали Excel).
     */
    public static String buildCsvContent(List<WorkoutSetWithEquipmentName> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(UTF8_BOM);
        sb.append("Дата;Тренажёр;Группа мышц;Вес (кг);Повторения;Объём\r\n");

        if (rows != null) {
            for (WorkoutSetWithEquipmentName row : rows) {
                float volume = row.weightKg * row.reps;
                sb.append(escapeCsv(DateUtils.formatDateTime(row.dateMs))).append(';');
                sb.append(escapeCsv(row.equipmentName)).append(';');
                sb.append(escapeCsv(row.muscleGroup)).append(';');
                sb.append(DateUtils.formatNumber(row.weightKg)).append(';');
                sb.append(row.reps).append(';');
                sb.append(DateUtils.formatNumber(volume)).append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Записываю CSV в каталог кэша приложения; вызывать строго в фоновом потоке.
     */
    public static File exportToCacheFile(Context context, List<WorkoutSetWithEquipmentName> rows) throws IOException {
        File outFile = new File(context.getCacheDir(), EXPORT_FILE_NAME);
        String content = buildCsvContent(rows);
        try (FileOutputStream fos = new FileOutputStream(outFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
        return outFile;
    }

    private static String escapeCsv(String raw) {
        if (raw == null) {
            return "";
        }
        boolean needsQuotes = raw.contains(";") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r");
        if (!needsQuotes) {
            return raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }
}
