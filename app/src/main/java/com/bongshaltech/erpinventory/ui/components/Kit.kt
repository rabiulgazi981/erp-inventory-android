package com.bongshaltech.erpinventory.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tint
import com.bongshaltech.erpinventory.ui.theme.Ui

const val DEV_URL = "https://bongshaltech.com"
const val APP_VERSION = "v1.0"

/** Text with the design's sizes in sp and the current palette's text colour by default. */
@Composable
fun T(
    text: String,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    shadow: Boolean = false,
) {
    Text(
        text, modifier,
        style = if (shadow) LocalTextStyle.current.copy(shadow = TextShadow) else LocalTextStyle.current,
        color = if (color == Color.Unspecified) Ui.c.text else color,
        fontSize = size, fontWeight = weight, textAlign = align, maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        lineHeight = (size.value * 1.35f).sp,
    )
}

@Composable
fun Muted(text: String, size: TextUnit = 12.sp, modifier: Modifier = Modifier, align: TextAlign? = null) =
    T(text, size, color = Ui.c.muted, modifier = modifier, align = align)

/** Soft drop shadow used on the design's 14px cards. */
fun Modifier.softShadow(shape: Shape, elevation: Dp = 8.dp): Modifier =
    shadow(elevation, shape, ambientColor = Color(0x380F172A), spotColor = Color(0x380F172A))

/** White card with a faint top-to-bottom sheen, 14dp corners and a hairline border. */
@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 12.dp, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .softShadow(shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Ui.c.cardTop, Ui.c.cardBottom)))
            .border(1.dp, Ui.c.border, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(padding),
        content = content,
    )
}

/** A coloured panel: gradient title bar with the little glass square, then white content. */
@Composable
fun Panel(title: String, grad: Brush, borderColor: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.fillMaxWidth().softShadow(shape).clip(shape).background(Ui.c.card)
            .border(1.dp, if (Ui.c.dark) Ui.c.border else borderColor, shape)
    ) {
        Row(
            Modifier.fillMaxWidth().background(grad)
                .drawBehind { drawRect(Color(0x1F000000), topLeft = Offset(0f, size.height - 2.dp.toPx())) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
            T(title, 15.sp, FontWeight.Bold, Color.White, shadow = true)
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

object PanelColors {
    val Blue = Color(0xFFBFD2FD)
    val Green = Color(0xFFBBF7D0)
    val Orange = Color(0xFFFED7AA)
}

/** White top bar, 56dp, with an optional back arrow. */
@Composable
fun Header(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().softShadow(RoundedCornerShape(0.dp), 6.dp).background(Ui.c.card).statusBarsPadding()
            .height(56.dp).padding(start = if (onBack != null) 4.dp else 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onBack != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Ui.c.text, modifier = Modifier.size(18.dp))
        }
        T(title, 18.sp, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
        actions()
    }
}

@Composable
fun HeaderIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Ui.c.text, modifier = Modifier.size(22.dp))
    }
}

/** Small rounded label such as a memo number or "Owner only". */
@Composable
fun Tag(text: String, fg: Color, bg: Brush) {
    T(text, 12.sp, FontWeight.SemiBold, fg, Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp))
}

/** The design's raised gradient button (blue for primary, green for save). */
@Composable
fun GradButton(text: String, grad: Brush, modifier: Modifier = Modifier, height: Dp = 50.dp, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(height)
            .let { if (enabled) it.shadow(8.dp, shape, ambientColor = Brand.Blue, spotColor = Brand.Blue) else it }
            .clip(shape)
            .background(if (enabled) grad else SolidColor(Ui.c.soft))
            .let { if (enabled) it else it.border(1.dp, Ui.c.inputBorder, shape) }
            .drawBehind { drawRect(Color(0x26000000), topLeft = Offset(0f, size.height - 3.dp.toPx())) }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 16.sp, FontWeight.SemiBold, if (enabled) Color.White else Ui.c.muted, maxLines = 1, shadow = enabled) }
}

