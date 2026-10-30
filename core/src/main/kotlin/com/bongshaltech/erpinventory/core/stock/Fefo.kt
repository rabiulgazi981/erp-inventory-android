package com.bongshaltech.erpinventory.core.stock

import java.time.LocalDate

/** A batch of medicine, scratch cards or anything else with an expiry date. */
data class Batch(val id: Long, val qty: Int, val expiry: LocalDate?)

data class Pick(val batchId: Long, val qty: Int)

/** First-expiry-first-out: sell from the batch that expires soonest. Batches without expiry go last. */
object Fefo {
    /** Returns the picks, or null when stock is not enough. Expired batches are skipped. */
    fun pick(batches: List<Batch>, qty: Int, today: LocalDate): List<Pick>? {
        require(qty > 0)
        val usable = batches
            .filter { it.qty > 0 && (it.expiry == null || !it.expiry.isBefore(today)) }
            .sortedWith(compareBy(nullsLast()) { it.expiry })
        var left = qty
        val picks = mutableListOf<Pick>()
        for (b in usable) {
            if (left == 0) break
            val take = minOf(left, b.qty)
            picks += Pick(b.id, take)
            left -= take
        }
        return if (left == 0) picks else null
    }

    /** Batches that expire within [days] days (and are not yet expired), soonest first. */
    fun expiringSoon(batches: List<Batch>, today: LocalDate, days: Long): List<Batch> =
        batches.filter { b ->
            b.qty > 0 && b.expiry != null && !b.expiry.isBefore(today) && !b.expiry.isAfter(today.plusDays(days))
        }.sortedBy { it.expiry }
}
