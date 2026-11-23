package com.example.lab1;

/**
 * Структура с результатами расчёта стоимости поездки.
 * Вычисляется в одном методе ViewModel и передаётся как в текстовые поля,
 * так и в собственный графический компонент PriceBreakdownView.
 */
public final class PriceBreakdown {

    public final int baseCost;     // Подача автомобиля, руб
    public final int kmCost;       // Стоимость пути по километрам, руб
    public final int minCost;      // Стоимость времени в пути, руб
    public final int optionsCost;  // Опции и доп. пассажиры, руб
    public final int totalCost;    // Итоговая сумма, руб

    // Массивы из 4 элементов (Подача, Километры, Минуты, Опции) для передачи в PriceBreakdownView
    public final int[] amounts;
    public final float[] shares;

    public PriceBreakdown(int baseCost, int kmCost, int minCost, int optionsCost) {
        this.baseCost = baseCost;
        this.kmCost = kmCost;
        this.minCost = minCost;
        this.optionsCost = optionsCost;
        this.totalCost = baseCost + kmCost + minCost + optionsCost;
        this.amounts = new int[]{baseCost, kmCost, minCost, optionsCost};

        if (totalCost > 0) {
            this.shares = new float[]{
                    (float) baseCost / totalCost,
                    (float) kmCost / totalCost,
                    (float) minCost / totalCost,
                    (float) optionsCost / totalCost
            };
        } else {
            this.shares = new float[]{0f, 0f, 0f, 0f};
        }
    }
}
