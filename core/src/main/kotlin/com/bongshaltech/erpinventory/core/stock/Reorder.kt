package com.bongshaltech.erpinventory.core.stock

/** How much to buy so a low product is back to twice its low-stock mark. Milli-units. */
object Reorder {
    fun suggest(stockMilli: Long, lowMilli: Long): Long {
        if (lowMilli <= 0 || stockMilli > lowMilli) return 0
        val need = lowMilli * 2 - stockMilli.coerceAtLeast(0)
        return ((need + 999) / 1000) * 1000
    }
}
