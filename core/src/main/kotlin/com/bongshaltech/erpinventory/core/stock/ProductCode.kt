package com.bongshaltech.erpinventory.core.stock

/**
 * Product codes P0001, P0002… like the web dashboard: given once when a product is made, never changed or reused.
 * "P0012", "p12" and "12" all mean the same product in a search box.
 */
object ProductCode {
    fun format(n: Int): String = "P" + n.toString().padStart(4, '0')

    /** The number inside a code, or null when [text] is not a product code. */
    fun number(text: String): Int? =
        Regex("^[Pp]?0*(\\d{1,9})$").matchEntire(text.trim())?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }

    /** The code a search text points to ("12" → "P0012"), or "" when it isn't one. */
    fun query(text: String): String = number(text)?.let(::format) ?: ""
}

/** Size / colour style variants: each variant is its own product with its own price, barcode and stock. */
object Variants {
    /** "S, M, L" → ["S", "M", "L"]. */
    fun options(text: String): List<String> = Categories.split(text)

    /** Every combination of the option groups: sizes × colours → ["S, Red", "S, Blue", "M, Red", …]. Empty groups are skipped. */
    fun combos(groups: List<List<String>>): List<String> {
        val used = groups.map { g -> g.map { it.trim() }.filter { it.isNotEmpty() } }.filter { it.isNotEmpty() }
        if (used.isEmpty()) return emptyList()
        return used.fold(listOf("")) { acc, g -> acc.flatMap { a -> g.map { if (a.isEmpty()) it else "$a, $it" } } }
    }

    /** "T-Shirt" + "M, Red" → "T-Shirt (M, Red)". */
    fun fullName(base: String, variant: String): String =
        if (variant.isBlank()) base.trim() else "${base.trim()} (${variant.trim()})"

    /** The style name without its variant part. */
    fun baseOf(name: String, variant: String): String {
        val suffix = " (${variant.trim()})"
        return if (variant.isNotBlank() && name.endsWith(suffix)) name.removeSuffix(suffix) else name
    }
}
