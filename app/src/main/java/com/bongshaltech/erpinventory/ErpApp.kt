package com.bongshaltech.erpinventory

import android.app.Application
import com.bongshaltech.erpinventory.ads.AdFreeBilling
import com.bongshaltech.erpinventory.data.SettingsRepo
import com.bongshaltech.erpinventory.data.ShopRepo
import com.bongshaltech.erpinventory.data.db.AppDatabase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ErpApp : Application() {
    val settings by lazy { SettingsRepo(this) }
    val db by lazy { AppDatabase.open(this) }
    val repo by lazy { ShopRepo(db, settings) }
    val adFree by lazy { AdFreeBilling(this) }

    /** Last known ad-free state, kept in memory so ads can be skipped without a suspend call. */
    @Volatile var adFreeCached = false
    @Volatile var installedAtCached = 0L

    override fun onCreate() {
        super.onCreate()
        adFree.refresh()
        MainScope().launch { settings.settings.collect { adFreeCached = it.adFree; installedAtCached = it.installedAt } }
    }
}
