package com.bongshaltech.erpinventory.data

import android.content.Context
import com.bongshaltech.erpinventory.core.sms.Gateway
import com.bongshaltech.erpinventory.core.sms.GatewayConfig
import com.bongshaltech.erpinventory.core.sms.Provider
import com.bongshaltech.erpinventory.security.SecretStore
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The shop's own SMS gateway account. The API key is the shopkeeper's, typed in More › SMS gateway,
 * and is kept encrypted on this phone only. Nothing goes through Bongshal Tech's servers.
 */
object SmsGateway {
    private const val PROVIDER = "gw_provider"
    private const val KEY = "gw_key"
    private const val SENDER = "gw_sender"
    private const val URL_T = "gw_url"

    sealed interface Result {
        data object Sent : Result
        data object Offline : Result
        data object BadNumber : Result
        data class Refused(val why: String) : Result
    }

    fun load(ctx: Context): GatewayConfig? {
        val p = SecretStore.get(ctx, PROVIDER)?.let { runCatching { Provider.valueOf(it) }.getOrNull() } ?: return null
        return GatewayConfig(p, SecretStore.get(ctx, KEY).orEmpty(), SecretStore.get(ctx, SENDER).orEmpty(), SecretStore.get(ctx, URL_T).orEmpty())
    }

    fun ready(ctx: Context): Boolean = load(ctx)?.ready == true

    fun save(ctx: Context, c: GatewayConfig) {
        SecretStore.put(ctx, PROVIDER, c.provider.name)
        SecretStore.put(ctx, KEY, c.apiKey.trim())
        SecretStore.put(ctx, SENDER, c.senderId.trim())
        SecretStore.put(ctx, URL_T, c.customUrl.trim())
    }

    fun clear(ctx: Context) = listOf(PROVIDER, KEY, SENDER, URL_T).forEach { SecretStore.put(ctx, it, null) }

    suspend fun send(ctx: Context, phone: String, text: String, c: GatewayConfig? = load(ctx)): Result = withContext(Dispatchers.IO) {
        val cfg = c?.takeIf { it.ready } ?: return@withContext Result.Refused("not set up")
        val number = Gateway.bdNumber(phone) ?: return@withContext Result.BadNumber
        try {
            val conn = URL(Gateway.url(cfg, number, text)).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            Gateway.refusal(cfg.provider, code, body)?.let { Result.Refused(it) } ?: Result.Sent
        } catch (e: IOException) {
            Result.Offline
        } catch (e: Exception) {
            Result.Refused(e.message ?: e.javaClass.simpleName)
        }
    }
}
