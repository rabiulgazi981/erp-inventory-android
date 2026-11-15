package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.sale.Instalment
import com.bongshaltech.erpinventory.core.sale.Installments
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InstallmentsTest {
    @Test fun splitsIntoWholeTakaAndLastTakesTheRest() {
        val p = Installments.plan(1000_00L, 3, LocalDate.of(2026, 1, 31))
        assertEquals(listOf(333_00L, 333_00L, 334_00L), p.map { it.amountPaisa })
        assertEquals(1000_00L, p.sumOf { it.amountPaisa })
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), p[1].dueDay)
        assertEquals(listOf(1, 2, 3), p.map { it.seq })
    }

    @Test fun nothingToPlan() {
        assertEquals(0, Installments.plan(0L, 3, LocalDate.of(2026, 1, 1)).size)
        assertEquals(0, Installments.plan(500L, 0, LocalDate.of(2026, 1, 1)).size)
    }

    @Test fun smallDueStillAddsUp() {
        val p = Installments.plan(150L, 3, LocalDate.of(2026, 1, 1))
        assertEquals(150L, p.sumOf { it.amountPaisa })
    }

    @Test fun paymentFillsOldestFirst() {
        val list = listOf(Instalment(1, 300_00L, 300_00L), Instalment(2, 300_00L, 100_00L), Instalment(3, 300_00L, 0L))
        val got = Installments.fill(list, 350_00L)
        assertEquals(mapOf(2L to 300_00L, 3L to 150_00L), got)
    }

    @Test fun overpaymentStopsAtTheEnd() {
        val got = Installments.fill(listOf(Instalment(1, 100L, 0L)), 500L)
        assertEquals(mapOf(1L to 100L), got)
    }
}
