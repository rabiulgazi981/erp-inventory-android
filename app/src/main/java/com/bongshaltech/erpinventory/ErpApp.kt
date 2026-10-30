package com.bongshaltech.erpinventory

import android.app.Application
import com.bongshaltech.erpinventory.data.SettingsRepo
import com.bongshaltech.erpinventory.data.ShopRepo
import com.bongshaltech.erpinventory.data.db.AppDatabase

class ErpApp : Application() {
    val settings by lazy { SettingsRepo(this) }
    val db by lazy { AppDatabase.open(this) }
    val repo by lazy { ShopRepo(db, settings) }
}
