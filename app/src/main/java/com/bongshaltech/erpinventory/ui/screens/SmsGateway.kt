package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.sms.GatewayConfig
import com.bongshaltech.erpinventory.core.sms.Provider
import com.bongshaltech.erpinventory.data.SmsGateway
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import kotlinx.coroutines.launch

/** More › SMS gateway: the shop's own SMS provider and API key. Without one, receipts go from the SIM. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmsGatewayScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val saved = remember { SmsGateway.load(ctx) }
    var provider by remember { mutableStateOf(saved?.provider ?: Provider.ALPHA) }
    var key by remember { mutableStateOf(saved?.apiKey.orEmpty()) }
    var sender by remember { mutableStateOf(saved?.senderId.orEmpty()) }
    var url by remember { mutableStateOf(saved?.customUrl.orEmpty()) }
    var testPhone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(if (saved?.ready == true) ctx.getString(R.string.gw_on) else ctx.getString(R.string.gw_off)) }
    var busy by remember { mutableStateOf(false) }
    val cfg = GatewayConfig(provider, key, sender, url)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.gw_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Muted(stringResource(R.string.gw_help), 13.sp)
            Panel(stringResource(R.string.gw_provider), Brand.BlueGrad, PanelColors.Blue) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Provider.entries.forEach { p -> Chip(p.label, provider == p, { provider = p }) }
                }
                Input(key, { key = it }, label = stringResource(R.string.gw_key), password = true)
                Input(sender, { sender = it }, label = stringResource(R.string.gw_sender))
                if (provider == Provider.CUSTOM) {
                    Input(url, { url = it }, label = stringResource(R.string.gw_url), keyboard = KeyboardType.Uri)
                    Muted(stringResource(R.string.gw_url_help), 12.sp)
                }
                GradButton(stringResource(R.string.gw_save), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = cfg.ready) {
                    SmsGateway.save(ctx, cfg); note = ctx.getString(R.string.gw_on)
                }
            }
            Panel(stringResource(R.string.gw_test), Brand.GreenGrad, PanelColors.Green) {
                Input(testPhone, { testPhone = it }, label = stringResource(R.string.gw_phone), keyboard = KeyboardType.Phone)
                GradButton(stringResource(if (busy) R.string.sms_working else R.string.gw_test_send), Brand.GreenGrad, Modifier.fillMaxWidth(),
                    enabled = cfg.ready && testPhone.isNotBlank() && !busy) {
                    busy = true
                    scope.launch {
                        note = when (val r = SmsGateway.send(ctx, testPhone, ctx.getString(R.string.gw_test_text), cfg)) {
                            SmsGateway.Result.Sent -> ctx.getString(R.string.gw_sent)
                            SmsGateway.Result.Offline -> ctx.getString(R.string.sms_offline)
                            SmsGateway.Result.BadNumber -> ctx.getString(R.string.gw_bad_number)
                            is SmsGateway.Result.Refused -> ctx.getString(R.string.gw_refused, r.why)
                        }
                        busy = false
                    }
                }
            }
            Muted(note, 13.sp)
            if (saved != null) WhiteButton(stringResource(R.string.gw_remove), Modifier.fillMaxWidth()) {
                SmsGateway.clear(ctx); key = ""; sender = ""; url = ""; note = ctx.getString(R.string.gw_off)
            }
        }
    }
}
