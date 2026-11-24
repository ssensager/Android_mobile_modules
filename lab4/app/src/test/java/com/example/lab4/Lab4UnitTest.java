package com.example.lab4;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Модульные тесты для расчёта коэффициента сжатия изображений и генерации правила RRULE.
 */
public class Lab4UnitTest {

    @Test
    public void calculateInSampleSize_returnsOneForSmallImages() {
        assertEquals(1, ImageCompressor.calculateInSampleSize(800, 600, 1024, 1024));
        assertEquals(1, ImageCompressor.calculateInSampleSize(1024, 1024, 1024, 1024));
    }

    @Test
    public void calculateInSampleSize_scalesDownLargePhotosByPowerOfTwo() {
        // Снимок 4000×3000 уменьшаю вдвое на этапе декодирования (inSampleSize = 2)
        assertEquals(2, ImageCompressor.calculateInSampleSize(4000, 3000, 1024, 1024));
        // Снимок 4096×4096 уменьшаю вчетверо (inSampleSize = 4)
        assertEquals(4, ImageCompressor.calculateInSampleSize(4096, 4096, 1024, 1024));
    }

    @Test
    public void calculateInSampleSize_handlesInvalidDimensionsSafely() {
        assertEquals(1, ImageCompressor.calculateInSampleSize(0, 0, 1024, 1024));
        assertEquals(1, ImageCompressor.calculateInSampleSize(-100, 500, 1024, 1024));
    }

    @Test
    public void buildRRule_formatsValidDailyInterval() {
        assertEquals("FREQ=DAILY;INTERVAL=1", CalendarHelper.buildRRule(1));
        assertEquals("FREQ=DAILY;INTERVAL=3", CalendarHelper.buildRRule(3));
        assertEquals("FREQ=DAILY;INTERVAL=7", CalendarHelper.buildRRule(7));
    }

    @Test
    public void buildRRule_clampsNonPositiveIntervalToOne() {
        assertEquals("FREQ=DAILY;INTERVAL=1", CalendarHelper.buildRRule(0));
        assertEquals("FREQ=DAILY;INTERVAL=1", CalendarHelper.buildRRule(-4));
    }
}
