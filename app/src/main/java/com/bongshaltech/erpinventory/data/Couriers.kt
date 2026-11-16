package com.bongshaltech.erpinventory.data

import android.content.Context
import com.bongshaltech.erpinventory.core.courier.CourierJson
import com.bongshaltech.erpinventory.core.courier.CourierKind
import com.bongshaltech.erpinventory.core.courier.Parcel
import com.bongshaltech.erpinventory.security.SecretStore
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The shop's own courier accounts. Keys are typed by the shopkeeper and kept encrypted on this phone;
 * the app talks to the courier directly, never through Bongshal Tech.
 * The calls follow the web ERP's adapters (apps/api/src/courier/courier-providers.ts in erp-inventory).
 */
object Couriers {
    private fun k(c: CourierKind, f: String) = "courier_${c.name}_$f"

    fun values(ctx: Context, c: CourierKind): Map<String, String> = c.fields.associateWith { SecretStore.get(ctx, k(c, it)).orEmpty() }
    fun ready(ctx: Context, c: CourierKind) = c.ready(values(ctx, c))
    fun bookable(ctx: Context) = CourierKind.entries.filter { it.canBook && ready(ctx, it) }

    fun save(ctx: Context, c: CourierKind, v: Map<String, String>) = c.fields.forEach { SecretStore.put(ctx, k(c, it), v[it].orEmpty().trim()) }
    fun clear(ctx: Context, c: CourierKind) = c.fields.forEach { SecretStore.put(ctx, k(c, it), null) }

    data class Check(val ok: Boolean, val detail: String)

    sealed interface Booked {
        data class Ok(val consignmentId: String, val trackingCode: String, val detail: String) : Booked
        data class Duplicate(val detail: String) : Booked
        /** Timeout or odd answer: the parcel may exist. Check the courier's panel before trying again. */
        data class Unknown(val detail: String) : Booked
        data class Refused(val detail: String) : Booked
    }

    private class Reply(val status: Int, val text: String) {
        val json: JSONObject? = runCatching { JSONObject(text) }.getOrNull()
        val array: JSONArray? = runCatching { JSONArray(text) }.getOrNull()
        val ok get() = status in 200..299
    }

    private fun call(url: String, method: String = "GET", headers: Map<String, String> = emptyMap(), body: String? = null): Reply {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.requestMethod = method
        c.setRequestProperty("Accept", "application/json")
        headers.forEach { (h, v) -> c.setRequestProperty(h, v) }
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        c.disconnect()
        return Reply(code, text.take(2000))
    }

    /** The courier's own words, with saved keys taken out in case the courier echoes them. */
    private fun message(r: Reply, secrets: Collection<String>): String {
        var m = r.json?.optString("message").orEmpty().ifBlank { r.text.take(200) }.ifBlank { "No answer text" }
        secrets.filter { it.length > 3 }.forEach { m = m.replace(it, "[hidden]") }
        return "HTTP ${r.status}: ${m.take(200)}"
    }

    private fun failText(e: Exception) = if (e is SocketTimeoutException) "No answer from the courier" else "Could not reach the courier: ${e.message.orEmpty().take(120)}"

    private const val STEADFAST = "https://portal.packzy.com/api/v1"
    private fun sf(v: Map<String, String>) = mapOf("api-key" to v["apiKey"].orEmpty(), "secret-key" to v["secretKey"].orEmpty())

