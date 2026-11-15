package com.bongshaltech.erpinventory.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.bongshaltech.erpinventory.ui.theme.ThemeMode

enum class ShopType { GENERAL, GROCERY, SUPER_SHOP, PHARMACY, RESTAURANT }

/**
 * Optional modules, like the web dashboard's Settings > Features. Each is off until the owner turns it on,
 * except the module that belongs to the shop's own business type. Turning one off only hides it; no data is removed.
 */
enum class Feature { CASH_DAY, LOANS, PROMOS, HR, ORDERS, TABLES, BATCHES, REPACK, SERIALS, WAREHOUSES, MOBILE_BANKING, INSTALLMENTS, SCALE_BARCODE, PACKS }

fun defaultFeatures(type: ShopType): Set<Feature> = when (type) {
    ShopType.RESTAURANT -> setOf(Feature.TABLES)
    ShopType.PHARMACY -> setOf(Feature.BATCHES)
    else -> emptySet()
}

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
    /** When the owner last saved a backup file, 0 if never. */
    val lastBackupAt: Long,
    /** Paired Bluetooth receipt printer, empty when none is chosen. */
    val printerAddress: String,
    val printerName: String,
    /** Paper roll width: 58 or 80 mm. */
    val paperMm: Int,
    /** Ad-free subscription active, as last confirmed by Google Play. */
    val adFree: Boolean,
    /** Optional modules that are on. Mobile banking keeps its own older switch and is merged in here. */
    val features: Set<Feature>,
    /** Scale labels carry the price instead of the weight. */
    val scaleByPrice: Boolean = false,
) {
    fun on(f: Feature) = f in features
}

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
        val lastBackup = longPreferencesKey("last_backup_at")
        val printerAddress = stringPreferencesKey("printer_address")
        val printerName = stringPreferencesKey("printer_name")
        val paperMm = intPreferencesKey("paper_mm")
        val adFree = booleanPreferencesKey("ad_free")
        val features = stringSetPreferencesKey("features_on")
        val scaleByPrice = booleanPreferencesKey("scale_by_price")
    }

    val settings: Flow<AppSettings> = context.store.data.map(::read)

    suspend fun current(): AppSettings = settings.first()

    private fun read(p: Preferences): AppSettings {
        val type = p[K.shopType]?.let { runCatching { ShopType.valueOf(it) }.getOrNull() } ?: ShopType.GENERAL
        val mfs = p[K.mfs] ?: false
        val chosen = p[K.features]?.mapNotNull { runCatching { Feature.valueOf(it) }.getOrNull() }?.toSet() ?: defaultFeatures(type)
        val features = (chosen - Feature.MOBILE_BANKING) + (if (mfs) setOf(Feature.MOBILE_BANKING) else emptySet())
        return readWith(p, type, mfs, features)
    }

    private fun readWith(p: Preferences, type: ShopType, mfs: Boolean, features: Set<Feature>) = AppSettings(
        onboarded = p[K.onboarded] ?: false,
        shopType = type,
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
        mobileBankingOn = mfs,
        theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        lastBackupAt = p[K.lastBackup] ?: 0L,
        printerAddress = p[K.printerAddress] ?: "",
        printerName = p[K.printerName] ?: "",
        paperMm = p[K.paperMm] ?: 58,
        adFree = p[K.adFree] ?: false,
        features = features,
        scaleByPrice = p[K.scaleByPrice] ?: false,
    )

    suspend fun setScaleByPrice(on: Boolean) = context.store.edit { it[K.scaleByPrice] = on }

    /** Turns one optional module on or off. The first change saves the shop type's defaults plus this change. */
    suspend fun setFeature(f: Feature, on: Boolean) {
        if (f == Feature.MOBILE_BANKING) { setMobileBanking(on); return }
        context.store.edit {
            val type = it[K.shopType]?.let { t -> runCatching { ShopType.valueOf(t) }.getOrNull() } ?: ShopType.GENERAL
            val now = it[K.features] ?: defaultFeatures(type).map { d -> d.name }.toSet()
            it[K.features] = if (on) now + f.name else now - f.name
        }
    }

    suspend fun setPrinter(address: String, name: String) = context.store.edit {
        it[K.printerAddress] = address; it[K.printerName] = name
    }

    suspend fun setAdFree(on: Boolean) = context.store.edit { it[K.adFree] = on }

    suspend fun setPaper(mm: Int) = context.store.edit { it[K.paperMm] = mm }

    suspend fun setLastBackup(at: Long) = context.store.edit { it[K.lastBackup] = at }

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
