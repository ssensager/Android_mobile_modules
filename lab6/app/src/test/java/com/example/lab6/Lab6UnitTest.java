package com.example.lab6;

import com.example.lab6.data.projection.WorkoutSetWithEquipmentName;
import com.example.lab6.util.CsvExporter;
import com.example.lab6.util.DateUtils;
import com.example.lab6.util.PrDetector;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Модульные тесты ключевой логики: определение нового личного рекорда,
 * формирование CSV-файла и расчёт границы дат для автоочистки.
 */
public class Lab6UnitTest {

    @Test
    public void prDetector_detectsHigherWeightAsNewRecord() {
        assertTrue(PrDetector.isNewRecord(80f, 85f));
        assertEquals(5f, PrDetector.getImprovementKg(80f, 85f), 0.001f);
    }

    @Test
    public void prDetector_doesNotFlagEqualOrLowerWeightAsRecord() {
        assertFalse(PrDetector.isNewRecord(80f, 80f));
        assertFalse(PrDetector.isNewRecord(80f, 75f));
        assertFalse(PrDetector.isNewRecord(80f, 0f));
        assertEquals(0f, PrDetector.getImprovementKg(80f, 75f), 0.001f);
    }

    @Test
    public void prDetector_treatsFirstPositiveSetAsInitialRecord() {
        assertTrue(PrDetector.isNewRecord(null, 40f));
        assertTrue(PrDetector.isNewRecord(0f, 40f));
        assertEquals(0f, PrDetector.getImprovementKg(null, 40f), 0.001f);
    }

    @Test
    public void csvExporter_buildsUtf8BomHeaderAndCalculatedVolume() {
        WorkoutSetWithEquipmentName row1 = new WorkoutSetWithEquipmentName(
                1, 1, 82.5f, 8, 1_700_000_000_000L, "Жим штанги лёжа", "Грудь"
        );
        WorkoutSetWithEquipmentName row2 = new WorkoutSetWithEquipmentName(
                2, 2, 100f, 5, 1_700_003_600_000L, "Присед; \"глубокий\"", "Ноги"
        );

        String csv = CsvExporter.buildCsvContent(Arrays.asList(row1, row2));

        assertTrue("В начале файла должен быть UTF-8 BOM для Excel", csv.startsWith(CsvExporter.UTF8_BOM));
        assertTrue(csv.contains("Дата;Тренажёр;Группа мышц;Вес (кг);Повторения;Объём\r\n"));
        assertTrue(csv.contains(";Жим штанги лёжа;Грудь;82.5;8;660\r\n"));
        assertTrue(csv.contains(";\"Присед; \"\"глубокий\"\"\";Ноги;100;5;500\r\n"));
    }

    @Test
    public void csvExporter_handlesEmptyHistoryGracefully() {
        String csv = CsvExporter.buildCsvContent(Collections.emptyList());
        assertEquals(
                CsvExporter.UTF8_BOM + "Дата;Тренажёр;Группа мышц;Вес (кг);Повторения;Объём\r\n",
                csv
        );
    }

    @Test
    public void dateUtils_calculatesCleanupCutoffCorrectly() {
        long nowMs = 1_760_000_000_000L;
        long cutoff30 = DateUtils.calculateCutoffMs(nowMs, 30);
        long cutoff7 = DateUtils.calculateCutoffMs(nowMs, 7);

        assertEquals(nowMs - TimeUnit.DAYS.toMillis(30), cutoff30);
        assertEquals(nowMs - TimeUnit.DAYS.toMillis(7), cutoff7);
        assertTrue("Порог 7 дней должен быть свежее порога 30 дней", cutoff7 > cutoff30);
    }

    @Test
    public void dateUtils_clampsNonPositiveRetentionToOneDay() {
        long nowMs = 1_760_000_000_000L;
        assertEquals(nowMs - TimeUnit.DAYS.toMillis(1), DateUtils.calculateCutoffMs(nowMs, 0));
        assertEquals(nowMs - TimeUnit.DAYS.toMillis(1), DateUtils.calculateCutoffMs(nowMs, -10));
    }

    @Test
    public void dateUtils_formatsCompactVolumeAndDayShort() {
        assertEquals("850", DateUtils.formatCompactVolume(850f));
        assertEquals("2.4т", DateUtils.formatCompactVolume(2400f));
        assertEquals("24.11", DateUtils.formatDayShort("2026-11-24"));
    }
}