    suspend fun test(ctx: Context, c: CourierKind, v: Map<String, String> = values(ctx, c), shop: String = ""): Check = withContext(Dispatchers.IO) {
        val secrets = v.values
        try {
            when (c) {
                CourierKind.STEADFAST -> {
                    val r = call("$STEADFAST/get_balance", headers = sf(v))
                    val bal = r.json?.opt("current_balance")
                    if (r.ok && r.json?.optInt("status") == 200 && bal != null) Check(true, "Steadfast OK · balance $bal") else Check(false, message(r, secrets))
                }
                CourierKind.PATHAO -> {
                    val body = JSONObject().put("client_id", v["clientId"]).put("client_secret", v["clientSecret"])
                        .put("username", v["username"]).put("password", v["password"]).put("grant_type", "password").toString()
                    val t = call("https://courier-api.pathao.com/aladdin/api/v1/issue-token", "POST", body = body)
                    val token = t.json?.optString("access_token").orEmpty()
                    if (!t.ok || token.isBlank()) return@withContext Check(false, message(t, secrets))
                    val r = call("https://courier-api.pathao.com/aladdin/api/v1/stores", headers = mapOf("Authorization" to "Bearer $token"))
                    val n = r.json?.optJSONObject("data")?.optJSONArray("data")?.length()
                    if (r.ok) Check(true, "Pathao OK" + (n?.let { " · $it store(s)" } ?: "")) else Check(false, message(r, secrets + token))
                }
                CourierKind.REDX -> {
                    val r = call("https://openapi.redx.com.bd/v1.0.0-beta/pickup/stores", headers = mapOf("API-ACCESS-TOKEN" to "Bearer ${v["apiToken"].orEmpty()}"))
                    val n = r.json?.optJSONArray("pickup_stores")?.length()
                    if (r.ok) Check(true, "RedX OK" + (n?.let { " · $it pickup store(s)" } ?: "")) else Check(false, message(r, secrets))
                }
                CourierKind.ECOURIER -> {
                    val r = call("https://backoffice.ecourier.com.bd/api/packages", "POST",
                        mapOf("API-KEY" to v["apiKey"].orEmpty(), "API-SECRET" to v["apiSecret"].orEmpty(), "USER-ID" to v["userId"].orEmpty()), "{}")
                    if (r.ok && r.array != null) Check(true, "eCourier OK · ${r.array!!.length()} package(s)") else Check(false, message(r, secrets))
                }
                CourierKind.WEBHOOK -> {
                    val r = call(v["url"].orEmpty(), "POST", hook(v), CourierJson.webhook(Parcel("TEST", "Test", "01700000000", "Test", 0, ""), shop, test = true))
                    if (r.ok) Check(true, "Webhook OK · HTTP ${r.status}") else Check(false, message(r, secrets))
                }
                CourierKind.PAPERFLY -> Check(false, "Paperfly has no public test call. Keys are saved.")
            }
        } catch (e: IOException) {
            Check(false, failText(e))
        } catch (e: Exception) {
            Check(false, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun hook(v: Map<String, String>) = v["secret"].orEmpty().takeIf { it.isNotBlank() }?.let { mapOf("X-Webhook-Secret" to it) } ?: emptyMap()

    suspend fun book(ctx: Context, c: CourierKind, p: Parcel, shop: String): Booked = withContext(Dispatchers.IO) {
        val v = values(ctx, c)
        try {
            when (c) {
                CourierKind.STEADFAST -> {
                    val r = call("$STEADFAST/create_order", "POST", sf(v), CourierJson.steadfastOrder(p))
                    val con = r.json?.optJSONObject("consignment")
                    val id = con?.opt("consignment_id")?.toString()
                    when {
                        r.ok && r.json?.optInt("status") == 200 && id != null ->
                            Booked.Ok(id, con!!.optString("tracking_code"), r.json!!.optString("message", "Booked"))
                        Regex("already.?exists", RegexOption.IGNORE_CASE).containsMatchIn(r.text) -> Booked.Duplicate(message(r, v.values))
                        r.status >= 500 || r.ok -> Booked.Unknown(message(r, v.values))
                        else -> Booked.Refused(message(r, v.values))
                    }
                }
                CourierKind.WEBHOOK -> {
                    val r = call(v["url"].orEmpty(), "POST", hook(v), CourierJson.webhook(p, shop))
                    val j = r.json
                    val id = listOf("consignment_id", "tracking_code", "id").firstNotNullOfOrNull { j?.opt(it)?.toString()?.takeIf { s -> s.isNotBlank() && s != "null" } }
                    when {
                        r.ok -> Booked.Ok(id ?: p.invoice, j?.optString("tracking_code").orEmpty(), "HTTP ${r.status}")
                        r.status >= 500 -> Booked.Unknown(message(r, v.values))
                        else -> Booked.Refused(message(r, v.values))
                    }
                }
                else -> Booked.Refused("Booking is not available for ${c.label} yet")
            }
        } catch (e: SocketTimeoutException) {
            Booked.Unknown(failText(e))
        } catch (e: IOException) {
            Booked.Refused(failText(e))
        } catch (e: Exception) {
            Booked.Refused(e.message ?: e.javaClass.simpleName)
        }
    }

    /** The courier's own word for where the parcel is, or null when it did not answer clearly. */
    suspend fun status(ctx: Context, c: CourierKind, consignmentId: String): String? = withContext(Dispatchers.IO) {
        if (c != CourierKind.STEADFAST) return@withContext null
        runCatching {
            val r = call("$STEADFAST/status_by_cid/${java.net.URLEncoder.encode(consignmentId, "UTF-8")}", headers = sf(values(ctx, c)))
            r.json?.optString("delivery_status")?.takeIf { r.ok && it.isNotBlank() }
        }.getOrNull()
    }
}
