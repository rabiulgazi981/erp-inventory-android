package com.bongshaltech.erpinventory.core.stock

/** A product can sit in several categories, saved as one comma-separated text ("Food, Cat"). */
object Categories {
    fun split(text: String): List<String> =
        text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    fun join(list: List<String>): String = list.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.joinToString(", ")

    fun has(text: String, category: String): Boolean = split(text).any { it.equals(category.trim(), ignoreCase = true) }
}
