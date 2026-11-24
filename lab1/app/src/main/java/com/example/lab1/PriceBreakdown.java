package com.example.lab1;

/**
 * Простая модель с готовой разбивкой цены.
 * Считаю её один раз во ViewModel и отдаю сразу и в текст на экране, и в шкалу PriceBreakdownView.
 */
public final class PriceBreakdown {

    public final int baseCost;
    public final int kmCost;
    public final int minCost;
    public final int optionsCost;
    public final int totalCost;

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
