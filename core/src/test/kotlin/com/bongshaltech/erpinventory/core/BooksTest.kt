package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.books.AccountType
import com.bongshaltech.erpinventory.core.books.Books
import com.bongshaltech.erpinventory.core.books.JournalLine
import com.bongshaltech.erpinventory.core.books.Recurring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BooksTest {
    @Test fun balancedEntry() {
        assertTrue(Books.isValid(listOf(JournalLine(1, 500L, 0L), JournalLine(2, 0L, 300L), JournalLine(3, 0L, 200L))))
    }

    @Test fun rejectsBadEntries() {
        assertFalse(Books.isValid(listOf(JournalLine(1, 500L, 0L), JournalLine(2, 0L, 400L))))
        assertFalse(Books.isValid(listOf(JournalLine(1, 500L, 0L))))
        assertFalse(Books.isValid(listOf(JournalLine(1, 500L, 500L), JournalLine(2, 0L, 0L))))
        assertFalse(Books.isValid(listOf(JournalLine(1, -5L, 0L), JournalLine(2, 0L, -5L))))
    }

    @Test fun normalSide() {
        assertEquals(300L, Books.balance(AccountType.ASSET, 500L, 200L))
        assertEquals(-300L, Books.balance(AccountType.INCOME, 500L, 200L))
    }

    @Test fun recurringDue() {
        assertTrue(Recurring.isDue(5, 202610, LocalDate.of(2026, 11, 5)))
        assertFalse(Recurring.isDue(5, 202610, LocalDate.of(2026, 11, 4)))
        assertFalse(Recurring.isDue(5, 202611, LocalDate.of(2026, 11, 20)))
        assertTrue(Recurring.isDue(31, 0, LocalDate.of(2026, 2, 28)))
    }
}
