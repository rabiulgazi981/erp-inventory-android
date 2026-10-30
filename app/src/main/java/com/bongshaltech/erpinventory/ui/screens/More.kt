package com.bongshaltech.erpinventory.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.BusinessProfile
import com.bongshaltech.erpinventory.data.Session
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.ui.components.DevFooter
import com.bongshaltech.erpinventory.ui.components.ErpTopBar
import com.bongshaltech.erpinventory.ui.components.Field
import com.bongshaltech.erpinventory.ui.theme.Brand
import kotlinx.coroutines.launch

private const val WHATSAPP = "8801938273878"

@Composable
private fun MoreRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
    )
}

@Composable
fun MoreScreen(onBack: () -> Unit, go: (String) -> Unit, signedOut: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val who = rememberWho()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val owner = who.allowed == null
    val bangla = ctx.resources.configuration.locales[0].language == "bn"

    Scaffold(topBar = { ErpTopBar(stringResource(R.string.more_title), onBack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (owner) {
                MoreRow(stringResource(R.string.more_business)) { go(Routes.BIZ) }
                MoreRow(stringResource(R.string.staff_manage)) { go(Routes.STAFF_MANAGE) }
                settings?.let { s ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.sale_sms), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Switch(checked = s.smsDefaultOn, onCheckedChange = { on -> scope.launch { app.settings.setSmsDefault(on) } })
                    }
                }
            }
            MoreRow(stringResource(R.string.more_language)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (bangla) "en" else "bn"))
            }
            // Opens the Play listing. The in-app review sheet replaces this once the app is published.
            MoreRow(stringResource(R.string.more_rate)) {
                val id = ctx.packageName.removeSuffix(".debug")
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))) }
                    .onFailure { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))) }
            }
            MoreRow(stringResource(R.string.more_share)) {
                val id = ctx.packageName.removeSuffix(".debug")
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "ERP Inventory: https://play.google.com/store/apps/details?id=$id")
                ctx.startActivity(Intent.createChooser(send, null))
            }
            MoreRow(stringResource(R.string.more_help)) {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP")))
            }
            MoreRow(stringResource(R.string.more_sign_out)) {
                scope.launch { app.settings.setSession(Session.SignedOut); signedOut() }
            }
            DevFooter()
        }
    }
}

@Composable
fun BizProfileScreen(onBack: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var p by remember { mutableStateOf<BusinessProfile?>(null) }
    LaunchedEffect(Unit) { p = app.settings.current().profile }
    Scaffold(topBar = { ErpTopBar(stringResource(R.string.biz_title), onBack) }) { pad ->
        val b = p ?: return@Scaffold
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.biz_note), style = MaterialTheme.typography.bodySmall)
            Field(b.name, { p = b.copy(name = it) }, stringResource(R.string.onb_shop_name))
            Field(b.slogan, { p = b.copy(slogan = it) }, stringResource(R.string.biz_slogan))
            Field(b.address, { p = b.copy(address = it) }, stringResource(R.string.biz_address), singleLine = false)
            Field(b.phone, { p = b.copy(phone = it) }, stringResource(R.string.onb_phone), phone = true)
            Field(b.email, { p = b.copy(email = it) }, stringResource(R.string.biz_email))
            Field(b.web, { p = b.copy(web = it) }, stringResource(R.string.biz_web))
            Field(b.bin, { p = b.copy(bin = it) }, stringResource(R.string.biz_bin))
            Field(b.thanks, { p = b.copy(thanks = it) }, stringResource(R.string.biz_thanks))
            Field(b.returnPolicy, { p = b.copy(returnPolicy = it) }, stringResource(R.string.biz_return))
            Button(
                onClick = { scope.launch { app.settings.saveProfile(b); onBack() } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

private val menuLabels = listOf(
    StaffMenu.SALE to R.string.home_new_sale, StaffMenu.PRODUCTS to R.string.home_products,
    StaffMenu.CUSTOMERS to R.string.home_customers, StaffMenu.REPORTS to R.string.home_reports,
)

@Composable
fun StaffManageScreen(onBack: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val staff by remember { app.repo.staff.all() }.collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var menus by remember { mutableStateOf(setOf(StaffMenu.SALE)) }

    Scaffold(topBar = { ErpTopBar(stringResource(R.string.staff_manage), onBack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            staff.forEach { s ->
                Text(
                    s.name, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                )
            }
            Text(stringResource(R.string.staff_add), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Field(name, { name = it }, stringResource(R.string.staff_name))
            Field(pin, { pin = it.filter(Char::isDigit).take(4) }, stringResource(R.string.staff_pin), number = true)
            Text(stringResource(R.string.staff_menus), style = MaterialTheme.typography.titleSmall)
            menuLabels.forEach { (m, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = m in menus, onCheckedChange = { on -> menus = if (on) menus + m else menus - m })
                    Text(stringResource(label))
                }
            }
            Button(
                enabled = name.isNotBlank() && pin.length == 4,
                onClick = {
                    scope.launch {
                        app.repo.addStaff(name, pin, menus)
                        name = ""; pin = ""; menus = setOf(StaffMenu.SALE)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
fun SoonScreen(onBack: () -> Unit) {
    Scaffold(topBar = { ErpTopBar("", onBack) }) { pad ->
        Text(
            stringResource(R.string.coming_soon), style = MaterialTheme.typography.titleMedium, color = Brand.Blue,
            modifier = Modifier.padding(pad).padding(24.dp),
        )
    }
}
