package com.bongshaltech.erpinventory.ui

import com.bongshaltech.erpinventory.core.Money

fun Long.taka(): String = Money(this).format()

/** Stock is stored ×1000. Shows "3" for whole numbers and "0.25" otherwise. */
fun Long.qty(): String =
    if (this % 1000 == 0L) (this / 1000).toString()
    else java.math.BigDecimal(this).movePointLeft(3).stripTrailingZeros().toPlainString()

/** "2.5" → 2500. Returns null for anything that is not a number. */
fun String.toMilli(): Long? = trim().replace(",", "").toBigDecimalOrNull()?.movePointRight(3)?.toLong()

fun String.toPaisa(): Long? = Money.parse(this)?.paisa
