package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.ads.AdConfig
import com.bongshaltech.erpinventory.core.ads.AdPacing
import com.bongshaltech.erpinventory.core.ads.AdState
import com.bongshaltech.erpinventory.core.ads.Placement
import com.bongshaltech.erpinventory.core.mfs.BalanceCounterparty
import com.bongshaltech.erpinventory.core.mfs.MfsCalculator
import com.bongshaltech.erpinventory.core.mfs.MfsInput
import com.bongshaltech.erpinventory.core.mfs.MfsSmsParser
import com.bongshaltech.erpinventory.core.mfs.MfsType
import com.bongshaltech.erpinventory.core.sms.SmsCounter
import com.bongshaltech.erpinventory.core.stock.Batch
import com.bongshaltech.erpinventory.core.stock.Fefo
import com.bongshaltech.erpinventory.core.stock.Pick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoneyTest {
    @Test fun formatsAndParses() {
        assertEquals("৳ 1,850.00", Money.taka(1850).format())
        assertEquals(Money(185050), Money.parse("1,850.50"))
        assertEquals(Money.taka(200), Money.parse("৳ 200"))
        assertNull(Money.parse("abc"))
        assertEquals(Money(280_00), Money.taka(10_000).percent(2.8))
    }
}

class SmsCounterTest {
    @Test fun englishFitsOneThenSplitsAt153() {
        assertEquals(1, SmsCounter.count("a".repeat(160)).parts)
        assertEquals(2, SmsCounter.count("a".repeat(161)).parts)
        assertEquals(3, SmsCounter.count("a".repeat(307)).parts)
    }

    @Test fun extensionCharsCountTwo() {
        val c = SmsCounter.count("[".repeat(80))
        assertEquals(160, c.units)
        assertEquals(1, c.parts)
    }

    @Test fun banglaUsesUnicodeLimits() {
        assertEquals(1, SmsCounter.count("ক".repeat(70)).parts)
        val c = SmsCounter.count("ক".repeat(88))
        assertEquals(SmsCounter.Encoding.UNICODE, c.encoding)
        assertEquals(2, c.parts)
        assertEquals(18, SmsCounter.creditsNeeded("ক".repeat(88), 9))
    }

    @Test fun takaSignFlaggedAndFixed() {
        val msg = "Dear customer, your due is ৳ 500. Please pay soon. Thanks"
        val c = SmsCounter.count(msg)
        assertEquals(SmsCounter.Encoding.UNICODE, c.encoding)
        assertEquals(listOf('৳'), c.unicodeCulprits)
        val fixed = SmsCounter.fixToGsm(msg)
        assertEquals(SmsCounter.Encoding.GSM7, SmsCounter.count(fixed).encoding)
        assertTrue(fixed.contains("Tk 500"))
    }
}

class AdPacingTest {
    private val day = 24L * 60 * 60 * 1000
    private fun state(days: Long, shown: Int = 0, lastMinAgo: Long? = null, records: Int = 0, online: Boolean = true, adFree: Boolean = false, sensitive: Boolean = false, justOpened: Boolean = false): AdState {
        val now = 100 * day
        return AdState(now - days * day, now, online, adFree, sensitive, shown, lastMinAgo?.let { now - it * 60_000 }, records, justOpened)
    }
    private val cfg = AdConfig()

    @Test fun quietFirstWeek() {
        assertFalse(AdPacing.canShow(Placement.HOME_BANNER, cfg, state(days = 3)))
        assertTrue(AdPacing.canShow(Placement.HOME_BANNER, cfg, state(days = 8)))
    }

    @Test fun neverOfflineAdFreeOrSensitive() {
        assertFalse(AdPacing.canShow(Placement.HOME_BANNER, cfg, state(days = 30, online = false)))
        assertFalse(AdPacing.canShow(Placement.HOME_BANNER, cfg, state(days = 30, adFree = true)))
        assertFalse(AdPacing.canShow(Placement.HOME_BANNER, cfg, state(days = 30, sensitive = true)))
        assertFalse(AdPacing.canShow(Placement.INTERSTITIAL_AFTER_TASK, cfg, state(days = 30, justOpened = true)))
    }

    @Test fun capGrowsWithUseButStopsAtMax() {
        assertEquals(1, AdPacing.dailyInterstitialCap(cfg, state(days = 30)))
        assertEquals(3, AdPacing.dailyInterstitialCap(cfg, state(days = 30, records = 80)))
        assertEquals(6, AdPacing.dailyInterstitialCap(cfg, state(days = 30, records = 5000)))
        assertFalse(AdPacing.canShow(Placement.INTERSTITIAL_AFTER_TASK, cfg, state(days = 30, shown = 1)))
    }

