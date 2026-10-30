package com.bongshaltech.erpinventory.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.ErpApp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.Session
import com.bongshaltech.erpinventory.data.ShopType
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.DevFooter
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Initials
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.LinkButton
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tint
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun app(): ErpApp = LocalContext.current.applicationContext as ErpApp

/** Current UI language: 0 = device, 1 = English, 2 = Bangla. */
fun currentLangIndex(): Int = when (AppCompatDelegate.getApplicationLocales().toLanguageTags()) {
    "" -> 0
    "en" -> 1
    else -> 2
}

fun setLang(index: Int) = AppCompatDelegate.setApplicationLocales(
    when (index) {
        1 -> LocaleListCompat.forLanguageTags("en")
        2 -> LocaleListCompat.forLanguageTags("bn")
        else -> LocaleListCompat.getEmptyLocaleList()
    }
)

/** Slogan box under the logo: dark-blue gradient, orange border and glow, English over Bangla. */
@Composable
fun SloganBox() {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.widthIn(max = 220.dp)
            .shadow(10.dp, shape, ambientColor = Color(0xFFF39A2B), spotColor = Color(0xFFF39A2B))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2563D9), Color(0xFF0F2F7A))))
            .border(1.dp, Color(0xFFF39A2B), shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Text(
            "Your whole shop in one app",
            style = TextStyle(
                brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFDFE6F1), Color(0xFFB9C4D6))),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                shadow = Shadow(Color(0x59000000), androidx.compose.ui.geometry.Offset(0f, 2f), 2f),
            ),
            maxLines = 1,
        )
        androidx.compose.material3.Text(
            "দোকানের সব হিসাব, এক অ্যাপে",
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(Color(0xFF6FDC4A), Color(0xFFF4D04C), Color(0xFFF39A2B))),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun SplashScreen(go: (String) -> Unit) {
    val app = app()
    LaunchedEffect(Unit) {
        app.settings.ensureInstalledAt()
        delay(1100)
        val s = app.settings.current()
        go(
            when {
                s.session == Session.SignedOut -> Routes.LOGIN
                !s.onboarded -> Routes.ONBOARDING
                else -> Routes.HOME
            }
        )
    }
    Box(Modifier.fillMaxSize().background(Brand.SplashGrad)) {
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Image(
                painterResource(R.drawable.logo), "ERP Inventory",
                Modifier.size(width = 259.dp, height = 153.dp),
            )
            SloganBox()
            Spacer(Modifier.height(28.dp))
            Box(Modifier.width(120.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.width(48.dp).height(4.dp).clip(CircleShape).background(Color.White))
            }
        }
        DevFooter(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp), light = true)
    }
}

@Composable
private fun Feature(icon: ImageVector, tint: Tint, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TintIcon(icon, tint, 36.dp, 18.dp, 10.dp)
        T(text, 14.sp)
    }
}

@Composable
fun LoginScreen(go: (String) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var googleNote by remember { mutableStateOf(false) }
    val lang = currentLangIndex()
    fun openShop() = scope.launch {
        app.settings.setSession(Session.Owner)
        go(if (app.settings.current().onboarded) Routes.HOME else Routes.ONBOARDING)
    }

    Column(
        Modifier.fillMaxSize().background(Ui.c.bg).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Segmented(listOf("EN", "বাংলা"), if (lang == 2) 1 else 0) { setLang(if (it == 1) 2 else 1) }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.logo), "ERP Inventory", Modifier.width(250.dp))
            T(stringResource(R.string.login_title), 26.sp, FontWeight.Bold, align = TextAlign.Center)
            T(stringResource(R.string.login_sub), 15.sp, color = Ui.c.muted, align = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
        }
        Card(Modifier.fillMaxWidth(), padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Feature(Icons.Outlined.WifiOff, Tints.Blue, stringResource(R.string.login_f1))
                Feature(Icons.Outlined.CloudQueue, Tints.Green, stringResource(R.string.login_f2))
                Feature(Icons.Outlined.Print, Tints.Orange, stringResource(R.string.login_f3))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().height(54.dp).shadow(8.dp, shape).clip(shape).background(Color.White)
                    .border(1.dp, Color(0xFFD1D5DB), shape).clickable { googleNote = true },
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ic_google), null, Modifier.size(22.dp))
                T(stringResource(R.string.login_google), 16.sp, FontWeight.SemiBold, Color(0xFF1F1F1F))
            }
            if (googleNote) Muted(stringResource(R.string.login_google_soon), align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            LinkButton(stringResource(R.string.login_without)) { openShop() }
            val sShape = RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().height(46.dp).clip(sShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFEEF3FF), Color(0xFFE8F6EF))))
                    .border(1.dp, Color(0xFFBFD2FD), sShape).clickable { go(Routes.STAFF_LOGIN) },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Person, null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(18.dp))
                T(stringResource(R.string.login_staff), 15.sp, FontWeight.SemiBold, Color(0xFF1E3A8A))
            }
            Muted(stringResource(R.string.login_note), align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            DevFooter()
        }
    }
}

private data class TypeOption(val type: ShopType, val label: Int, val note: Int)

