package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.BusinessProfile
import com.bongshaltech.erpinventory.data.Session
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.menuSet
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.BottomNav
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.DevFooter
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Group
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Initials
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.ListRow
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tab
import com.bongshaltech.erpinventory.ui.components.Tag
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.components.Toggle
import com.bongshaltech.erpinventory.ui.components.initialsOf
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.ThemeMode
import com.bongshaltech.erpinventory.ui.theme.Tint
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import kotlinx.coroutines.launch

private const val WHATSAPP = "8801938273878"

@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(14.dp))

@Composable
private fun Side(text: String) = Muted(text)

@Composable
private fun RowScope.BizTile(icon: ImageVector, tint: Tint, label: String, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TintIcon(icon, tint, 40.dp, 20.dp)
        T(label, 12.sp, maxLines = 1)
    }
}

@Composable
fun MoreScreen(go: (String) -> Unit, tab: (Tab) -> Unit, signedOut: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val who = rememberWho()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val staff by remember { app.repo.staff.all() }.collectAsStateWithLifecycle(emptyList())
    val s = settings
    val appId = ctx.packageName.removeSuffix(".debug")
    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.more_title))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(Modifier.fillMaxWidth(), onClick = if (who.owner) ({ go(Routes.BIZ) }) else null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Initials(who.name.ifBlank { s?.profile?.name.orEmpty() })
                    Column(Modifier.weight(1f)) {
                        T(who.name.ifBlank { s?.profile?.name.orEmpty() }, 15.sp, FontWeight.Bold, maxLines = 1)
                        Muted(
                            if (who.owner) listOfNotNull(
                                s?.let { st -> stringResource(typeOptions.first { it.type == st.shopType }.label) },
                                s?.profile?.phone?.takeIf { it.isNotBlank() },
                            ).joinToString(" · ")
                            else stringResource(R.string.signed_in_staff)
                        )
                    }
                    if (who.owner) T(stringResource(R.string.more_business), 12.sp, FontWeight.SemiBold, Brand.Link)
                }
            }
            Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row {
                    val soon = { go(Routes.SOON) }
                    BizTile(Icons.Outlined.People, Tints.Blue, stringResource(R.string.home_customers), soon)
                    BizTile(Icons.Outlined.LocalShipping, Tints.Orange, stringResource(R.string.suppliers), soon)
                    BizTile(Icons.Outlined.Inventory2, Tints.Green, stringResource(R.string.purchases), soon)
                    BizTile(Icons.Outlined.ReceiptLong, Tints.Purple, stringResource(R.string.expenses), soon)
                }
            }

            Group(stringResource(R.string.group_app)) {
                if (who.owner) {
                    ListRow(stringResource(R.string.more_business_long), Icons.Outlined.Storefront, Tints.Blue, trailing = { Chevron() }) { go(Routes.BIZ) }
                    ListRow(stringResource(R.string.staff_manage), Icons.Outlined.Badge, Tints.Blue, trailing = { Side(stringResource(R.string.n_staff, staff.size)) }) { go(Routes.STAFF_MANAGE) }
                    ListRow(stringResource(R.string.remove_ads), Icons.Outlined.WorkspacePremium, Tints.Amber, trailing = {
                        Tag(stringResource(R.string.go_ad_free), Color(0xFF92400E), Tints.Amber.brush)
                    }) { go(Routes.SOON) }
                    ListRow(stringResource(R.string.sms_customers), Icons.Outlined.ChatBubbleOutline, Tints.Orange, trailing = {
                        Toggle(s?.smsDefaultOn == true, { on -> scope.launch { app.settings.setSmsDefault(on) } })
                    }, sub = stringResource(R.string.sms_default_note))
                    ListRow(stringResource(R.string.backup), Icons.Outlined.CloudUpload, Tints.Blue, trailing = { Chevron() }) { go(Routes.SOON) }
                }
                ListRow(stringResource(R.string.printer), Icons.Outlined.Print, Tints.Grey, trailing = { Chevron() }) { go(Routes.SOON) }
                ListRow(stringResource(R.string.appearance), Icons.Outlined.Contrast, Tints.Indigo, trailing = {
                    val mode = s?.theme ?: ThemeMode.SYSTEM
                    Segmented(
                        listOf(stringResource(R.string.light), stringResource(R.string.dark), stringResource(R.string.system)),
                        when (mode) { ThemeMode.LIGHT -> 0; ThemeMode.DARK -> 1; ThemeMode.SYSTEM -> 2 },
                    ) { i -> scope.launch { app.settings.setTheme(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[i]) } }
                })
                ListRow(stringResource(R.string.language), Icons.Outlined.Translate, Tints.Teal, last = true, trailing = {
                    Segmented(listOf(stringResource(R.string.device), "English", "বাংলা"), currentLangIndex()) { setLang(it) }
                })
            }

            if (who.owner) Group(stringResource(R.string.group_business)) {
                ListRow(
                    stringResource(R.string.mfs_toggle), Icons.Outlined.AccountBalanceWallet, Tints.Pink,
                    sub = stringResource(R.string.mfs_toggle_sub), last = s?.mobileBankingOn != true,
                    trailing = { Toggle(s?.mobileBankingOn == true, { on -> scope.launch { app.settings.setMobileBanking(on) } }) },
                )
                if (s?.mobileBankingOn == true) {
                    ListRow(stringResource(R.string.mfs_open), trailing = { Chevron() }) { go(Routes.SOON) }
                    ListRow(stringResource(R.string.mfs_rates), last = true, trailing = { Chevron() }) { go(Routes.SOON) }
                }
            }

            Group(stringResource(R.string.group_support)) {
                ListRow(stringResource(R.string.more_rate), Icons.Outlined.Star, Tints.Amber, sub = stringResource(R.string.rate_sub), trailing = {
                    Box(
                        Modifier.height(34.dp).clip(RoundedCornerShape(9.dp)).background(Brand.OrangeGrad)
                            .clickable {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))) }
                                    .onFailure { open("https://play.google.com/store/apps/details?id=$appId") }
                            }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { T(stringResource(R.string.rate_now), 13.sp, FontWeight.SemiBold, Color.White, shadow = true) }
                })
                ListRow(stringResource(R.string.more_share), trailing = { Icon(Icons.Outlined.Share, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp)) }) {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "ERP Inventory: https://play.google.com/store/apps/details?id=$appId")
                    ctx.startActivity(Intent.createChooser(send, null))
                }
                ListRow(stringResource(R.string.more_help), last = true, trailing = { Side("01938273878") }) { open("https://wa.me/$WHATSAPP") }
            }

            Group(stringResource(R.string.group_account)) {
                ListRow(stringResource(R.string.more_sign_out), last = true) {
                    scope.launch { app.settings.setSession(Session.SignedOut); signedOut() }
                }
            }
            DevFooter()
        }
        BottomNav(Tab.MORE, tab)
    }
}