@Composable
fun WhiteButton(text: String, modifier: Modifier = Modifier, height: Dp = 50.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(height).clip(shape).background(Ui.c.card).border(1.dp, Ui.c.inputBorder, shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 15.sp, FontWeight.SemiBold, maxLines = 1) }
}

@Composable
fun LinkButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        T(text, 15.sp, FontWeight.SemiBold, Brand.Link)
    }
}

/** Bottom action bar on forms (white, top border). */
@Composable
fun BottomBar(content: @Composable RowScope.() -> Unit) {
    val line = Ui.c.border
    Row(
        Modifier.fillMaxWidth().background(Ui.c.card).drawBehind { drawRect(line, size = size.copy(height = 1.dp.toPx())) }
            .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** The design's input: label above, 46dp box with 12dp corners and a slight inner shadow. */
@Composable
fun Input(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    height: Dp = 46.dp,
    error: String? = null,
    password: Boolean = false,
    alignEnd: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) T(label, 13.sp, FontWeight.SemiBold, Ui.c.label)
        val shape = RoundedCornerShape(if (height < 44.dp) 10.dp else 12.dp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp, color = Ui.c.text, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            ),
            cursorBrush = SolidColor(Brand.Blue),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().height(height).clip(shape).background(Ui.c.input)
                        .drawBehind { drawRect(Color(0x140F172A), size = size.copy(height = 2.dp.toPx())) }
                        .border(1.dp, if (error != null) Brand.Red else Ui.c.inputBorder, shape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    leading?.invoke()
                    Box(Modifier.weight(1f), contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) T(placeholder, 14.sp, color = Ui.c.muted, maxLines = 1)
                        inner()
                    }
                }
            },
        )
        if (error != null) T(error, 12.sp, color = Brand.Red)
    }
}

/** Soft shadow that keeps white text readable on the pale end of a gradient. */
private val TextShadow = Shadow(Color(0x66000000), Offset(0f, 1.5f), 3f)

/** Gradient on/off switch from the design (green when on). */
@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit, width: Dp = 44.dp, height: Dp = 26.dp) {
    val knob = height - 6.dp
    Row(
        Modifier.width(width).height(height).clip(CircleShape)
            .background(if (on) Brand.GreenGrad else SolidColor(if (Ui.c.dark) Ui.c.inputBorder else Color(0xFFD1D5DB)))
            .clickable { onChange(!on) }.padding(3.dp),
        horizontalArrangement = if (on) Arrangement.End else Arrangement.Start,
    ) { Box(Modifier.size(knob).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White)) }
}

/** Round chip; the selected one is the blue gradient. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, soft: Boolean = false) {
    val shape = CircleShape
    val base = Modifier.height(36.dp).clip(shape)
    val m = when {
        selected && soft -> base.background(Brush.horizontalGradient(listOf(Ui.c.infoA, Ui.c.infoB))).border(2.dp, Brand.Blue, shape)
        selected -> base.background(Brand.BlueGrad)
        else -> base.background(Ui.c.card).border(1.dp, Ui.c.inputBorder, shape)
    }
    Box(m.clickable(onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        T(
            text, 13.sp, if (selected) FontWeight.SemiBold else FontWeight.Normal,
            when { selected && !soft -> Color.White; selected -> Ui.c.text; else -> Ui.c.label }, maxLines = 1,
        )
    }
}

/** Segmented control used for Appearance and Language. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Ui.c.soft).padding(2.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.height(30.dp).clip(RoundedCornerShape(6.dp)).background(if (on) Ui.c.card else Color.Transparent)
                    .clickable { onSelect(i) }.padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center,
            ) { T(label, 12.sp, if (on) FontWeight.SemiBold else FontWeight.Normal, if (on) Ui.c.text else Ui.c.label) }
        }
    }
}

/** Tinted rounded icon square. */
@Composable
fun TintIcon(icon: ImageVector, tint: Tint, size: Dp = 44.dp, iconSize: Dp = 22.dp, radius: Dp = 12.dp) {
    Box(
        Modifier.size(size).shadow(4.dp, RoundedCornerShape(radius), ambientColor = Color(0x4D0F172A), spotColor = Color(0x4D0F172A))
            .clip(RoundedCornerShape(radius)).background(tint.brush),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint.fg, modifier = Modifier.size(iconSize)) }
}

