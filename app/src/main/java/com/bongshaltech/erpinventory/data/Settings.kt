package com.bongshaltech.erpinventory.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.bongshaltech.erpinventory.ui.theme.ThemeMode

enum class ShopType { GENERAL, GROCERY, SUPER_SHOP, PHARMACY, RESTAURANT }

/** Menus the owner can allow for a staff member. */
enum class StaffMenu { SALE, CUSTOMERS, PRODUCTS, COST_PROFIT, PURCHASE, EXPENSE, REPORTS, DISCOUNT, MOBILE_BANKING }

/** Prints on the POS memo and the PDF invoice. */
data class BusinessProfile(
    val name: String = "",
    val slogan: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val web: String = "",
    val bin: String = "",
    val thanks: String = "",
    val returnPolicy: String = "",
)

/** Who is using the app right now. One person at a time; sign out lets another person in. */
sealed interface Session {
    data object SignedOut : Session
    data object Owner : Session
    data class Staff(val id: Long) : Session
}

data class AppSettings(
    val onboarded: Boolean,
    val shopType: ShopType,
    val shopCode: String,
    val profile: BusinessProfile,
    val session: Session,
    val installedAt: Long,
    val smsDefaultOn: Boolean,
    val mobileBankingOn: Boolean,
    val theme: ThemeMode,
)

private val Context.store by preferencesDataStore("settings")

class SettingsRepo(private val context: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val shopType = stringPreferencesKey("shop_type")
        val shopCode = stringPreferencesKey("shop_code")
        val session = stringPreferencesKey("session")
        val installedAt = longPreferencesKey("installed_at")
        val smsDefault = booleanPreferencesKey("sms_default_on")
        val mfs = booleanPreferencesKey("mobile_banking_on")
        val theme = stringPreferencesKey("theme")
        val name = stringPreferencesKey("biz_name")
        val slogan = stringPreferencesKey("biz_slogan")
        val address = stringPreferencesKey("biz_address")
        val phone = stringPreferencesKey("biz_phone")
        val email = stringPreferencesKey("biz_email")
        val web = stringPreferencesKey("biz_web")
        val bin = stringPreferencesKey("biz_bin")
        val thanks = stringPreferencesKey("biz_thanks")
        val returnPolicy = stringPreferencesKey("biz_return")
    }

    val settings: Flow<AppSettings> = context.store.data.map(::read)

    suspend fun current(): AppSettings = settings.first()

    private fun read(p: Preferences) = AppSettings(
        onboarded = p[K.onboarded] ?: false,
        shopType = p[K.shopType]?.let { runCatching { ShopType.valueOf(it) }.getOrNull() } ?: ShopType.GENERAL,
        shopCode = p[K.shopCode] ?: "ERP",
        profile = BusinessProfile(
            name = p[K.name] ?: "", slogan = p[K.slogan] ?: "", address = p[K.address] ?: "",
            phone = p[K.phone] ?: "", email = p[K.email] ?: "", web = p[K.web] ?: "", bin = p[K.bin] ?: "",
            thanks = p[K.thanks] ?: "", returnPolicy = p[K.returnPolicy] ?: "",
        ),
        session = when (val s = p[K.session]) {
            null, "" -> Session.SignedOut
            "owner" -> Session.Owner
            else -> s.removePrefix("staff:").toLongOrNull()?.let { Session.Staff(it) } ?: Session.SignedOut
        },
        installedAt = p[K.installedAt] ?: 0L,
        smsDefaultOn = p[K.smsDefault] ?: false,
        mobileBankingOn = p[K.mfs] ?: false,
        theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
    )

    suspend fun ensureInstalledAt() = context.store.edit { if (it[K.installedAt] == null) it[K.installedAt] = System.currentTimeMillis() }

    suspend fun setSession(s: Session) = context.store.edit {
        it[K.session] = when (s) {
            Session.SignedOut -> ""
            Session.Owner -> "owner"
            is Session.Staff -> "staff:${s.id}"
        }
    }

    suspend fun finishOnboarding(type: ShopType, shopName: String, phone: String, address: String, code: String) = context.store.edit {
        it[K.shopType] = type.name
        it[K.name] = shopName
        it[K.phone] = phone
        it[K.address] = address
        it[K.shopCode] = code
        it[K.onboarded] = true
    }

    suspend fun saveProfile(b: BusinessProfile) = context.store.edit {
        it[K.name] = b.name; it[K.slogan] = b.slogan; it[K.address] = b.address; it[K.phone] = b.phone
        it[K.email] = b.email; it[K.web] = b.web; it[K.bin] = b.bin; it[K.thanks] = b.thanks; it[K.returnPolicy] = b.returnPolicy
    }

    suspend fun setSmsDefault(on: Boolean) = context.store.edit { it[K.smsDefault] = on }
    suspend fun setMobileBanking(on: Boolean) = context.store.edit { it[K.mfs] = on }
    suspend fun setTheme(t: ThemeMode) = context.store.edit { it[K.theme] = t.name }
}
