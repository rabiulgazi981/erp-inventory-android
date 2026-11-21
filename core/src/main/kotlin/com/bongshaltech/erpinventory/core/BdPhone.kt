package com.bongshaltech.erpinventory.core

/** Phone numbers picked from the phone's contacts come as "+880 1711-223344", "01711 223344" and so on. */
object BdPhone {
    /** A Bangladeshi mobile as 01XXXXXXXXX; any other number is returned as its digits (null when there are none). */
    fun local(raw: String): String? {
        val d = raw.filter(Char::isDigit)
        if (d.isEmpty()) return null
        val local = when {
            d.startsWith("880") && d.length == 13 -> "0" + d.drop(3)
            d.startsWith("88") && d.length == 13 -> d.drop(2)
            d.length == 10 && d.startsWith("1") -> "0$d"
            else -> d
        }
        return local
    }
}