@Composable
fun BizProfileScreen(onBack: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var p by remember { mutableStateOf<BusinessProfile?>(null) }
    LaunchedEffect(Unit) { p = app.settings.current().profile }
    val soon = stringResource(R.string.coming_soon)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.biz_title), onBack)
        val b = p
        if (b == null) { Box(Modifier.weight(1f)); return@Column }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Panel(stringResource(R.string.biz_logo_name), Brand.BlueGrad, PanelColors.Blue) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(76.dp).shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Brand.Blue, spotColor = Brand.Blue)
                            .clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Brand.Blue, Brand.Mint))),
                        contentAlignment = Alignment.Center,
                    ) { T(initialsOf(b.name), 24.sp, FontWeight.Bold, Color.White) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        T(stringResource(R.string.biz_logo), 14.sp, FontWeight.SemiBold)
                        Muted(stringResource(R.string.biz_logo_note))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(R.string.camera, R.string.gallery).forEach { l ->
                                Box(
                                    Modifier.height(30.dp).clip(RoundedCornerShape(8.dp)).background(Ui.c.card)
                                        .border(1.dp, Ui.c.inputBorder, RoundedCornerShape(8.dp))
                                        .clickable { Toast.makeText(ctx, soon, Toast.LENGTH_SHORT).show() }.padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) { T(stringResource(l), 12.sp, FontWeight.SemiBold) }
                            }
                        }
                    }
                }
                Input(b.name, { p = b.copy(name = it) }, label = stringResource(R.string.onb_shop_name), height = 42.dp)
                Input(b.slogan, { p = b.copy(slogan = it) }, label = stringResource(R.string.biz_slogan), height = 42.dp)
            }
            Panel(stringResource(R.string.biz_contact), Brand.GreenGrad, PanelColors.Green) {
                Input(b.address, { p = b.copy(address = it) }, label = stringResource(R.string.biz_address), height = 42.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(b.phone, { p = b.copy(phone = it) }, Modifier.weight(1f), label = stringResource(R.string.mobile), keyboard = KeyboardType.Phone, height = 42.dp)
                    Input(b.email, { p = b.copy(email = it) }, Modifier.weight(1f), label = stringResource(R.string.biz_email), keyboard = KeyboardType.Email, height = 42.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(b.web, { p = b.copy(web = it) }, Modifier.weight(1f), label = stringResource(R.string.biz_web), height = 42.dp)
                    Input(b.bin, { p = b.copy(bin = it) }, Modifier.weight(1f), label = stringResource(R.string.biz_bin), height = 42.dp)
                }
            }
            Panel(stringResource(R.string.biz_receipt), Brand.OrangeGrad, PanelColors.Orange) {
                Input(b.thanks, { p = b.copy(thanks = it) }, label = stringResource(R.string.biz_thanks), placeholder = "Thank you! Come again", height = 42.dp)
                Input(b.returnPolicy, { p = b.copy(returnPolicy = it) }, label = stringResource(R.string.biz_return), height = 42.dp)
                T(stringResource(R.string.live_preview), 12.sp, FontWeight.SemiBold, Ui.c.label, Modifier.padding(top = 4.dp))
                ReceiptPreview(b)
                Muted(stringResource(R.string.biz_pdf_note), align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        BottomBar {
            GradButton(stringResource(R.string.biz_save), Brand.GreenGrad, Modifier.weight(1f)) {
                scope.launch { app.settings.saveProfile(b); onBack() }
            }
        }
    }
}

/** Small paper-receipt mock that updates as the owner types. */
@Composable
private fun ReceiptPreview(b: BusinessProfile) {
    val mono = FontFamily.Monospace
    @Composable
    fun line(text: String, size: Int = 11, bold: Boolean = false, color: Color = Color(0xFF111827)) = androidx.compose.material3.Text(
        text, fontFamily = mono, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    @Composable
    fun dash() = Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Color(0xFF9CA3AF)))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(230.dp).shadow(12.dp).background(Color.White).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(Modifier.size(40.dp).border(2.dp, Color(0xFF111827), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                line(initialsOf(b.name), 11, true)
            }
            line(b.name.uppercase().ifBlank { "SHOP NAME" }, 13, true)
            if (b.slogan.isNotBlank()) line(b.slogan)
            line(listOf(b.address, b.phone).filter { it.isNotBlank() }.joinToString(" · "))
            dash()
            Row(Modifier.fillMaxWidth()) { line("Item x2", 11); }
            Row(Modifier.fillMaxWidth()) { line("TOTAL  1900", 11, true) }
            dash()
            if (b.thanks.isNotBlank()) line(b.thanks)
            if (b.returnPolicy.isNotBlank()) line(b.returnPolicy, 10, color = Color(0xFF4B5563))
            line("Software: ERP Inventory by Bongshal Tech", 10, color = Color(0xFF4B5563))
            line("bongshaltech.com", 10, color = Color(0xFF4B5563))
        }
    }
}

