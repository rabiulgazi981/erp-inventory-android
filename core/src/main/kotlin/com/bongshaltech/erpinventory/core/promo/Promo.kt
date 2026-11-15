package com.bongshaltech.erpinventory.core.promo

/** One cart line as offers see it. Money in paisa, quantity in milli-units. */
data class PromoLine(val productId: Long, val category: String, val qtyMilli: Long, val totalPaisa: Long)

/**
 * A rule worth some money off. [percentBp] is basis points (1000 = 10%); when it is 0, [fixedPaisa] is taken off
 * instead: per unit for an offer, once per bill for a coupon. An offer with [productId] or [category] set only
 * covers those lines; with both empty it covers everything.
 */
data class PromoRule(
    val id: Long,
    val percentBp: Int,
    val fixedPaisa: Long,
    val productId: Long? = null,
    val category: String = "",
    val minPaisa: Long = 0,
)

object PromoMath {
    private fun pct(amount: Long, bp: Int) = (amount * bp + 5000) / 10000

    /** Money off one line from one offer, never more than the line. */
    fun lineOff(line: PromoLine, offer: PromoRule): Long {
        val covers = when {
            offer.productId != null -> offer.productId == line.productId
            offer.category.isNotBlank() -> offer.category.equals(line.category, ignoreCase = true)
            else -> true
        }
        if (!covers || line.totalPaisa <= 0) return 0
        val off = if (offer.percentBp > 0) pct(line.totalPaisa, offer.percentBp) else (offer.fixedPaisa * line.qtyMilli + 500) / 1000
        return off.coerceIn(0, line.totalPaisa)
    }

    /** Automatic offers: each line gets the single best offer that covers it. */
    fun offers(lines: List<PromoLine>, offers: List<PromoRule>): Long =
        lines.sumOf { l -> offers.maxOfOrNull { lineOff(l, it) } ?: 0 }

    /** A coupon on what is left of the bill; 0 when the bill is under the coupon's minimum. */
    fun coupon(billPaisa: Long, coupon: PromoRule): Long {
        if (billPaisa <= 0 || billPaisa < coupon.minPaisa) return 0
        val off = if (coupon.percentBp > 0) pct(billPaisa, coupon.percentBp) else coupon.fixedPaisa
        return off.coerceIn(0, billPaisa)
    }

    /** A gift voucher pays what it can of the bill from its balance. */
    fun voucher(billPaisa: Long, balancePaisa: Long): Long = minOf(billPaisa, balancePaisa).coerceAtLeast(0)
}
