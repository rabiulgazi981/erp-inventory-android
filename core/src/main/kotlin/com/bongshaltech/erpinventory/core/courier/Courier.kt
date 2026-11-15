package com.bongshaltech.erpinventory.core.courier

/**
 * The couriers a shop can connect in More › Courier. The shopkeeper opens a merchant account with
 * the courier, copies the API keys from the courier's merchant panel, and puts them here.
 *
 * What each one can do follows the web ERP's courier adapters (apps/api/src/courier/courier-providers.ts):
 * Steadfast has public documentation for booking and status; Pathao, RedX and eCourier have a
 * connection test only; Paperfly keys can be saved but there is no public call to test them.
 * WEBHOOK posts each parcel as JSON to the shop's own link (for any other courier or an automation tool).
 */
enum class CourierKind(val label: String, val fields: List<String>, val canTest: Boolean, val canBook: Boolean) {
    STEADFAST("Steadfast", listOf("apiKey", "secretKey"), canTest = true, canBook = true),
    PATHAO("Pathao", listOf("clientId", "clientSecret", "username", "password"), canTest = true, canBook = false),
    REDX("RedX", listOf("apiToken"), canTest = true, canBook = false),
    PAPERFLY("Paperfly", listOf("username", "password", "apiKey"), canTest = false, canBook = false),
    ECOURIER("eCourier", listOf("userId", "apiKey", "apiSecret"), canTest = true, canBook = false),
    WEBHOOK("Webhook", listOf("url", "secret"), canTest = true, canBook = true);

    /** Fields that may be left empty. */
    fun optional(field: String) = this == WEBHOOK && field == "secret"

    fun ready(values: Map<String, String>): Boolean = fields.all { optional(it) || values[it].orEmpty().isNotBlank() } &&
        (this != WEBHOOK || values["url"].orEmpty().startsWith("https://"))
}

/** Field names that hold secrets, so the screen hides what is typed. */
val secretFields = setOf("secretKey", "clientSecret", "password", "apiToken", "apiSecret", "apiKey", "secret")

data class Parcel(
    val invoice: String,
    val name: String,
    val phone: String,
    val address: String,
    val codPaisa: Long,
    val items: String,
    val note: String = "",
)

object CourierJson {
    fun esc(s: String): String = buildString {
        s.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }

    /** Taka with no paisa when whole (couriers take COD in whole taka; paisa are rounded up). */
    fun codTaka(paisa: Long): Long = (paisa + 99) / 100

    /** 8801XXXXXXXXX or +880… → 01XXXXXXXXX, the form couriers ask for. */
    fun localPhone(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.startsWith("880") && d.length == 13) d.substring(2) else d
    }

    fun steadfastOrder(p: Parcel): String = buildString {
        append("{\"invoice\":\"").append(esc(p.invoice)).append('"')
        append(",\"recipient_name\":\"").append(esc(p.name)).append('"')
        append(",\"recipient_phone\":\"").append(esc(localPhone(p.phone))).append('"')
        append(",\"recipient_address\":\"").append(esc(p.address)).append('"')
        append(",\"cod_amount\":\"").append(codTaka(p.codPaisa)).append('"')
        if (p.note.isNotBlank()) append(",\"note\":\"").append(esc(p.note)).append('"')
        if (p.items.isNotBlank()) append(",\"item_description\":\"").append(esc(p.items.take(250))).append('"')
        append('}')
    }

    fun webhook(p: Parcel, shop: String, test: Boolean = false): String = buildString {
        append("{\"event\":\"").append(if (test) "test" else "parcel.create").append('"')
        append(",\"shop\":\"").append(esc(shop)).append('"')
        append(",\"invoice\":\"").append(esc(p.invoice)).append('"')
        append(",\"recipient_name\":\"").append(esc(p.name)).append('"')
        append(",\"recipient_phone\":\"").append(esc(localPhone(p.phone))).append('"')
        append(",\"recipient_address\":\"").append(esc(p.address)).append('"')
        append(",\"cod_amount\":").append(codTaka(p.codPaisa))
        append(",\"items\":\"").append(esc(p.items)).append('"')
        append(",\"note\":\"").append(esc(p.note)).append('"')
        append('}')
    }
}
