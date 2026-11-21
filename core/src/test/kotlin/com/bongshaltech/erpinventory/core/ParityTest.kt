package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.garment.Garments
import com.bongshaltech.erpinventory.core.stock.ProductCode
import com.bongshaltech.erpinventory.core.stock.Variants
import com.bongshaltech.erpinventory.core.tailor.Tailor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParityTest {
    @Test fun productCodes() {
        assertEquals("P0001", ProductCode.format(1))
        assertEquals("P12345", ProductCode.format(12345))
        assertEquals(12, ProductCode.number("P0012"))
        assertEquals(12, ProductCode.number("p12"))
        assertEquals(12, ProductCode.number(" 12 "))
        assertNull(ProductCode.number("P"))
        assertNull(ProductCode.number("0"))
        assertNull(ProductCode.number("rice"))
        assertEquals("P0012", ProductCode.query("12"))
        assertEquals("", ProductCode.query("Soap 12"))
    }

    @Test fun variants() {
        assertEquals(listOf("S, Red", "S, Blue", "M, Red", "M, Blue"), Variants.combos(listOf(listOf("S", "M"), listOf("Red", "Blue"))))
        assertEquals(listOf("S", "M"), Variants.combos(listOf(listOf("S", "M"), emptyList())))
        assertEquals(emptyList<String>(), Variants.combos(listOf(emptyList(), listOf(" "))))
        assertEquals("T-Shirt (M, Red)", Variants.fullName(" T-Shirt ", "M, Red"))
        assertEquals("T-Shirt", Variants.fullName("T-Shirt", ""))
        assertEquals("T-Shirt", Variants.baseOf("T-Shirt (M, Red)", "M, Red"))
        assertEquals("Odd name", Variants.baseOf("Odd name", "M"))
    }

    @Test fun phones() {
        assertEquals("01711223344", BdPhone.local("+880 1711-223344"))
        assertEquals("01711223344", BdPhone.local("8801711223344"))
        assertEquals("01711223344", BdPhone.local("01711 223344"))
        assertEquals("01711223344", BdPhone.local("1711223344"))
        assertEquals("447700900123", BdPhone.local("+44 7700 900123"))
        assertNull(BdPhone.local("abc"))
    }

    @Test fun tailorText() {
        val m = mapOf("chest" to "40", "waist" to " ", "length" to "30½")
        assertEquals(mapOf("chest" to "40", "length" to "30½"), Tailor.decode(Tailor.encode(m)))
        val rates = listOf(Tailor.Rate("SHIRT", "SEWING", 8000), Tailor.Rate("PANT", "Iron|press", 2000), Tailor.Rate("SHIRT", "CUTTING", 0))
        val back = Tailor.decodeRates(Tailor.encodeRates(rates))
        assertEquals(2, back.size)
        assertEquals(8000, Tailor.rateFor(back, "SHIRT", "sewing"))
        assertEquals(2000, Tailor.rateFor(back, "PANT", "Iron/press"))
        assertEquals(0, Tailor.rateFor(back, "PANT", "SEWING"))
        assertEquals("Shirt making (মজুরি)", Tailor.makingName("SHIRT"))
        assertEquals("Fatua making (মজুরি)", Tailor.makingName("OTHER", "Fatua"))
    }

    @Test fun garmentCosting() {
        val mats = listOf(Garments.Material(1, 1_200, 500), Garments.Material(2, 6_000, 0))
        // 1.2 m of fabric at ৳150 + 5% = ৳189; 6 buttons at ৳1 = ৳6.
        val c = Garments.costing(mats, listOf(Garments.Operation("Sewing", "SEWING", 1500), Garments.Operation("Iron", "FINISHING", 300)),
            listOf(Garments.OtherCost("Thread", 200)), 30_000) { if (it == 1L) 15_000 else 100 }
        assertEquals(19_500, c.materialPaisa)
        assertEquals(1_800, c.labourPaisa)
        assertEquals(21_500, c.totalPaisa)
        assertEquals(8_500, c.profitPaisa)
        assertEquals(2833L, c.marginBp)
        assertEquals(1_260_000, Garments.need(mats[0], 1000))
        assertEquals(listOf(Garments.Cell("S", "Red", 10)), Garments.decodeCells(Garments.encodeCells(listOf(Garments.Cell("S", "Red", 10), Garments.Cell("M", "Red", 0)))))
        assertEquals(33, Garments.pct(1, 3))
        assertEquals(100, Garments.pct(5, 3))
        assertEquals(333, Garments.pieceCost(1000, 3))
        assertEquals(500, Garments.pieceCost(1000, 2))
        assertEquals(mats, Garments.decodeMaterials(Garments.encodeMaterials(mats)))
    }
}
