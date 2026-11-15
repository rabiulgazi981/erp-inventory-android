package com.bongshaltech.erpinventory.core.stock

import java.time.LocalDate
import java.time.YearMonth

/** Reads an expiry date as people write it on a strip: "12/27", "12/2027", "31/12/2027" or "2027-12-31". */
object Expiry {
    fun parse(text: String): LocalDate? {
        val t = text.trim()
        if (t.isEmpty()) return null
        runCatching { return LocalDate.parse(t) }
        val parts = t.split('/', '-', '.', ' ').filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: return null }
        fun year(y: Int) = if (y < 100) 2000 + y else y
        return runCatching {
            when (parts.size) {
                2 -> YearMonth.of(year(parts[1]), parts[0]).atEndOfMonth()
                3 -> LocalDate.of(year(parts[2]), parts[1], parts[0])
                else -> null
            }
        }.getOrNull()
    }
}
