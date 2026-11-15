package com.bongshaltech.erpinventory.core.billing

/** Ad-free plan maths. Play Console base plans: 1, 3, 6 and 12 months (owner's prices 250 / 700 / 1300 / 2400 taka). */
object PlanMath {
    /** Months in an ISO-8601 billing period such as P1M, P3M, P6M, P1Y or P52W; 0 when unknown. */
    fun months(period: String): Int {
        val m = Regex("^P(\\d+)([YMW])$").find(period.uppercase()) ?: return 0
        val n = m.groupValues[1].toInt()
        return when (m.groupValues[2]) { "Y" -> n * 12; "M" -> n; else -> (n * 12 + 26) / 52 }
    }

    /** Whole-percent saving of a longer plan against paying monthly for the same months; 0 when none. */
    fun savingPercent(priceMicros: Long, months: Int, monthlyMicros: Long): Int {
        if (months <= 1 || monthlyMicros <= 0) return 0
        val full = monthlyMicros * months
        return if (priceMicros >= full) 0 else ((full - priceMicros) * 100 / full).toInt()
    }
}
