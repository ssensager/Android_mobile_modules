package com.example.lab1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Простые модульные тесты расчёта стоимости поездки в такси.
 * Проверяем формулу прайса, коэффициент спроса и сумму долей для шкалы PriceBreakdownView.
 */
public class ExampleUnitTest {

    @Test
    public void defaultComfortCityCalculationIsCorrect() {
        TaxiOrderState state = TaxiOrderState.initial(); // Комфорт (149 + 9*24 + 22*8), 1.0x, 1 пасс.
        PriceBreakdown breakdown = MainViewModel.calculateBreakdown(state);

        assertEquals(149, breakdown.baseCost);
        assertEquals(216, breakdown.kmCost);
        assertEquals(176, breakdown.minCost);
        assertEquals(0, breakdown.optionsCost);
        assertEquals(541, breakdown.totalCost);
    }

    @Test
    public void demandMultiplierAndOptionsRecalculateTotal() {
        // Эконом (99 + 4*18 + 12*6), Центр, спрос 2.0x (индекс 3), кресло (150) + багаж (100) + 2 пасс. (+40)
        TaxiOrderState state = new TaxiOrderState(0, 0, 3, true, true, false, 2, -1, 0);
        PriceBreakdown breakdown = MainViewModel.calculateBreakdown(state);

        assertEquals(198, breakdown.baseCost);
        assertEquals(144, breakdown.kmCost);
        assertEquals(144, breakdown.minCost);
        assertEquals(580, breakdown.optionsCost);
        assertEquals(1066, breakdown.totalCost);
    }
}
