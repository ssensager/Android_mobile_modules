package com.example.lab3

import com.example.lab3.data.FinanceSampleData
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceUnitTest {

    @Test
    fun categorySharesSumToOne() {
        val sum = FinanceSampleData.sampleContent.categories.sumOf { it.share.toDouble() }
        assertEquals(1.0, sum, 0.001)
    }

    @Test
    fun negativeAndPositiveAmountsFormatCleanly() {
        val negative = FinanceSampleData.formatRub(-1480)
        val positive = FinanceSampleData.formatRub(46000, showPlusSign = true)
        assertEquals(true, negative.startsWith("−") && negative.endsWith("₽"))
        assertEquals(true, positive.startsWith("+") && positive.endsWith("₽"))
    }
}
