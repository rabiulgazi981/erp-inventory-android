package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.promo.PromoLine
import com.bongshaltech.erpinventory.core.promo.PromoMath
import com.bongshaltech.erpinventory.core.promo.PromoRule
import com.bongshaltech.erpinventory.core.stock.Categories
import com.bongshaltech.erpinventory.core.stock.Reorder
import com.bongshaltech.erpinventory.core.stock.ScaleBarcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockMoreTest {
    @Test fun categoriesSplitAndMatch() {
        assertEquals(listOf("Food", "Cat"), Categories.split(" Food, Cat ,food,,"))
        assertTrue(Categories.has("Food, Cat", "cat"))
        assertFalse(Categories.has("Food, Cat", "Dog"))
        assertEquals("Food, Cat", Categories.join(listOf("Food", " Cat", "FOOD")))
    }

    @Test fun offerCoversASecondCategory() {
        val line = PromoLine(1, "Food, Cat", 1000L, 100_00L)
        assertEquals(10_00L, PromoMath.lineOff(line, PromoRule(1, 1000, 0L, category = "Cat")))
    }

    @Test fun readsScaleLabel() {
        val body = "201234500750" // PLU 12345, 750 g
        val code = body + ScaleBarcode.checkDigit(body)
        val r = ScaleBarcode.read(code)!!
        assertEquals("12345", r.plu)
        assertEquals(750L, r.value)
        assertEquals(750L, ScaleBarcode.qtyMilli(r, false, 0L))
        // Price label: 75.00 taka at 150 taka/kg = 0.5 kg
        assertEquals(500L, ScaleBarcode.qtyMilli(r.copy(value = 7500L), true, 150_00L))
    }

    @Test fun rejectsOtherCodes() {
        assertNull(ScaleBarcode.read("8901234567890"))
        assertNull(ScaleBarcode.read("201234500750" + (ScaleBarcode.checkDigit("201234500750") + 1) % 10))
        assertNull(ScaleBarcode.read("12345"))
    }

    @Test fun reorderBackToTwiceTheMark() {
        assertEquals(17_000L, Reorder.suggest(3_000L, 10_000L))
        assertEquals(0L, Reorder.suggest(11_000L, 10_000L))
        assertEquals(0L, Reorder.suggest(0L, 0L))
        assertEquals(20_000L, Reorder.suggest(-2_000L, 10_000L))
    }
}
