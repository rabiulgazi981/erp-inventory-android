package com.bongshaltech.erpinventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
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

/** Brand colours from the design canvas. Gradients run left to right like the design's 90deg ones. */
object Brand {
    val Blue = Color(0xFF2F55D4)
    val Sky = Color(0xFF4AA3E3)
    val Green = Color(0xFF2F8F4E)
    val Lime = Color(0xFF9BCF3F)
    val Orange = Color(0xFFE05A2A)
    val Yellow = Color(0xFFF4D04C)
    val Mint = Color(0xFF5FB88A)
    val Link = Color(0xFF1D4ED8)
    val Red = Color(0xFFB91C1C)
    val Amber = Color(0xFFB45309)
    val DarkGreen = Color(0xFF15803D)

    val BlueGrad = Brush.horizontalGradient(listOf(Blue, Sky))
    val GreenGrad = Brush.horizontalGradient(listOf(Green, Lime))
    val OrangeGrad = Brush.horizontalGradient(listOf(Orange, Yellow))
    val TotalGrad = Brush.linearGradient(listOf(Blue, Mint), start = Offset.Zero, end = Offset(900f, 500f))
    val SplashGrad = Brush.linearGradient(listOf(Blue, Sky, Mint), start = Offset(0f, 0f), end = Offset(700f, 2000f))
}

/** Soft tinted icon backgrounds (light gradient + strong foreground), as used on Home and More. */
@Immutable
data class Tint(val from: Color, val to: Color, val fg: Color) {
    val brush get() = Brush.linearGradient(listOf(from, to))
}

object Tints {
    val Blue = Tint(Color(0xFFDBE6FE), Color(0xFFCDEEFB), Color(0xFF1D4ED8))
    val Green = Tint(Color(0xFFDCFCE7), Color(0xFFECFCCB), Color(0xFF15803D))
    val Orange = Tint(Color(0xFFFFEDD5), Color(0xFFFEF3C7), Color(0xFFC2410C))
    val Purple = Tint(Color(0xFFF3E8FF), Color(0xFFFCE7F3), Color(0xFF7E22CE))
    val Amber = Tint(Color(0xFFFEF3C7), Color(0xFFFFEDD5), Color(0xFFB45309))
    val Teal = Tint(Color(0xFFCCFBF1), Color(0xFFDCFCE7), Color(0xFF0F766E))
    val Indigo = Tint(Color(0xFFE0E7FF), Color(0xFFDBE6FE), Color(0xFF4338CA))
    val Pink = Tint(Color(0xFFFCE7F3), Color(0xFFFFEDD5), Color(0xFFBE185D))
    val Grey = Tint(Color(0xFFF3F4F6), Color(0xFFF3F4F6), Color(0xFF374151))
}

/** Surface colours that change between light and dark. */
@Immutable
data class Palette(
    val bg: Color,
    val card: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val text: Color,
    val muted: Color,
    val label: Color,
    val border: Color,
    val inputBorder: Color,
    val input: Color,
    val divider: Color,
    val soft: Color,
    val dark: Boolean,
    // Pale blue-to-green "info" surfaces (selected chip, notes, staff button) and the text on them.
    val infoA: Color,
    val infoB: Color,
    val infoBorder: Color,
    val infoText: Color,
)

val LightPalette = Palette(
    bg = Color(0xFFF4F6FA), card = Color.White, cardTop = Color.White, cardBottom = Color(0xFFF4F7FC),
    text = Color(0xFF111827), muted = Color(0xFF5B6472), label = Color(0xFF374151),
    border = Color(0xFFE5E7EB), inputBorder = Color(0xFFD1D5DB), input = Color.White,
    divider = Color(0xFFF3F4F6), soft = Color(0xFFF3F4F6), dark = false,
    infoA = Color(0xFFDBE6FE), infoB = Color(0xFFD6F0E3), infoBorder = Color(0xFFBFD2FD), infoText = Color(0xFF1E3A8A),
)

val DarkPalette = Palette(
    bg = Color(0xFF0B1220), card = Color(0xFF141C2E), cardTop = Color(0xFF172036), cardBottom = Color(0xFF121A2B),
    text = Color(0xFFF1F5F9), muted = Color(0xFF94A3B8), label = Color(0xFFCBD5E1),
    border = Color(0xFF243049), inputBorder = Color(0xFF334155), input = Color(0xFF0F172A),
    divider = Color(0xFF1E293B), soft = Color(0xFF1E293B), dark = true,
    infoA = Color(0xFF1C2B4F), infoB = Color(0xFF15302A), infoBorder = Color(0xFF2C4373), infoText = Color(0xFFA5C0FF),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

object Ui {
    val c: Palette @Composable get() = LocalPalette.current
}

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter_tight, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val InterTight = FontFamily(inter(400), inter(500), inter(600), inter(700))
private val Hind = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal),
    Font(R.font.hind_siliguri_regular, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold),
)

private fun typography(f: FontFamily) = Typography(
    bodyLarge = TextStyle(fontFamily = f, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = f, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = f, fontSize = 12.sp),
    titleLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    labelLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
)

/** Appearance chosen in More: follow the phone, or force light / dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun ErpTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Hind Siliguri carries both Bangla and Latin, so Bangla screens use it throughout.
    val bangla = LocalConfiguration.current.locales[0].language == "bn"
    val family = if (bangla) Hind else InterTight
    val p = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(primary = Brand.Sky, background = p.bg, surface = p.card)
            else lightColorScheme(primary = Brand.Blue, background = p.bg, surface = p.card),
            typography = typography(family),
            content = content,
        )
    }
}
