package com.bongshaltech.erpinventory.core.sms

/**
 * Counts SMS parts the way operators bill them, so shopkeepers never lose balance by surprise.
 *
 * GSM-7: 160 chars in one SMS, 153 per part when longer; extension chars (€ [ ] { } ^ ~ \ | and form feed) cost 2.
 * Anything outside GSM-7 (Bangla, ৳, emoji, curly quotes) turns the whole message into UCS-2: 70, then 67 per part.
 * The Firebase function recounts with the same rules before sending.
 */
object SmsCounter {

    private const val BASIC =
        "@£\$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?" +
            "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà"
    private const val EXTENSION = "\u000C^{}\\[~]|€"

    private val basicSet = BASIC.toSet()
    private val extensionSet = EXTENSION.toSet()

    /** Hard cap so one tap can never burn a pile of balance. */
    const val MAX_PARTS = 6

    enum class Encoding { GSM7, UNICODE }

    data class Count(
        val encoding: Encoding,
        /** Units used: GSM septets, or UTF-16 code units for Unicode. */
        val units: Int,
        val parts: Int,
        val perPart: Int,
        /** Units left before the next part starts. */
        val remainingInPart: Int,
        /** Characters that forced Unicode in an otherwise English message. */
        val unicodeCulprits: List<Char>,
    ) {
        val tooLong get() = parts > MAX_PARTS
    }

    fun isGsm(c: Char) = c in basicSet || c in extensionSet

    fun count(text: String): Count {
        if (text.isEmpty()) return Count(Encoding.GSM7, 0, 0, 160, 160, emptyList())
        val culprits = text.filterNot(::isGsm).toList()
        return if (culprits.isEmpty()) {
            val units = text.fold(0) { acc, c -> acc + if (c in extensionSet) 2 else 1 }
            build(Encoding.GSM7, units, single = 160, multi = 153, culprits = emptyList())
        } else {
            // A message that is mostly Bangla is meant to be Unicode; only flag culprits in mostly-English text.
            val mostlyGsm = culprits.size * 5 < text.length
            build(Encoding.UNICODE, text.length, single = 70, multi = 67, culprits = if (mostlyGsm) culprits.distinct() else emptyList())
        }
    }

    private fun build(encoding: Encoding, units: Int, single: Int, multi: Int, culprits: List<Char>): Count {
        val parts = if (units <= single) 1 else (units + multi - 1) / multi
        val perPart = if (parts == 1) single else multi
        val remaining = parts * perPart - units
        return Count(encoding, units, parts, perPart, remaining, culprits)
    }

    private val fixes = mapOf(
        '৳' to "Tk", '‘' to "'", '’' to "'", '“' to "\"", '”' to "\"",
        '–' to "-", '—' to "-", '…' to "...", ' ' to " ", '•' to "-",
    )

    /** One-tap fix: swaps look-alike characters for GSM ones. Leaves Bangla and emoji alone. */
    fun fixToGsm(text: String): String = buildString {
        for (c in text) append(fixes[c] ?: c.toString())
    }

    /** Credits needed for sending [text] to [recipients] numbers. */
    fun creditsNeeded(text: String, recipients: Int): Int = count(text).parts * recipients
}
