package com.bongshaltech.erpinventory.core.sms

import java.net.URLEncoder

/**
 * The shop's own SMS gateway. The shopkeeper buys SMS from a Bangladeshi provider and puts
 * the provider's API key in More › SMS gateway; the app then sends receipts straight to that provider.
 */
enum class Provider(val label: String) {
    ALPHA("Alpha SMS (sms.net.bd)"),
    BULKSMSBD("BulkSMSBD"),
    CUSTOM("Other (my own link)"),
}

data class GatewayConfig(
    val provider: Provider,
    val apiKey: String,
    val senderId: String = "",
    /** CUSTOM only: a GET link with {number}, {message}, {key} and {sender} in it. */
    val customUrl: String = "",
) {
    val ready: Boolean get() = when (provider) {
        Provider.CUSTOM -> customUrl.startsWith("https://") && customUrl.contains("{number}") && customUrl.contains("{message}")
        else -> apiKey.isNotBlank()
    }
}

object Gateway {
    /** 01XXXXXXXXX, +8801XXXXXXXXX or 8801XXXXXXXXX → 8801XXXXXXXXX. Null when it is not a BD mobile number. */
    fun bdNumber(raw: String): String? {
        val d = raw.filter { it.isDigit() }
        val n = when {
            d.length == 11 && d.startsWith("01") -> "88$d"
            d.length == 13 && d.startsWith("8801") -> d
            d.length == 10 && d.startsWith("1") -> "880$d"
            else -> return null
        }
        return n.takeIf { it[4] in '3'..'9' }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun url(c: GatewayConfig, number: String, message: String): String = when (c.provider) {
        Provider.ALPHA -> "https://api.sms.net.bd/sendsms?api_key=${enc(c.apiKey)}&msg=${enc(message)}&to=$number" +
            (if (c.senderId.isNotBlank()) "&sender_id=${enc(c.senderId)}" else "")
        Provider.BULKSMSBD -> "https://bulksmsbd.net/api/smsapi?api_key=${enc(c.apiKey)}&type=text&number=$number" +
            "&senderid=${enc(c.senderId)}&message=${enc(message)}"
        Provider.CUSTOM -> c.customUrl
            .replace("{number}", number)
            .replace("{message}", enc(message))
            .replace("{key}", enc(c.apiKey))
            .replace("{sender}", enc(c.senderId))
    }

    /**
     * Reads the provider's answer. Alpha SMS says {"error":0,...} when it took the message; BulkSMSBD says
     * "response_code":202. Any other provider counts as sent when the link answers with HTTP 2xx.
     * Returns null when sent, or the provider's own words when it refused.
     */
    fun refusal(p: Provider, httpCode: Int, body: String): String? {
        if (httpCode !in 200..299) return "HTTP $httpCode " + body.take(120)
        return when (p) {
            Provider.ALPHA -> {
                val err = Regex("\"error\"\\s*:\\s*\"?(\\d+)").find(body)?.groupValues?.get(1)
                if (err == null || err == "0") null else msg(body) ?: "error $err"
            }
            Provider.BULKSMSBD -> {
                val code = Regex("\"response_code\"\\s*:\\s*\"?(\\d+)").find(body)?.groupValues?.get(1)
                if (code == null || code == "202") null else (Regex("\"error_message\"\\s*:\\s*\"([^\"]*)").find(body)?.groupValues?.get(1)
                    ?.takeIf { it.isNotBlank() } ?: "code $code")
            }
            Provider.CUSTOM -> null
        }
    }

    private fun msg(body: String) = Regex("\"msg\"\\s*:\\s*\"([^\"]*)").find(body)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
}
