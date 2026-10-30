package com.bongshaltech.erpinventory.core

/**
 * Document numbers follow the ERP rule: sales are {CODE}-{YY}-{0001}, other documents {CODE}-{DOC}-{YY}-{0001}.
 * Each document type has its own counter that restarts every year. Old numbers are never renumbered.
 */
enum class DocType(val tag: String?) {
    SALE(null), PURCHASE("PUR"), EXPENSE("EXP"), RETURN("RET"), PAYMENT("PAY"), MFS("MFS"), KOT("KOT")
}

object DocNumber {
    fun format(shopCode: String, type: DocType, year: Int, serial: Int): String {
        val yy = (year % 100).toString().padStart(2, '0')
        val seq = serial.toString().padStart(4, '0')
        val code = shopCode.uppercase().filter { it.isLetterOrDigit() }.ifEmpty { "ERP" }
        return listOfNotNull(code, type.tag, yy, seq).joinToString("-")
    }

    /** Next serial given the last one used this year (null when none yet). */
    fun next(lastSerialThisYear: Int?) = (lastSerialThisYear ?: 0) + 1
}
