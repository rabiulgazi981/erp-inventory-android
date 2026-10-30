package com.bongshaltech.erpinventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.R

object Brand {
    val Blue = Color(0xFF2F55D4)
    val Sky = Color(0xFF4AA3E3)
    val Green = Color(0xFF2F8F4E)
    val Lime = Color(0xFF9BCF3F)
    val Orange = Color(0xFFE05A2A)
    val Yellow = Color(0xFFF4D04C)
    val Mint = Color(0xFF5FB88A)
    val Navy = Color(0xFF14204A)
    val Bg = Color(0xFFF4F7FD)
    val Danger = Color(0xFFC0392B)

    val SalesGradient = Brush.linearGradient(listOf(Blue, Sky))
    val StockGradient = Brush.linearGradient(listOf(Green, Lime))
    val DueGradient = Brush.linearGradient(listOf(Orange, Yellow))
    val HeroGradient = Brush.linearGradient(listOf(Blue, Mint))
}

private val Light = lightColorScheme(
    primary = Brand.Blue, onPrimary = Color.White,
    secondary = Brand.Green, tertiary = Brand.Orange,
    background = Brand.Bg, surface = Color.White, onSurface = Brand.Navy, onBackground = Brand.Navy,
    surfaceVariant = Color(0xFFE8EDF8), outline = Color(0xFFCBD4E6), error = Brand.Danger,
)
private val Dark = darkColorScheme(
    primary = Brand.Sky, secondary = Brand.Lime, tertiary = Brand.Yellow,
    background = Color(0xFF0E1530), surface = Color(0xFF172042), error = Color(0xFFFF6B5E),
)

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter_tight, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val InterTight = FontFamily(inter(400), inter(500), inter(600), inter(700), inter(800))
private val Hind = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold),
)

private fun typography(f: FontFamily) = Typography(
    headlineMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = f, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = f, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = f, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun ErpTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Hind Siliguri carries both Bangla and Latin, so Bangla screens use it throughout.
    val bangla = LocalConfiguration.current.locales[0].language == "bn"
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = typography(if (bangla) Hind else InterTight),
        content = content,
    )
}