    @Test fun interstitialGap() {
        assertFalse(AdPacing.canShow(Placement.INTERSTITIAL_AFTER_TASK, cfg, state(days = 30, records = 200, shown = 1, lastMinAgo = 5)))
        assertTrue(AdPacing.canShow(Placement.INTERSTITIAL_AFTER_TASK, cfg, state(days = 30, records = 200, shown = 1, lastMinAgo = 20)))
    }
}

class DocNumberTest {
    @Test fun formats() {
        assertEquals("DMS-26-0001", DocNumber.format("dms", DocType.SALE, 2026, 1))
        assertEquals("DMS-PUR-26-0021", DocNumber.format("DMS", DocType.PURCHASE, 2026, 21))
        assertEquals(1, DocNumber.next(null))
    }
}

class FefoTest {
    private val today = LocalDate.of(2026, 10, 30)
    @Test fun sellsEarliestExpiryAndSkipsExpired() {
        val batches = listOf(
            Batch(1, 10, LocalDate.of(2027, 5, 1)),
            Batch(2, 3, LocalDate.of(2026, 11, 8)),
            Batch(3, 50, LocalDate.of(2026, 10, 1)), // expired
            Batch(4, 5, null),
        )
        assertEquals(listOf(Pick(2, 3), Pick(1, 2)), Fefo.pick(batches, 5, today))
        assertNull(Fefo.pick(batches, 100, today))
        assertEquals(listOf(2L), Fefo.expiringSoon(batches, today, 15).map { it.id })
    }
}

class MfsTest {
    @Test fun cashInAndOut() {
        val inn = MfsCalculator.effect(MfsInput.Simple(MfsType.CASH_IN, Money.taka(5000), 0.4))
        assertEquals(Money.taka(-5000), inn.walletDelta); assertEquals(Money.taka(5000), inn.cashDelta); assertEquals(Money.taka(20), inn.earning)
        val out = MfsCalculator.effect(MfsInput.Simple(MfsType.CASH_OUT, Money.taka(1000), 0.41))
        assertEquals(Money.taka(1000), out.walletDelta); assertEquals(Money.taka(-1000), out.cashDelta); assertEquals(Money(410), out.earning)
    }

    @Test fun payBill() {
        val e = MfsCalculator.effect(MfsInput.PayBill(Money.taka(1850), Money.taka(20), Money.taka(10)))
        assertEquals(Money.taka(-1860), e.walletDelta); assertEquals(Money.taka(1870), e.cashDelta); assertEquals(Money.taka(10), e.earning)
    }

    @Test fun loadBoughtAtDiscount() {
        val e = MfsCalculator.effect(MfsInput.Balance(true, Money.taka(10000), Money.taka(9720), BalanceCounterparty.DSO))
        assertEquals(Money.taka(10000), e.walletDelta); assertEquals(Money.taka(-9720), e.cashDelta); assertEquals(Money.taka(280), e.earning)
        val bank = MfsCalculator.effect(MfsInput.Balance(false, Money.taka(50000), Money.taka(50000), BalanceCounterparty.BANK))
        assertEquals(Money.ZERO, bank.cashDelta)
    }

    @Test fun scratchCardSale() {
        val e = MfsCalculator.effect(MfsInput.StockSale(Money.taka(50), Money.taka(48.5), 2))
        assertEquals(Money.taka(100), e.cashDelta); assertEquals(Money.taka(3), e.earning); assertEquals(-2, e.stockDelta)
    }

    @Test fun parsesMaskedSms() {
        val p = MfsSmsParser.parse("Cash In Tk 5,000.00 to 01711***567 successful. Fee Tk 0.00. Balance Tk 48,250.00. TrxID BK7X3QF92A at 30/10/2026 14:41")
        assertEquals(Money.taka(5000), p.amount)
        assertEquals("BK7X3QF92A", p.trxId)
        assertEquals("01711***567", p.maskedNumber)
        assertNull(p.number)
        assertEquals("567", p.visibleTail)
        assertEquals(listOf("01711-223567"), MfsSmsParser.matchMasked("01711***567", listOf("01711-223567", "01811-223567", "01711-223568")))
    }

    @Test fun parsesBillFee() {
        val p = MfsSmsParser.parse("Bill payment Tk 1,850.00 for acc 12045587 successful. Fee Tk 10. TrxID BK7X3QF92A")
        assertEquals(Money.taka(1850), p.amount)
        assertEquals(Money.taka(10), p.fee)
    }
}
