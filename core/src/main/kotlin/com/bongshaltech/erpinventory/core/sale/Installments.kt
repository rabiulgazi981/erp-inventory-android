package com.bongshaltech.erpinventory.core.sale

import java.time.LocalDate

/** One planned payment: [seq] from 1, due on [dueDay] (days since 1970-01-01). */
data class PlannedPayment(val seq: Int, val dueDay: Long, val amountPaisa: Long)

/** An instalment as saved: how much it asks for and how much is paid so far. */
data class Instalment(val id: Long, val amountPaisa: Long, val paidPaisa: Long) {
    val leftPaisa get() = (amountPaisa - paidPaisa).coerceAtLeast(0)
}

object Installments {
    /**
     * Splits [duePaisa] into [count] payments, one each month from [first].
     * Every payment is the same whole taka; the last one takes what is left so the total always matches.
     */
    fun plan(duePaisa: Long, count: Int, first: LocalDate): List<PlannedPayment> {
        if (duePaisa <= 0 || count <= 0) return emptyList()
        val n = count.coerceAtMost(60)
        val each = (duePaisa / n / 100) * 100
        return (1..n).map { i ->
            val amount = if (i < n) each else duePaisa - each * (n - 1)
            PlannedPayment(i, first.plusMonths((i - 1).toLong()).toEpochDay(), amount)
        }.filter { it.amountPaisa > 0 }
    }

    /** Fills the oldest unpaid instalments with [paymentPaisa]. Returns the new paid amount for each one it touched. */
    fun fill(oldestFirst: List<Instalment>, paymentPaisa: Long): Map<Long, Long> {
        var left = paymentPaisa
        val out = LinkedHashMap<Long, Long>()
        for (i in oldestFirst) {
            if (left <= 0) break
            val take = minOf(left, i.leftPaisa)
            if (take <= 0) continue
            out[i.id] = i.paidPaisa + take
            left -= take
        }
        return out
    }
}
