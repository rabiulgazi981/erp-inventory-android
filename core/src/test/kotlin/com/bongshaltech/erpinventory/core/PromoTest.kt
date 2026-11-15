package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.promo.PromoLine
import com.bongshaltech.erpinventory.core.promo.PromoMath
import com.bongshaltech.erpinventory.core.promo.PromoRule
import org.junit.Test
import org.junit.Assert.assertEquals

class PromoTest {
    private val rice = PromoLine(1, "Grocery", 2000, 20000)
    private val soap = PromoLine(2, "Cosmetics", 1000, 5000)

    @Test fun bestOfferPerLine() {
        val all10 = PromoRule(1, 1000, 0)
        val rice20 = PromoRule(2, 0, 2000, productId = 1)
        // rice: 10% = 2000, ৳20 x 2 = 4000 → 4000; soap: 10% = 500
        assertEquals(4500L, PromoMath.offers(listOf(rice, soap), listOf(all10, rice20)))
    }

    @Test fun categoryOfferOnlyCoversItsCategory() {
        assertEquals(1000L, PromoMath.offers(listOf(rice, soap), listOf(PromoRule(3, 2000, 0, category = "cosmetics"))))
    }

    @Test fun fixedOfferNeverExceedsLine() {
        assertEquals(5000L, PromoMath.lineOff(soap, PromoRule(4, 0, 900000)))
    }

    @Test fun couponNeedsMinimum() {
        val c = PromoRule(5, 0, 5000, minPaisa = 50000)
        assertEquals(0L, PromoMath.coupon(40000, c))
        assertEquals(5000L, PromoMath.coupon(60000, c))
        assertEquals(1500L, PromoMath.coupon(10000, PromoRule(6, 1500, 0)))
    }

    @Test fun voucherPaysUpToBalance() {
        assertEquals(3000L, PromoMath.voucher(3000, 10000))
        assertEquals(10000L, PromoMath.voucher(30000, 10000))
    }
}
