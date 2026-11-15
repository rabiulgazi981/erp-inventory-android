package com.bongshaltech.erpinventory.data

import android.content.Context
import com.bongshaltech.erpinventory.security.SecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Receipt SMS through the shop's erpinventory.bd account. The Alpha SMS key stays on the server;
 * the app only holds the account's sign-in tokens, encrypted on the phone.
 */
object SmsCloud {
    private const val BASE = "https://erpinventory.bd/api/v1"
    private const val ACCESS = "sms_access"
    private const val REFRESH = "sms_refresh"
    private const val EMAIL = "sms_email"
    private val refreshing = Mutex()

    sealed interface Send {
        data class Sent(val parts: Int, val balance: Int) : Send
        /** Use the SIM instead: no credit, rate limit, gateway down, not set up, or no internet. */
        data class UseSim(val reason: Reason) : Send
        /** The message itself is wrong (bad number or text); the SIM would not help. */
        data class Failed(val message: String) : Send
    }

    enum class Reason { NO_BALANCE, BUSY, GATEWAY, NOT_SET_UP, OFFLINE, SIGNED_OUT }

    sealed interface Login {
        data object Ok : Login
        data object WrongPassword : Login
        data object NoShop : Login
        data object Offline : Login
        data class Error(val message: String) : Login
    }

    data class Balance(val balance: Int, val lastTopups: List<Pair<Int, String>>)

    private class Resp(val code: Int, val body: JSONObject)

    fun signedInEmail(ctx: Context): String? = SecretStore.get(ctx, EMAIL)?.takeIf { SecretStore.get(ctx, REFRESH) != null }

    private fun call(method: String, path: String, body: JSONObject?, token: String?): Resp {
        val c = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 10_000
            c.readTimeout = 20_000
            c.setRequestProperty("Accept", "application/json")
            token?.let { c.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Resp(code, runCatching { JSONObject(text) }.getOrDefault(JSONObject()))
        } finally {
            c.disconnect()
        }
    }

    private fun saveTokens(ctx: Context, j: JSONObject) {
        // Tokens rotate: keep the new refresh token before anything else uses it.
        SecretStore.put(ctx, REFRESH, j.getString("refreshToken"))
        SecretStore.put(ctx, ACCESS, j.getString("accessToken"))
    }

    suspend fun login(ctx: Context, email: String, password: String): Login = withContext(Dispatchers.IO) {
        try {
            val r = call("POST", "/auth/login", JSONObject().put("email", email.trim()).put("password", password).put("client", "MOBILE"), null)
            when {
                r.code in 200..299 -> {
                    saveTokens(ctx, r.body)
                    SecretStore.put(ctx, EMAIL, email.trim())
                    // An account without a business cannot send.
                    val me = withAuth(ctx) { call("GET", "/auth/me", null, it) }
                    if (me != null && me.code == 403 && me.body.optString("code") == "NO_TENANT") { signOutLocal(ctx); Login.NoShop } else Login.Ok
                }
                r.code == 401 || r.code == 400 -> Login.WrongPassword
                else -> Login.Error(r.body.optString("message").ifBlank { "HTTP ${r.code}" })
            }
        } catch (e: IOException) {
            Login.Offline
        }
    }

    /** Runs [block] with a fresh access token, refreshing once on 401. Null when signed out. */
    private suspend fun withAuth(ctx: Context, block: (String) -> Resp): Resp? {
        val token = SecretStore.get(ctx, ACCESS) ?: return null
        val first = block(token)
        if (first.code != 401) return first
        val fresh = refreshing.withLock {
            // Another call may have refreshed while this one waited.
            SecretStore.get(ctx, ACCESS)?.takeIf { it != token } ?: run {
                val rt = SecretStore.get(ctx, REFRESH) ?: return@run null
                val r = call("POST", "/auth/refresh", JSONObject().put("refreshToken", rt), null)
                if (r.code in 200..299) { saveTokens(ctx, r.body); r.body.getString("accessToken") } else { signOutLocal(ctx); null }
            }
        } ?: return null
        return block(fresh)
    }

    suspend fun balance(ctx: Context): Balance? = withContext(Dispatchers.IO) {
        runCatching {
            val r = withAuth(ctx) { call("GET", "/sms/balance", null, it) } ?: return@runCatching null
            if (r.code !in 200..299) return@runCatching null
            val tops = r.body.optJSONArray("lastTopups")
            Balance(
                r.body.optInt("balance"),
                (0 until (tops?.length() ?: 0)).map { i -> tops!!.getJSONObject(i).let { it.optInt("credits") to it.optString("at") } },
            )
        }.getOrNull()
    }

    /** [key] must stay the same when the same receipt is retried, so it is never sent or charged twice. */
    suspend fun send(ctx: Context, to: String, message: String, key: String): Send = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("to", to).put("message", message).put("idempotencyKey", key)
            val r = withAuth(ctx) { call("POST", "/sms/receipt", body, it) } ?: return@withContext Send.UseSim(Reason.SIGNED_OUT)
            when (r.code) {
                in 200..299 -> Send.Sent(r.body.optInt("parts", 1), r.body.optInt("balance"))
                402 -> Send.UseSim(Reason.NO_BALANCE)
                409, 429 -> Send.UseSim(Reason.BUSY)
                502 -> Send.UseSim(Reason.GATEWAY)
                503 -> Send.UseSim(Reason.NOT_SET_UP)
                401 -> Send.UseSim(Reason.SIGNED_OUT)
                else -> Send.Failed(r.body.optString("message").ifBlank { "HTTP ${r.code}" })
            }
        } catch (e: IOException) {
            Send.UseSim(Reason.OFFLINE)
        }
    }

    suspend fun signOut(ctx: Context) = withContext(Dispatchers.IO) {
        runCatching { SecretStore.get(ctx, ACCESS)?.let { call("POST", "/auth/logout", null, it) } }
        signOutLocal(ctx)
    }

    private fun signOutLocal(ctx: Context) {
        SecretStore.put(ctx, ACCESS, null); SecretStore.put(ctx, REFRESH, null)
    }
}