private val typeOptions = listOf(
    TypeOption(ShopType.GENERAL, R.string.type_general, R.string.type_note_general),
    TypeOption(ShopType.GROCERY, R.string.type_grocery, R.string.type_note_grocery),
    TypeOption(ShopType.SUPER_SHOP, R.string.type_super, R.string.type_note_super),
    TypeOption(ShopType.PHARMACY, R.string.type_pharmacy, R.string.type_note_pharmacy),
    TypeOption(ShopType.RESTAURANT, R.string.type_restaurant, R.string.type_note_restaurant),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(go: (String) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(ShopType.GENERAL) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val required = stringResource(R.string.required)
    val selected = typeOptions.first { it.type == type }

    Column(
        Modifier.fillMaxSize().background(Ui.c.bg).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(R.drawable.logo), "ERP Inventory", Modifier.width(120.dp))
            T(stringResource(R.string.onb_title), 22.sp, FontWeight.Bold)
            T(stringResource(R.string.onb_sub), 14.sp, color = Ui.c.muted, align = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                Box(Modifier.size(28.dp, 6.dp).clip(CircleShape).background(Brand.BlueGrad))
                Box(Modifier.size(28.dp, 6.dp).clip(CircleShape).background(Color(0xFFD1D5DB)))
            }
        }
        Card(Modifier.fillMaxWidth(), padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Input(name, { name = it }, label = stringResource(R.string.onb_shop_name), error = required.takeIf { tried && name.isBlank() })
                Input(phone, { phone = it }, label = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone)
                Input(address, { address = it }, label = stringResource(R.string.onb_address))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    T(stringResource(R.string.onb_type), 13.sp, FontWeight.SemiBold, Ui.c.label)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        typeOptions.forEach { o -> Chip(stringResource(o.label), o.type == type, { type = o.type }, soft = true) }
                    }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFDBE6FE), Color(0xFFD6F0E3))))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        T(stringResource(R.string.onb_turns_on, stringResource(selected.label)), 13.sp, FontWeight.Bold, Color(0xFF1E3A8A))
                        T(stringResource(selected.note), 12.sp, color = Color(0xFF1F2937))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Input(
                        code, { code = it.filter(Char::isLetterOrDigit).uppercase().take(4) }, label = stringResource(R.string.onb_code),
                        placeholder = "DMS", error = required.takeIf { tried && code.length < 2 },
                    )
                    Muted(stringResource(R.string.onb_code_hint, (code.ifEmpty { "DMS" }) + "-26-0001"))
                }
            }
        }
        GradButton(stringResource(R.string.onb_continue), Brand.BlueGrad, Modifier.fillMaxWidth(), height = 52.dp) {
            tried = true
            if (name.isNotBlank() && code.length >= 2) scope.launch {
                app.settings.finishOnboarding(type, name.trim(), phone.trim(), address.trim(), code)
                go(Routes.HOME)
            }
        }
    }
}

@Composable
fun StaffLoginScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val staff by remember { app.repo.staff.all() }.collectAsStateWithLifecycle(emptyList())
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    var picked by remember { mutableStateOf<Long?>(null) }
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val sel = staff.firstOrNull { it.id == picked } ?: staff.firstOrNull()

    fun press(k: String) {
        val who = sel ?: return
        wrong = false
        if (k == "del") { pin = pin.dropLast(1); return }
        val p = pin + k
        if (p.length < 4) { pin = p; return }
        pin = ""
        scope.launch {
            if (app.repo.checkStaffPin(who.id, p)) {
                app.settings.setSession(Session.Staff(who.id))
                go(Routes.HOME)
            } else wrong = true
        }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.staff_login_title), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                T(settings?.profile?.name.orEmpty(), 16.sp, FontWeight.Bold)
                Muted(stringResource(R.string.staff_pick), 13.sp)
            }
            if (staff.isEmpty()) {
                Muted(stringResource(R.string.staff_none), 14.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                staff.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { s ->
                            val on = s.id == sel?.id
                            val shape = RoundedCornerShape(14.dp)
                            Column(
                                Modifier.weight(1f).shadow(6.dp, shape).clip(shape)
                                    .background(if (on) Brush.verticalGradient(listOf(Color(0xFFEEF3FF), Ui.c.card)) else Brush.verticalGradient(listOf(Ui.c.card, Ui.c.card)))
                                    .border(if (on) 2.dp else 1.dp, if (on) Brand.Blue else Ui.c.border, shape)
                                    .clickable { picked = s.id; pin = ""; wrong = false }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Initials(s.name)
                                T(s.name, 13.sp, FontWeight.SemiBold, maxLines = 1)
                                Muted(s.role.ifBlank { " " }, 11.sp)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    T(stringResource(R.string.staff_enter_pin, sel?.name.orEmpty()), 14.sp, FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier.size(16.dp).clip(CircleShape)
                                    .background(if (i < pin.length) Brand.BlueGrad else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                    .border(2.dp, Brand.Blue, CircleShape)
                            )
                        }
                    }
                    T(if (wrong) stringResource(R.string.staff_wrong_pin) else " ", 12.sp, color = Brand.Red)
                }
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "del")).forEach { keys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            keys.forEach { k ->
                                val shape = RoundedCornerShape(16.dp)
                                if (k.isEmpty()) Spacer(Modifier.weight(1f).height(58.dp)) else Box(
                                    Modifier.weight(1f).height(58.dp).shadow(4.dp, shape).clip(shape)
                                        .background(if (k == "del") Ui.c.soft else Ui.c.card).border(1.dp, Ui.c.border, shape)
                                        .clickable { press(k) },
                                    contentAlignment = Alignment.Center,
                                ) { T(if (k == "del") "⌫" else k, 22.sp, FontWeight.SemiBold) }
                            }
                        }
                    }
                }
                Muted(stringResource(R.string.staff_forgot), align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            LinkButton(stringResource(R.string.staff_owner_link)) { onBack() }
        }
    }
}
