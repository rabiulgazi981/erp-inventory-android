package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.courier.CourierKind
import com.bongshaltech.erpinventory.core.courier.Parcel
import com.bongshaltech.erpinventory.core.courier.secretFields
import com.bongshaltech.erpinventory.data.Couriers
import com.bongshaltech.erpinventory.data.db.CourierParcelEntity
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private fun fieldLabel(f: String) = when (f) {
    "apiKey" -> "API key"; "secretKey" -> "Secret key"; "clientId" -> "Client ID"; "clientSecret" -> "Client secret"
    "username" -> "Username / e-mail"; "password" -> "Password"; "apiToken" -> "API token"; "userId" -> "User ID"
    "apiSecret" -> "API secret"; "url" -> "Webhook link (https)"; "secret" -> "Secret (optional)"; else -> f
}

/** More › Courier: connect the shop's own courier accounts, and see parcels sent from sales. */
@Composable
fun CourierScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.cr_title), onBack)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Segmented(listOf(stringResource(R.string.cr_parcels), stringResource(R.string.cr_accounts)), tab) { tab = it }
        }
        if (tab == 0) ParcelList(Modifier.weight(1f)) else CourierAccounts(Modifier.weight(1f))
    }
}

@Composable
private fun ParcelList(modifier: Modifier) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val parcels by remember { app.repo.couriers.recent() }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (parcels.isEmpty()) item { Muted(stringResource(R.string.cr_none), 14.sp) }
        items(parcels, key = { it.id }) { p ->
            val kind = runCatching { CourierKind.valueOf(p.courier) }.getOrNull()
            Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        T(p.saleNumber + " · " + (kind?.label ?: p.courier), 15.sp, FontWeight.Bold)
                        Muted(p.name + " · " + p.phone)
                        Muted(stringResource(R.string.cr_cid, p.consignmentId) + (if (p.trackingCode.isNotBlank()) " · " + p.trackingCode else ""))
                        Muted(fmt.format(Date(p.updatedAt)) + " · COD " + p.codPaisa.taka())
                    }
                    T(p.status, 13.sp, FontWeight.SemiBold, Brand.Link)
                }
                if (kind == CourierKind.STEADFAST) WhiteButton(stringResource(R.string.cr_check), Modifier.fillMaxWidth(), height = 40.dp) {
                    scope.launch {
                        val st = Couriers.status(ctx, kind, p.consignmentId)
                        if (st == null) Toast.makeText(ctx, ctx.getString(R.string.cr_no_status), Toast.LENGTH_LONG).show()
                        else app.repo.couriers.setStatus(p.id, st, System.currentTimeMillis())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourierAccounts(modifier: Modifier) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    var kind by remember { mutableStateOf(CourierKind.STEADFAST) }
    val values = remember(kind) { mutableStateMapOf<String, String>().apply { putAll(Couriers.values(ctx, kind)) } }
    var note by remember(kind) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var readyMap by remember { mutableStateOf(CourierKind.entries.associateWith { Couriers.ready(ctx, it) }) }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Muted(stringResource(R.string.cr_help), 13.sp) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CourierKind.entries.forEach { c -> Chip(c.label + if (readyMap[c] == true) " ✓" else "", kind == c, { kind = c }) }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                T(kind.label, 16.sp, FontWeight.Bold)
                Muted(stringResource(when {
                    kind == CourierKind.WEBHOOK -> R.string.cr_webhook_help
                    kind.canBook -> R.string.cr_can_book
                    kind.canTest -> R.string.cr_test_only
                    else -> R.string.cr_save_only
                }), 12.sp)
                kind.fields.forEach { f ->
                    Input(values[f].orEmpty(), { values[f] = it }, label = fieldLabel(f), password = f in secretFields,
                        keyboard = if (f == "url") KeyboardType.Uri else KeyboardType.Text)
                }
                val ready = kind.ready(values)
                GradButton(stringResource(R.string.gw_save), Brand.BlueGrad, Modifier.fillMaxWidth(), height = 44.dp, enabled = ready) {
                    Couriers.save(ctx, kind, values)
                    readyMap = CourierKind.entries.associateWith { Couriers.ready(ctx, it) }
                    note = ctx.getString(R.string.cr_saved)
                }
                if (kind.canTest) WhiteButton(stringResource(if (busy) R.string.sms_working else R.string.cr_test), Modifier.fillMaxWidth(), height = 44.dp) {
                    if (!ready || busy) return@WhiteButton
                    busy = true
                    scope.launch {
                        val r = Couriers.test(ctx, kind, values.toMap(), settings?.profile?.name.orEmpty())
                        note = (if (r.ok) "✓ " else "✗ ") + r.detail
                        busy = false
                    }
                }
                if (readyMap[kind] == true) T(stringResource(R.string.cr_remove), 13.sp, FontWeight.SemiBold, Brand.Red,
                    Modifier.clickable {
                        Couriers.clear(ctx, kind); values.clear(); kind.fields.forEach { values[it] = "" }
                        readyMap = CourierKind.entries.associateWith { Couriers.ready(ctx, it) }
                        note = ""
                    }.padding(vertical = 6.dp))
                if (note.isNotBlank()) Muted(note, 13.sp)
            }
        }
    }
}