private data class Perm(val menu: StaffMenu, val label: Int)

private val perms = listOf(
    Perm(StaffMenu.SALE, R.string.perm_sale),
    Perm(StaffMenu.CUSTOMERS, R.string.perm_customers),
    Perm(StaffMenu.PRODUCTS, R.string.perm_products),
    Perm(StaffMenu.COST_PROFIT, R.string.perm_cost),
    Perm(StaffMenu.PURCHASE, R.string.perm_purchase),
    Perm(StaffMenu.EXPENSE, R.string.perm_expense),
    Perm(StaffMenu.REPORTS, R.string.perm_reports),
    Perm(StaffMenu.DISCOUNT, R.string.perm_discount),
)

@Composable
fun StaffManageScreen(onBack: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val staff by remember { app.repo.staff.all() }.collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var menus by remember { mutableStateOf(setOf(StaffMenu.SALE, StaffMenu.CUSTOMERS, StaffMenu.DISCOUNT)) }
    val savedMsg = stringResource(R.string.staff_saved)
    val labels = perms.associate { it.menu to stringResource(it.label) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.staff_title), onBack) {
            Tag(stringResource(R.string.owner_only), Color(0xFF1E40AF), Brush.linearGradient(listOf(Color(0xFFDBE6FE), Color(0xFFDBE6FE))))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (staff.isNotEmpty()) Card(Modifier.fillMaxWidth(), padding = 0.dp) {
                staff.forEachIndexed { i, s ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Initials(s.name, 38.dp, 13.sp)
                        Column(Modifier.weight(1f)) {
                            T(s.name, 14.sp, FontWeight.SemiBold)
                            Muted(listOf(s.role.ifBlank { null }, s.menuSet().mapNotNull { labels[it] }.take(2).joinToString(", ").ifBlank { null })
                                .filterNotNull().joinToString(" · "))
                        }
                        Tag(stringResource(R.string.active), Brand.DarkGreen, Tints.Green.brush)
                    }
                    if (i < staff.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.c.divider))
                }
            }
            Panel(stringResource(R.string.staff_add), Brand.BlueGrad, PanelColors.Blue) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(name, { name = it }, Modifier.weight(1f), label = stringResource(R.string.staff_name), height = 42.dp)
                    Input(pin, { pin = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f), label = stringResource(R.string.staff_pin_label),
                        keyboard = KeyboardType.NumberPassword, password = true, height = 42.dp)
                }
                Input(role, { role = it }, label = stringResource(R.string.staff_role), placeholder = stringResource(R.string.staff_role_hint), height = 42.dp)
                T(stringResource(R.string.staff_can_use), 12.sp, FontWeight.SemiBold, Ui.c.label, Modifier.padding(top = 4.dp))
                perms.forEach { p ->
                    val on = p.menu in menus
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clickable { menus = if (on) menus - p.menu else menus + p.menu }.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        T(stringResource(p.label), 14.sp, modifier = Modifier.weight(1f))
                        Toggle(on, { menus = if (it) menus + p.menu else menus - p.menu })
                    }
                }
                Muted(stringResource(R.string.staff_never))
            }
        }
        BottomBar {
            GradButton(stringResource(R.string.staff_save), Brand.GreenGrad, Modifier.weight(1f), enabled = name.isNotBlank() && pin.length == 4) {
                scope.launch {
                    app.repo.addStaff(name, role, pin, menus)
                    name = ""; pin = ""; role = ""; menus = setOf(StaffMenu.SALE, StaffMenu.CUSTOMERS, StaffMenu.DISCOUNT)
                    Toast.makeText(ctx, savedMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun SoonScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header("", onBack)
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(Icons.Outlined.PhoneAndroid, Tints.Blue, 64.dp, 30.dp, 18.dp)
            T(stringResource(R.string.coming_soon), 17.sp, FontWeight.Bold, align = TextAlign.Center)
            Muted(stringResource(R.string.coming_soon_sub), 14.sp, align = TextAlign.Center)
        }
    }
}

