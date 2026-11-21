package com.bongshaltech.erpinventory.core.tailor

/**
 * Tailors shops only. An order is a normal sale (making charge + shop fabric + extras, advance as the payment,
 * the rest a due on the customer) with the garments, measurements, delivery date and piece-rate jobs beside it.
 * Labels carry English and Bangla together, like the web dashboard.
 */
object Tailor {
    /** Measurement boxes, in inches (free text so "15½" or "38-40" fit too). */
    val FIELDS: List<Pair<String, String>> = listOf(
        "length" to "Length (লম্বা)", "chest" to "Chest (বুক)", "waist" to "Waist (কোমর / পেট)", "hip" to "Hip (হিপ)",
        "shoulder" to "Shoulder (কাঁধ)", "sleeve" to "Sleeve (হাতা)", "collar" to "Collar / neck (কলার / গলা)",
        "cuff" to "Cuff (মুহুরি / কাফ)", "armhole" to "Armhole (মোড়া)", "bicep" to "Bicep (হাতার মোটা)",
        "thigh" to "Thigh (রান)", "knee" to "Knee (হাঁটু)", "bottom" to "Bottom / leg (পায়ের মুহুরি)",
        "rise" to "Rise / fly (ফ্লাই)", "front" to "Front neck (সামনের গলা)", "back" to "Back neck (পেছনের গলা)",
    )

    data class Garment(val key: String, val label: String, val fields: List<String>)

    val GARMENTS = listOf(
        Garment("SHIRT", "Shirt (শার্ট)", listOf("length", "chest", "waist", "hip", "shoulder", "sleeve", "collar", "cuff", "armhole")),
        Garment("PANT", "Pant (প্যান্ট)", listOf("length", "waist", "hip", "thigh", "knee", "bottom", "rise")),
        Garment("PANJABI", "Panjabi (পাঞ্জাবি)", listOf("length", "chest", "waist", "hip", "shoulder", "sleeve", "collar", "cuff", "armhole")),
        Garment("PAJAMA", "Pajama (পায়জামা)", listOf("length", "waist", "hip", "thigh", "bottom")),
        Garment("BLAZER", "Blazer / coat (ব্লেজার / কোট)", listOf("length", "chest", "waist", "hip", "shoulder", "sleeve", "armhole", "bicep")),
        Garment("KAMEEZ", "Kameez (কামিজ)", listOf("length", "chest", "waist", "hip", "shoulder", "sleeve", "armhole", "front", "back")),
        Garment("SALWAR", "Salwar (সালোয়ার)", listOf("length", "waist", "hip", "bottom")),
        Garment("BLOUSE", "Blouse (ব্লাউজ)", listOf("length", "chest", "waist", "shoulder", "sleeve", "armhole", "front", "back")),
        Garment("OTHER", "Other (অন্যান্য)", listOf("length", "chest", "waist", "hip", "shoulder", "sleeve", "bottom")),
    )

    fun garment(key: String): Garment = GARMENTS.firstOrNull { it.key == key } ?: GARMENTS.last()

    fun garmentLabel(key: String, name: String = ""): String = if (key == "OTHER" && name.isNotBlank()) name.trim() else garment(key).label

    fun fieldLabel(key: String): String = FIELDS.firstOrNull { it.first == key }?.second ?: key

    /** The service product the making charge is billed on: "Shirt making (মজুরি)". */
    fun makingName(key: String, name: String = ""): String {
        val base = if (key == "OTHER" && name.isNotBlank()) name.trim() else garment(key).label.substringBefore(" (")
        return "$base making (মজুরি)"
    }

    val TASKS = listOf("CUTTING" to "Cutting (কাটিং)", "SEWING" to "Sewing (সেলাই)", "FINISHING" to "Finishing (ফিনিশিং)")

    fun taskLabel(t: String): String = TASKS.firstOrNull { it.first == t }?.second ?: t

    val STATUSES = listOf(
        "RECEIVED" to "Received (নেওয়া হয়েছে)", "CUTTING" to "Cutting (কাটিং)", "SEWING" to "Sewing (সেলাই)",
        "READY" to "Ready (তৈরি)", "DELIVERED" to "Delivered (ডেলিভারি)", "CANCELLED" to "Cancelled (বাতিল)",
    )

    /** Statuses moved by hand; Delivered and Cancelled have their own buttons. */
    val WORK_STATUSES = listOf("RECEIVED", "CUTTING", "SEWING", "READY")

    fun statusLabel(s: String): String = STATUSES.firstOrNull { it.first == s }?.second ?: s

    /** Measurements are saved as "key=value" lines; empty values are left out. */
    fun encode(values: Map<String, String>): String =
        values.filter { it.value.isNotBlank() }.entries.joinToString("\n") { "${it.key}=${it.value.trim().replace("\n", " ")}" }

    fun decode(text: String): Map<String, String> =
        text.lines().mapNotNull { l -> l.split('=', limit = 2).takeIf { it.size == 2 && it[1].isNotBlank() }?.let { it[0].trim() to it[1].trim() } }.toMap()

    /** A worker's pay for one piece, per garment and job: "SHIRT|SEWING|8000" lines (paisa). */
    data class Rate(val garment: String, val task: String, val paisa: Long)

    fun encodeRates(rates: List<Rate>): String =
        rates.filter { it.paisa > 0 && it.task.isNotBlank() }.joinToString("\n") { "${it.garment}|${it.task.trim().replace("|", "/")}|${it.paisa}" }

    fun decodeRates(text: String): List<Rate> = text.lines().mapNotNull { l ->
        val p = l.split('|')
        if (p.size == 3) p[2].toLongOrNull()?.let { Rate(p[0], p[1], it) } else null
    }

    fun rateFor(rates: List<Rate>, garment: String, task: String): Long =
        rates.firstOrNull { it.garment == garment && it.task.equals(task, ignoreCase = true) }?.paisa ?: 0

    /** What the shop owes a worker: work done minus pay and advances. Negative means the worker took more than earned. */
    fun owed(earnedPaisa: Long, paidPaisa: Long): Long = earnedPaisa - paidPaisa
}
