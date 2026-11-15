package com.bongshaltech.erpinventory

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.ui.screens.ErpNavHost
import com.bongshaltech.erpinventory.ui.theme.ErpTheme
import com.bongshaltech.erpinventory.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map

// AppCompatActivity so the in-app English / Bangla switch works on Android 7 to 12 too.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeFlow = (application as ErpApp).settings.settings.map { it.theme }
        setContent {
            val mode by themeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            // Edge-to-edge ignores adjustResize, so lift every screen above the keyboard here.
            ErpTheme(mode) { Box(Modifier.fillMaxSize().imePadding()) { ErpNavHost() } }
        }
    }
}
