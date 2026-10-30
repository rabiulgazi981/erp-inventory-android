package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.ErpApp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.Session
import com.bongshaltech.erpinventory.data.ShopType
import com.bongshaltech.erpinventory.ui.components.DevFooter
import com.bongshaltech.erpinventory.ui.components.Field
import com.bongshaltech.erpinventory.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun app(): ErpApp = LocalContext.current.applicationContext as ErpApp

@Composable
fun LogoWithSlogan() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.logo), contentDescription = "ERP Inventory", modifier = Modifier.size(width = 259.dp, height = 153.dp))
        Spacer(Modifier.height(14.dp))
        // Dark-blue pill with an orange glow, never wider than the logo.
        Text(
            stringResource(R.string.slogan),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 220.dp)
                .shadow(10.dp, CircleShape, ambientColor = Brand.Orange, spotColor = Brand.Orange)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF173A9E), Brand.Blue)))
                .border(1.dp, Brand.Orange.copy(alpha = 0.7f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/** Shown briefly after the system splash, then routes to the right place. */
@Composable
fun SplashScreen(go: (String) -> Unit) {
    val app = app()
    LaunchedEffect(Unit) {
        app.settings.ensureInstalledAt()
        delay(900)
        val s = app.settings.current()
        go(
            when {
                s.session == Session.SignedOut -> Routes.LOGIN
                !s.onboarded -> Routes.ONBOARDING
                else -> Routes.HOME
            }
        )
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.align(Alignment.Center)) { LogoWithSlogan() }
        DevFooter(Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

@Composable
fun LoginScreen(go: (String) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var googleNote by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        LogoWithSlogan()
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.login_sub), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        // Google sign-in needs the Firebase project, which is set up when the app goes on Play.
        OutlinedButton(onClick = { googleNote = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.login_google))
        }
        if (googleNote) Text(stringResource(R.string.login_google_soon), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                scope.launch {
                    app.settings.setSession(Session.Owner)
                    go(if (app.settings.current().onboarded) Routes.HOME else Routes.ONBOARDING)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.login_owner)) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { go(Routes.STAFF_LOGIN) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.login_staff))
        }
        Spacer(Modifier.weight(1f))
        DevFooter()
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

@Composable
fun OnboardingScreen(go: (String) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(ShopType.GENERAL) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val required = stringResource(R.string.required)

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.onb_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onb_type), style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.height(250.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(typeOptions) { o ->
                val selected = o.type == type
                Text(
                    stringResource(o.label),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Brand.Blue else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (selected) Brand.Blue else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { type = o.type }
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                )
            }
        }
        Text(stringResource(typeOptions.first { it.type == type }.note), style = MaterialTheme.typography.bodySmall)
        Field(name, { name = it }, stringResource(R.string.onb_shop_name), error = required.takeIf { tried && name.isBlank() })
        Field(phone, { phone = it }, stringResource(R.string.onb_phone), phone = true)
        Field(
            code, { code = it.filter(Char::isLetterOrDigit).uppercase().take(4) }, stringResource(R.string.onb_code),
            error = required.takeIf { tried && code.length < 2 },
        )
        Text(stringResource(R.string.onb_code_hint), style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = {
                tried = true
                if (name.isNotBlank() && code.length >= 2) scope.launch {
                    app.settings.finishOnboarding(type, name.trim(), phone.trim(), code)
                    go(Routes.HOME)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.onb_start)) }
    }
}

@Composable
fun StaffLoginScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val staff by remember { app.repo.staff.all() }.collectAsStateWithLifecycle(emptyList())
    var picked by remember { mutableStateOf<Long?>(null) }
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
            Text(stringResource(R.string.staff_login_title), style = MaterialTheme.typography.headlineSmall)
        }
        if (staff.isEmpty()) {
            Text(stringResource(R.string.staff_none))
            return@Column
        }
        Text(stringResource(R.string.staff_pick), style = MaterialTheme.typography.titleMedium)
        staff.forEach { s ->
            val sel = picked == s.id
            Text(
                s.name,
                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (sel) Brand.Blue else MaterialTheme.colorScheme.surface)
                    .clickable { picked = s.id; pin = ""; wrong = false }.padding(14.dp),
            )
        }
        if (picked != null) {
            Field(pin, { v -> pin = v.filter(Char::isDigit).take(4); wrong = false }, stringResource(R.string.staff_pin), number = true,
                error = stringResource(R.string.staff_wrong_pin).takeIf { wrong })
            Button(
                enabled = pin.length == 4,
                onClick = {
                    val id = picked ?: return@Button
                    scope.launch {
                        if (app.repo.checkStaffPin(id, pin)) {
                            app.settings.setSession(Session.Staff(id))
                            go(Routes.HOME)
                        } else wrong = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.login_staff)) }
        }
    }
}
