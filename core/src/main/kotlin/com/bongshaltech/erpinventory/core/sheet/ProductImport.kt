package com.bongshaltech.erpinventory.core.sheet

/** One product row from a sheet. Money as typed (taka), quantities as typed. */
data class ProductRow(
    val line: Int, val name: String, val barcode: String, val category: String, val unit: String,
    val cost: String, val price: String, val stock: String, val lowAt: String, val generic: String,
)

/** Finds the columns by their header, in English or Bangla, so people can use their own sheet. */
object ProductImport {
    enum class Field(vararg val names: String) {
        NAME("name", "product", "product name", "item", "নাম", "পণ্য", "পণ্যের নাম"),
        BARCODE("barcode", "code", "sku", "বারকোড", "কোড"),
        CATEGORY("category", "group", "ক্যাটাগরি", "ধরন"),
        UNIT("unit", "ইউনিট", "একক"),
        COST("cost", "buying price", "purchase price", "cost price", "কেনা দাম", "ক্রয় মূল্য"),
        PRICE("price", "selling price", "sale price", "mrp", "বিক্রি দাম", "বিক্রয় মূল্য", "দাম"),
        STOCK("stock", "qty", "quantity", "স্টক", "পরিমাণ"),
        LOW("low stock", "low", "alert", "কম স্টক"),
        GENERIC("generic", "জেনেরিক"),
    }

    private fun norm(s: String) = s.trim().lowercase().replace(Regex("[\\s_]+"), " ").replace(Regex("[(].*[)]"), "").trim()

    fun columns(header: List<String>): Map<Field, Int> {
        val out = mutableMapOf<Field, Int>()
        header.forEachIndexed { i, h ->
            val n = norm(h)
            Field.entries.firstOrNull { f -> f !in out && f.names.any { it == n } }?.let { out[it] = i }
        }
        return out
    }

    /** Rows after the header. Null when there is no name column. */
    fun rows(sheet: List<List<String>>): List<ProductRow>? {
        if (sheet.isEmpty()) return null
        val cols = columns(sheet.first())
        val name = cols[Field.NAME] ?: return null
        fun List<String>.at(f: Field) = cols[f]?.let { getOrNull(it) }.orEmpty().trim()
        return sheet.drop(1).mapIndexedNotNull { i, r ->
            val n = r.getOrNull(name).orEmpty().trim()
            if (n.isEmpty()) null
            else ProductRow(i + 2, n, r.at(Field.BARCODE), r.at(Field.CATEGORY), r.at(Field.UNIT), r.at(Field.COST).filterMoney(),
                r.at(Field.PRICE).filterMoney(), r.at(Field.STOCK), r.at(Field.LOW), r.at(Field.GENERIC))
        }
    }

    private fun String.filterMoney() = replace("৳", "").replace(",", "").replace("tk", "", ignoreCase = true).trim()

    const val TEMPLATE = "Name,Barcode,Category,Unit,Cost,Price,Stock,Low stock\nRice 1kg,8901234567890,Grocery,pcs,70,80,50,10\n"
}
