package com.bongshaltech.erpinventory.core

/**
 * Money is kept as whole paisa (1 Taka = 100 paisa) so sums never drift like Double does.
 */
@JvmInline
value class Money(val paisa: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(paisa + other.paisa)
    operator fun minus(other: Money) = Money(paisa - other.paisa)
    operator fun times(qty: Int) = Money(paisa * qty)
    operator fun unaryMinus() = Money(-paisa)
    override fun compareTo(other: Money) = paisa.compareTo(other.paisa)

    /** Percentage of this amount, rounded half up to the nearest paisa. [percent] is e.g. 2.8 for 2.8%. */
    fun percent(percent: Double): Money = Money(Math.round(paisa * percent / 100.0))

    val isZero get() = paisa == 0L

    /** "1,850.00" style, Bangladeshi users read western grouping on receipts. */
    fun format(withSymbol: Boolean = true): String {
        val abs = kotlin.math.abs(paisa)
        val whole = abs / 100
        val frac = abs % 100
        val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
        val sign = if (paisa < 0) "-" else ""
        val body = "$grouped.${frac.toString().padStart(2, '0')}"
        return if (withSymbol) "$sign৳ $body" else "$sign$body"
    }

    override fun toString() = format()

    companion object {
        val ZERO = Money(0)
        fun taka(taka: Long) = Money(taka * 100)
        fun taka(taka: Double) = Money(Math.round(taka * 100))

        /** Parses what a shopkeeper types: "1,850", "1850.5", "৳ 200", "Tk 50". Returns null for junk. */
        fun parse(text: String): Money? {
            val cleaned = text.replace("৳", "").replace("Tk", "", ignoreCase = true)
                .replace(",", "").trim()
            if (cleaned.isEmpty()) return null
            val value = cleaned.toBigDecimalOrNull() ?: return null
            return Money(value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong())
        }
    }
}

fun Iterable<Money>.sum(): Money = Money(sumOf { it.paisa })
