package com.bongshaltech.erpinventory.core.stock

/**
 * Weighing-scale labels (EAN-13 starting with 20-29): 2 digits prefix, 5 digits item code (PLU),
 * 5 digits weight in grams or price in taka×100 (paisa), 1 check digit.
 */
object ScaleBarcode {
    data class Read(val plu: String, val value: Long)

    fun read(code: String): Read? {
        val c = code.trim()
        if (c.length != 13 || !c.all(Char::isDigit) || c[0] != '2') return null
        if (checkDigit(c.substring(0, 12)) != c[12].digitToInt()) return null
        return Read(c.substring(2, 7), c.substring(7, 12).toLong())
    }

    fun checkDigit(first12: String): Int {
        val sum = first12.mapIndexed { i, ch -> ch.digitToInt() * if (i % 2 == 0) 1 else 3 }.sum()
        return (10 - sum % 10) % 10
    }

    /** Quantity in milli-units: grams are already milli-kg; a price is turned into quantity at the product's price. */
    fun qtyMilli(r: Read, byPrice: Boolean, unitPricePaisa: Long): Long =
        if (!byPrice) r.value
        else if (unitPricePaisa <= 0) 0
        else (r.value * 1000 + unitPricePaisa / 2) / unitPricePaisa
}
