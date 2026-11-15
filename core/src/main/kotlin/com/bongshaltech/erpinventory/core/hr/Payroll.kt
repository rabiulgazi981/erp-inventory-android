package com.bongshaltech.erpinventory.core.hr

/** Monthly salary less unpaid days. Leave days are paid; a half day loses half a day's pay. */
object Payroll {
    fun earned(monthlyPaisa: Long, daysInMonth: Int, absentDays: Int, halfDays: Int): Long {
        if (monthlyPaisa <= 0 || daysInMonth <= 0) return 0
        val lostHalves = (absentDays * 2L + halfDays).coerceIn(0, daysInMonth * 2L)
        val cut = (monthlyPaisa * lostHalves + daysInMonth) / (daysInMonth * 2L)
        return (monthlyPaisa - cut).coerceAtLeast(0)
    }
}
