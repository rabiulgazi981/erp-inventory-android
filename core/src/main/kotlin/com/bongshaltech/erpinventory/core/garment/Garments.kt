package com.bongshaltech.erpinventory.core.garment

/**
 * Garments factories only. A style holds the costing of one design (material per piece, piece rates per operation,
 * other costs) and its finished goods (one product per size and colour). A production order goes through the stages;
 * material given to the floor and piece-rate pay build its cost (work in progress), which moves into finished stock
 * as pieces are packed.
 */
object Garments {
    val STAGES = listOf(
        "CUTTING" to "Cutting (কাটিং)", "SEWING" to "Sewing (সেলাই)", "FINISHING" to "Finishing / iron (ফিনিশিং)",
        "QC" to "Quality check (QC)", "PACKING" to "Packing (প্যাকিং)",
    )

    fun stageLabel(s: String): String = STAGES.firstOrNull { it.first == s }?.second ?: s

    val STATUSES = listOf(
        "CONFIRMED" to "Confirmed (নতুন)", "IN_PRODUCTION" to "In production (চলছে)", "READY" to "Ready (সব প্যাক)",
        "DELIVERED" to "Delivered (ডেলিভারি)", "CLOSED" to "Closed (বন্ধ)", "CANCELLED" to "Cancelled (বাতিল)",
    )

    val OPEN = setOf("CONFIRMED", "IN_PRODUCTION", "READY")

    fun statusLabel(s: String): String = STATUSES.firstOrNull { it.first == s }?.second ?: s

    val SIZES = listOf("XS", "S", "M", "L", "XL", "XXL", "3XL", "28", "30", "32", "34", "36", "38", "40", "2-3Y", "4-5Y", "6-7Y", "8-9Y", "10-11Y", "12-13Y")

    data class Operation(val name: String, val stage: String, val ratePaisa: Long)

    val OPERATIONS = listOf(
        Operation("Cutting", "CUTTING", 0), Operation("Body sewing", "SEWING", 0), Operation("Collar / neck", "SEWING", 0),
        Operation("Sleeve attach", "SEWING", 0), Operation("Overlock", "SEWING", 0), Operation("Button & hole", "FINISHING", 0),
        Operation("Thread cutting", "FINISHING", 0), Operation("Iron", "FINISHING", 0), Operation("Folding & packing", "PACKING", 0),
    )

    /** Material per piece: [perPieceMilli] of a product (× 1000) plus [wastageBp] (basis points, 500 = 5%). */
    data class Material(val productId: Long, val perPieceMilli: Long, val wastageBp: Int)

    data class OtherCost(val label: String, val paisa: Long)

    fun encodeOps(ops: List<Operation>) = ops.filter { it.name.isNotBlank() }.joinToString("\n") { "${clean(it.name)}|${it.stage}|${it.ratePaisa}" }
    fun decodeOps(text: String) = text.lines().mapNotNull { l ->
        val p = l.split('|'); if (p.size == 3) p[2].toLongOrNull()?.let { Operation(p[0], p[1], it) } else null
    }

    fun encodeMaterials(m: List<Material>) = m.filter { it.perPieceMilli > 0 }.joinToString("\n") { "${it.productId}|${it.perPieceMilli}|${it.wastageBp}" }
    fun decodeMaterials(text: String) = text.lines().mapNotNull { l ->
        val p = l.split('|')
        if (p.size == 3) { val id = p[0].toLongOrNull(); val q = p[1].toLongOrNull(); val w = p[2].toIntOrNull(); if (id != null && q != null && w != null) Material(id, q, w) else null } else null
    }

    fun encodeOther(o: List<OtherCost>) = o.filter { it.label.isNotBlank() && it.paisa > 0 }.joinToString("\n") { "${clean(it.label)}|${it.paisa}" }
    fun decodeOther(text: String) = text.lines().mapNotNull { l ->
        val p = l.split('|'); if (p.size == 2) p[1].toLongOrNull()?.let { OtherCost(p[0], it) } else null
    }

    private fun clean(s: String) = s.trim().replace("|", "/").replace("\n", " ")

    /** Material needed for [pieces] pieces, with wastage, in milli units, rounded up. */
    fun need(m: Material, pieces: Long): Long {
        val raw = m.perPieceMilli * pieces * (10_000 + m.wastageBp)
        return (raw + 9_999) / 10_000
    }

    data class Costing(val materialPaisa: Long, val labourPaisa: Long, val otherPaisa: Long, val pricePaisa: Long) {
        val totalPaisa get() = materialPaisa + labourPaisa + otherPaisa
        val profitPaisa get() = pricePaisa - totalPaisa
        /** Margin in basis points of the price, or null without a price. */
        val marginBp: Long? get() = if (pricePaisa > 0) profitPaisa * 10_000 / pricePaisa else null
    }

    /** Cost of one piece. [unitCost] gives each material's buying price per whole unit (paisa). */
    fun costing(materials: List<Material>, ops: List<Operation>, other: List<OtherCost>, pricePaisa: Long, unitCost: (Long) -> Long): Costing {
        val material = materials.sumOf { m -> (m.perPieceMilli * (10_000 + m.wastageBp) * unitCost(m.productId) + 5_000_000) / 10_000_000 }
        return Costing(material, ops.sumOf { it.ratePaisa }, other.sumOf { it.paisa }, pricePaisa)
    }

    /** Size and colour breakdown "S|Red|120" lines. */
    data class Cell(val size: String, val color: String, val qty: Long)

    fun encodeCells(cells: List<Cell>) = cells.filter { it.qty > 0 }.joinToString("\n") { "${clean(it.size)}|${clean(it.color)}|${it.qty}" }
    fun decodeCells(text: String) = text.lines().mapNotNull { l ->
        val p = l.split('|'); if (p.size == 3) p[2].toLongOrNull()?.let { Cell(p[0], p[1], it) } else null
    }

    /** "S, M, L" → list, keeping the typed order and dropping repeats. */
    fun list(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    /** Percentage done, 0–100. */
    fun pct(done: Long, of: Long): Int = if (of <= 0) 0 else minOf(100, (done * 100 / of).toInt())

    /** Cost carried by each packed piece: work in progress so far shared over the pieces still to pack. */
    fun pieceCost(wipPaisa: Long, piecesLeft: Long): Long = if (piecesLeft <= 0) 0 else (wipPaisa + piecesLeft / 2) / piecesLeft
}
