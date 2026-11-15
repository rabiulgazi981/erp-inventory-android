package com.bongshaltech.erpinventory.core.books

import java.time.LocalDate

/** One side of a journal entry, in paisa. A line has a debit or a credit, not both. */
data class JournalLine(val accountId: Long, val debitPaisa: Long, val creditPaisa: Long)

enum class AccountType { ASSET, LIABILITY, EQUITY, INCOME, EXPENSE }

object Books {
    /** A journal entry is valid when it has two or more lines, every line is one-sided and positive, and debits equal credits. */
    fun isValid(lines: List<JournalLine>): Boolean {
        val used = lines.filter { it.debitPaisa != 0L || it.creditPaisa != 0L }
        if (used.size < 2) return false
        if (used.any { it.debitPaisa < 0 || it.creditPaisa < 0 || (it.debitPaisa > 0 && it.creditPaisa > 0) }) return false
        val dr = used.sumOf { it.debitPaisa }
        return dr > 0 && dr == used.sumOf { it.creditPaisa }
    }

    /** Balance on the account's normal side: assets and expenses grow with debits, the rest with credits. */
    fun balance(type: AccountType, debitPaisa: Long, creditPaisa: Long): Long = when (type) {
        AccountType.ASSET, AccountType.EXPENSE -> debitPaisa - creditPaisa
        else -> creditPaisa - debitPaisa
    }
}

object Recurring {
    /**
     * A monthly item set for [dayOfMonth] is due once [today] reaches that day (or the month's last day when shorter)
     * and it hasn't been posted this month. [lastMonth] is yyyyMM of the last post, 0 if never.
     */
    fun isDue(dayOfMonth: Int, lastMonth: Int, today: LocalDate): Boolean {
        val thisMonth = today.year * 100 + today.monthValue
        if (lastMonth >= thisMonth) return false
        val day = dayOfMonth.coerceIn(1, today.lengthOfMonth())
        return today.dayOfMonth >= day
    }
}
