package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.label.Code128
import com.bongshaltech.erpinventory.core.sheet.ProductImport
import com.bongshaltech.erpinventory.core.sheet.Sheet
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelSheetTest {
    @Test fun everyPatternIsElevenModules() {
        assertEquals(106, Code128.PATTERNS.size)
        Code128.PATTERNS.forEach { assertEquals(it, 11, it.sumOf { c -> c - '0' }) }
        assertEquals(13, Code128.STOP.sumOf { it - '0' })
        assertEquals(106, Code128.PATTERNS.toSet().size)
    }

    @Test fun checksum() {
        // Start B (104) + 1 × 'A' (33) = 137 → 137 mod 103 = 34
        assertEquals(listOf(104, 33, 34), Code128.values("A"))
        assertEquals(11 * 3 + 13, Code128.modules("A").sum())
    }

    @Test fun csvWithQuotes() {
        val rows = Sheet.csv("Name,Price\n\"Soap, big\",\"1,200\"\r\nTea,50\n\n")
        assertEquals(listOf(listOf("Name", "Price"), listOf("Soap, big", "1,200"), listOf("Tea", "50")), rows)
    }

    @Test fun semicolonCsv() = assertEquals(listOf("a", "b"), Sheet.csv("a;b\n1;2").first())

    @Test fun xlsx() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            z.write("""<sst><si><t>Name</t></si><si><t>Price</t></si><si><t>চাল</t></si></sst>""".toByteArray())
            z.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            z.write("""<worksheet><sheetData><row r="1"><c r="A1" t="s"><v>0</v></c><c r="C1" t="s"><v>1</v></c></row>
                <row r="2"><c r="A2" t="s"><v>2</v></c><c r="C2"><v>80.5</v></c></row><row r="3"><c r="A3" t="inlineStr"><is><t>Tea</t></is></c><c r="C3"><v>50</v></c></row></sheetData></worksheet>""".toByteArray())
        }
        val rows = Sheet.read(out.toByteArray())
        assertEquals(listOf("Name", "", "Price"), rows[0])
        assertEquals(listOf("চাল", "", "80.5"), rows[1])
        assertEquals("50", rows[2][2])
    }

    @Test fun importFindsColumns() {
        val rows = ProductImport.rows(listOf(listOf("পণ্যের নাম", "Selling Price", "Stock"), listOf("Tea", "৳1,200", "5"), listOf("", "1", "1")))!!
        assertEquals(1, rows.size)
        assertEquals("1200", rows[0].price)
        assertEquals("5", rows[0].stock)
        assertNull(ProductImport.rows(listOf(listOf("x", "y"))))
        assertTrue(ProductImport.rows(Sheet.csv(ProductImport.TEMPLATE))!!.size == 1)
    }
}
