package com.bongshaltech.erpinventory.core.stock

/** Recipe (bill of materials) maths, in milli-units. */
object Recipe {
    /** How much of an input a run needs: the recipe uses [itemMilli] to make [outMilli]; this run makes [makeMilli]. Rounds half up. */
    fun need(itemMilli: Long, outMilli: Long, makeMilli: Long): Long {
        require(outMilli > 0)
        return (itemMilli * makeMilli * 2 + outMilli) / (outMilli * 2)
    }

    /** Cost of [qtyMilli] at [unitPaisa] per whole unit, rounded half up. */
    fun cost(qtyMilli: Long, unitPaisa: Long): Long = (qtyMilli * unitPaisa + 500) / 1000

    /** True once every line's done plus scrapped quantity reaches what was sent or ordered. */
    fun finished(lines: List<Triple<Long, Long, Long>>): Boolean = lines.all { (qty, done, scrap) -> done + scrap >= qty }
}