/** Round avatar with initials on the blue-to-mint gradient. */
@Composable
fun Initials(name: String, size: Dp = 44.dp, fontSize: TextUnit = 15.sp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Brand.Blue, Brand.Mint))), contentAlignment = Alignment.Center) {
        T(initialsOf(name), fontSize, FontWeight.Bold, Color.White)
    }
}

fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** "Developed by Bongshal Tech · ERP Inventory v1.0", opens bongshaltech.com. */
@Composable
fun DevFooter(modifier: Modifier = Modifier, light: Boolean = false) {
    val ctx = LocalContext.current
    val muted = if (light) Color(0xFFDBE6FE) else Color(0xFF6B7280)
    val link = if (light) Color.White else Brand.Link
    val by = stringResource(R.string.dev_by)
    Text(
        buildAnnotatedString {
            append("$by ")
            withStyle(SpanStyle(color = link, fontWeight = FontWeight.SemiBold)) { append("Bongshal Tech") }
            if (!light) append(" · ERP Inventory $APP_VERSION")
        },
        color = muted, fontSize = 12.sp, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEV_URL))) }.padding(8.dp),
    )
}

enum class Tab { HOME, SALE, PRODUCTS, REPORTS, MORE }

/** Five-tab bottom bar from the design. */
@Composable
fun BottomNav(current: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(
        Triple(Tab.HOME, Icons.Outlined.Home, R.string.nav_home),
        Triple(Tab.SALE, Icons.Outlined.ShoppingCart, R.string.nav_sale),
        Triple(Tab.PRODUCTS, Icons.Outlined.Inventory2, R.string.nav_products),
        Triple(Tab.REPORTS, Icons.Outlined.BarChart, R.string.nav_reports),
        Triple(Tab.MORE, Icons.Outlined.Menu, R.string.nav_more),
    )
    Row(
        Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(0.dp)).background(Ui.c.card).navigationBarsPadding().height(64.dp)
    ) {
        items.forEach { (tab, icon, label) ->
            val on = tab == current
            val color = if (on) Brand.Link else Ui.c.muted
            Column(
                Modifier.weight(1f).fillMaxWidth().height(64.dp).clickable { if (!on) onTab(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(3.dp))
                T(stringResource(label), 11.sp, if (on) FontWeight.SemiBold else FontWeight.Normal, color, maxLines = 1)
            }
        }
    }
}

/** A row inside a grouped list (More screen). */
@Composable
fun ListRow(
    title: String,
    icon: ImageVector? = null,
    tint: Tint? = null,
    sub: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    color: Color = Color.Unspecified,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val divider = Ui.c.divider
    Row(
        Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .drawBehind { if (!last) drawRect(divider, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = size.copy(height = 1.dp.toPx())) }
            .heightIn(min = 46.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null && tint != null) TintIcon(icon, tint, 30.dp, 18.dp, 8.dp)
        Column(Modifier.weight(1f)) {
            T(title, 14.sp, FontWeight.Medium, color)
            if (sub != null) Muted(sub)
        }
        trailing?.invoke()
    }
}

@Composable
fun Group(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) T(title.uppercase(), 12.sp, FontWeight.Bold, Ui.c.muted)
        val shape = RoundedCornerShape(14.dp)
        Column(Modifier.fillMaxWidth().softShadow(shape).clip(shape).background(Ui.c.card).border(1.dp, Ui.c.border, shape), content = content)
    }
}

@Composable
fun SumRow(label: String, value: String, strong: Boolean = false, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T(label, 14.sp, if (strong) FontWeight.Bold else FontWeight.Normal, if (strong) color else Ui.c.muted, Modifier.weight(1f))
        T(value, 14.sp, if (strong) FontWeight.Bold else FontWeight.Normal, color)
    }
}

/** Italic is used on the receipt preview's slogan line. */
val Italic = FontStyle.Italic
