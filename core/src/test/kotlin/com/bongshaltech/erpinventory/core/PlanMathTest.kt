package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.billing.PlanMath
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanMathTest {
    @Test fun periods() {
        assertEquals(1, PlanMath.months("P1M"))
        assertEquals(3, PlanMath.months("P3M"))
        assertEquals(6, PlanMath.months("P6M"))
        assertEquals(12, PlanMath.months("P1Y"))
        assertEquals(0, PlanMath.months("bad"))
    }

    @Test fun ownerPricesSave() {
        val m = 250_000_000L
        assertEquals(6, PlanMath.savingPercent(700_000_000L, 3, m))
        assertEquals(13, PlanMath.savingPercent(1_300_000_000L, 6, m))
        assertEquals(20, PlanMath.savingPercent(2_400_000_000L, 12, m))
        assertEquals(0, PlanMath.savingPercent(m, 1, m))
    }
}
