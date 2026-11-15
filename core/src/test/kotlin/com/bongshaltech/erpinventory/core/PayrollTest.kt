package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.hr.Payroll
import org.junit.Assert.assertEquals
import org.junit.Test

class PayrollTest {
    @Test fun fullMonth() = assertEquals(1500000L, Payroll.earned(1500000, 30, 0, 0))
    @Test fun twoAbsentOneHalf() = assertEquals(1375000L, Payroll.earned(1500000, 30, 2, 1))
    @Test fun neverBelowZero() = assertEquals(0L, Payroll.earned(1500000, 30, 40, 0))
}
