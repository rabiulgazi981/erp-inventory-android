package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.stock.Expiry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiryTest {
    @Test fun monthYear() = assertEquals(LocalDate.of(2027, 12, 31), Expiry.parse("12/27"))
    @Test fun monthFullYear() = assertEquals(LocalDate.of(2027, 2, 28), Expiry.parse("02/2027"))
    @Test fun dayMonthYear() = assertEquals(LocalDate.of(2027, 3, 5), Expiry.parse("5/3/2027"))
    @Test fun iso() = assertEquals(LocalDate.of(2027, 3, 5), Expiry.parse("2027-03-05"))
    @Test fun bad() { assertNull(Expiry.parse("13/27")); assertNull(Expiry.parse("abc")); assertNull(Expiry.parse("")) }
}
