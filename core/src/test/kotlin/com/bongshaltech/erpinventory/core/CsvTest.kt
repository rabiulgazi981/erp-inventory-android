package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.report.Col
import com.bongshaltech.erpinventory.core.report.Csv
import com.bongshaltech.erpinventory.core.report.ReportTable
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {
    @Test fun quotesOnlyWhenNeeded() {
        assertEquals("abc", Csv.cell("abc"))
        assertEquals("\"a,b\"", Csv.cell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.cell("say \"hi\""))
    }

    @Test fun writesRowsAndTotals() {
        val t = ReportTable("T", "", listOf(Col("Name"), Col("Total", true)), listOf(listOf("Rice", "1,200")), listOf("Total", "1,200"))
        assertEquals("﻿Name,Total\r\nRice,\"1,200\"\r\nTotal,\"1,200\"\r\n", Csv.of(t))
    }
}