/** Receipt › Send to courier: books the sale as a parcel with one of the shop's connected couriers. */
@Composable
fun SendToCourierDialog(saleId: Long, itemsText: String, onClose: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val options = remember { Couriers.bookable(ctx) }
    var kind by remember { mutableStateOf(options.firstOrNull()) }
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var cod by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(saleId) {
        val s = app.repo.sales.get(saleId) ?: return@LaunchedEffect
        number = s.number
        cod = ((s.totalPaisa - s.paidPaisa).coerceAtLeast(0) / 100).toString()
        s.customerId?.let { app.repo.customers.get(it) }?.let { c ->
            name = if (c.name == c.phone) "" else c.name; phone = c.phone; address = c.address
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { T(stringResource(R.string.cr_send), 18.sp, FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (options.isEmpty()) Muted(stringResource(R.string.cr_none_connected), 13.sp)
                if (options.isNotEmpty() && options.size > 1) Segmented(options.map { it.label }, options.indexOf(kind).coerceAtLeast(0)) { kind = options[it] }
                if (options.isNotEmpty()) Input(name, { name = it }, label = stringResource(R.string.cr_name))
                if (options.isNotEmpty()) Input(phone, { phone = it }, label = stringResource(R.string.gw_phone), keyboard = KeyboardType.Phone)
                if (options.isNotEmpty()) Input(address, { address = it }, label = stringResource(R.string.cr_address))
                if (options.isNotEmpty()) Input(cod, { cod = it.filter { ch -> ch.isDigit() } }, label = stringResource(R.string.cr_cod), keyboard = KeyboardType.Number)
                if (options.isNotEmpty()) Input(note, { note = it }, label = stringResource(R.string.cr_note))
                error?.let { Muted(it, 13.sp) }
            }
        },
        confirmButton = {
            if (options.isNotEmpty()) GradButton(stringResource(if (busy) R.string.sms_working else R.string.cr_book), Brand.BlueGrad, height = 44.dp,
                enabled = !busy && name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                val k = kind ?: return@GradButton
                busy = true; error = null
                scope.launch {
                    val codPaisa = (cod.toLongOrNull() ?: 0) * 100
                    val p = Parcel(number, name.trim(), phone.trim(), address.trim(), codPaisa, itemsText, note.trim())
                    when (val r = Couriers.book(ctx, k, p, settings?.profile?.name.orEmpty())) {
                        is Couriers.Booked.Ok -> {
                            app.repo.saveParcel(CourierParcelEntity(saleId = saleId, saleNumber = number, courier = k.name, consignmentId = r.consignmentId,
                                trackingCode = r.trackingCode, status = "booked", codPaisa = codPaisa, name = p.name, phone = p.phone, address = p.address))
                            Toast.makeText(ctx, ctx.getString(R.string.cr_done, r.consignmentId), Toast.LENGTH_LONG).show()
                            onClose()
                        }
                        is Couriers.Booked.Duplicate -> error = ctx.getString(R.string.cr_dup) + "\n" + r.detail
                        is Couriers.Booked.Unknown -> error = ctx.getString(R.string.cr_unknown) + "\n" + r.detail
                        is Couriers.Booked.Refused -> error = r.detail
                    }
                    busy = false
                }
            }
        },
        dismissButton = { WhiteButton(stringResource(R.string.bk_cancel), height = 44.dp) { if (!busy) onClose() } },
        containerColor = Ui.c.card,
    )
}
