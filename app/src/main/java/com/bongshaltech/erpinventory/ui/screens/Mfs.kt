package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.Money
import com.bongshaltech.erpinventory.core.mfs.BalanceCounterparty
import com.bongshaltech.erpinventory.core.mfs.MfsSmsParser
import com.bongshaltech.erpinventory.core.mfs.MfsType
import com.bongshaltech.erpinventory.core.mfs.WalletGroup
import com.bongshaltech.erpinventory.data.MfsResult
import com.bongshaltech.erpinventory.data.NewMfs
import com.bongshaltech.erpinventory.data.SharedSms
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.MfsTxnRow
import com.bongshaltech.erpinventory.data.db.MfsWalletEntity
import com.bongshaltech.erpinventory.data.group
import com.bongshaltech.erpinventory.data.hasCharge
import com.bongshaltech.erpinventory.data.isBalance
import com.bongshaltech.erpinventory.data.label
import com.bongshaltech.erpinventory.data.mfsEffect
import com.bongshaltech.erpinventory.data.providerOrNull
import com.bongshaltech.erpinventory.data.typesFor
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.SumRow
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Toggle
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val mfsTypeLabel = mapOf(
    MfsType.CASH_IN to R.string.mfs_cash_in, MfsType.CASH_OUT to R.string.mfs_cash_out, MfsType.SEND_MONEY to R.string.mfs_send,
    MfsType.PAYMENT to R.string.mfs_payment, MfsType.PAY_BILL to R.string.mfs_pay_bill, MfsType.RECHARGE to R.string.mfs_recharge,
    MfsType.INTERNET_PACK to R.string.mfs_internet, MfsType.MINUTE_PACK to R.string.mfs_minute, MfsType.BUNDLE_PACK to R.string.mfs_bundle,
    MfsType.BALANCE_IN to R.string.mfs_balance_in, MfsType.BALANCE_OUT to R.string.mfs_balance_out,
    MfsType.SCRATCH_CARD to R.string.mfs_payment, MfsType.SIM_SALE to R.string.mfs_payment,
)

private val viaLabel = listOf(
    BalanceCounterparty.DSO to R.string.mfs_via_dso, BalanceCounterparty.CASH to R.string.mfs_via_cash,
    BalanceCounterparty.BANK to R.string.mfs_via_bank, BalanceCounterparty.OTHER_WALLET to R.string.mfs_via_wallet,
)

/** Signed money for wallet / cash effects: "+৳ 500.00", "-৳ 500.00". */
private fun Long.signed() = (if (this > 0) "+" else "") + taka()

@Composable
private fun ProviderBadge(w: MfsWalletEntity, size: Int = 40) {
    val c = Color(w.providerOrNull()?.color ?: 0xFF6B7280)
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)).background(c),
        contentAlignment = Alignment.Center,
    ) { T(w.label().take(1), (size / 2.4f).sp, FontWeight.Bold, Color.White) }
}

private fun startOfToday(): Long = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
fun MfsScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.MOBILE_BANKING)) return NotAllowed(onBack)
    LaunchedEffect(Unit) { app.repo.ensureWallets() }
    val today = remember { startOfToday() }
    val wallets by remember { app.repo.mfs.wallets() }.collectAsStateWithLifecycle(emptyList())
    val active = wallets.filter { it.active }
    val recent by remember { app.repo.mfs.recent(today, 50) }.collectAsStateWithLifecycle(emptyList())
    val sum by remember { app.repo.mfs.summary(today) }.collectAsStateWithLifecycle(null)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.mfs_title), onBack) {
            if (who.owner) T(stringResource(R.string.mfs_rates_short), 13.sp, FontWeight.SemiBold, Brand.Link,
                Modifier.clickable { go(Routes.MFS_WALLETS) }.padding(8.dp))
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (active.isEmpty()) item {
                    Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                        T(stringResource(R.string.mfs_no_wallets), 15.sp, FontWeight.SemiBold)
                        Muted(stringResource(R.string.mfs_no_wallets_sub), 13.sp)
                        if (who.owner) GradButton(stringResource(R.string.mfs_setup), Brand.BlueGrad, Modifier.fillMaxWidth().padding(top = 8.dp), height = 44.dp) {
                            go(Routes.MFS_WALLETS)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(Modifier.weight(1f), padding = 12.dp) {
                            Muted(stringResource(R.string.mfs_today_earning))
                            T((sum?.earningPaisa ?: 0).taka(), 18.sp, FontWeight.Bold, Brand.DarkGreen)
                            Muted(stringResource(R.string.mfs_entries, sum?.count ?: 0))
                        }
                        Card(Modifier.weight(1f), padding = 12.dp) {
                            Muted(stringResource(R.string.mfs_today_cash))
                            val cash = sum?.cashPaisa ?: 0
                            T(cash.signed(), 18.sp, FontWeight.Bold, if (cash < 0) Brand.Red else Ui.c.text)
                            Muted(stringResource(R.string.mfs_cash_drawer))
                        }
                    }
                }
                items(active, key = { "w${it.id}" }) { w ->
                    Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { go(Routes.mfsEntry(w.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProviderBadge(w)
                            Column(Modifier.weight(1f)) {
                                T(w.label(), 15.sp, FontWeight.SemiBold)
                                Muted(listOf(
                                    stringResource(if (w.group() == WalletGroup.LOAD) R.string.mfs_load else if (w.personal) R.string.mfs_personal else R.string.mfs_agent),
                                    w.number,
                                ).filter { it.isNotBlank() }.joinToString(" · "))
                            }
                            T(w.balancePaisa.taka(), 15.sp, FontWeight.Bold)
                        }
                    }
                }
                if (recent.isNotEmpty()) item { T(stringResource(R.string.mfs_today_entries), 15.sp, FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                items(recent, key = { "t${it.id}" }) { TxnRow(it) }
            }
            if (active.isNotEmpty()) {
                val shape = CircleShape
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(16.dp).height(52.dp)
                        .shadow(10.dp, shape, ambientColor = Brand.Pink, spotColor = Brand.Pink).clip(shape)
                        .background(Brand.BlueGrad).clickable { go(Routes.mfsEntry(0)) }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    T(stringResource(R.string.mfs_new), 15.sp, FontWeight.SemiBold, Color.White, shadow = true)
                }
            }
        }
    }
}

@Composable
private fun TxnRow(t: MfsTxnRow) {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val type = runCatching { MfsType.valueOf(t.type) }.getOrNull()
    val provider = com.bongshaltech.erpinventory.data.MfsProvider.entries.firstOrNull { it.name == t.provider }
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T("${provider?.label ?: t.provider} · ${stringResource(mfsTypeLabel[type] ?: R.string.mfs_payment)}", 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(listOf(t.customerPhone, t.trxId, fmt.format(Date(t.createdAt)), t.number).filter { it.isNotBlank() }.joinToString(" · "))
                if (t.earningPaisa != 0L) T(stringResource(R.string.mfs_earning_is, t.earningPaisa.taka()), 12.sp, FontWeight.SemiBold, Brand.DarkGreen)
            }
            T(t.amountPaisa.taka(), 14.sp, FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MfsEntryScreen(walletId: Long, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.MOBILE_BANKING)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { app.repo.ensureWallets() }
    val wallets by remember { app.repo.mfs.wallets() }.collectAsStateWithLifecycle(emptyList())
    val active = wallets.filter { it.active }
    var wid by rememberSaveable { mutableStateOf(walletId) }
    val w = active.firstOrNull { it.id == wid } ?: active.firstOrNull()
    var type by rememberSaveable { mutableStateOf<MfsType?>(null) }
    var phone by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var charge by rememberSaveable { mutableStateOf("") }
    var fee by rememberSaveable { mutableStateOf("") }
    var settled by rememberSaveable { mutableStateOf("") }
    var via by rememberSaveable { mutableStateOf(BalanceCounterparty.DSO) }
    var trx by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var tried by remember { mutableStateOf(false) }
    val suggestions = remember { mutableStateListOf<String>() }

    // A wallet SMS shared to the app fills what it can; the shopkeeper checks the rest.
    LaunchedEffect(Unit) {
        val sms = SharedSms.text ?: return@LaunchedEffect
        SharedSms.text = null
        val p = MfsSmsParser.parse(sms)
        p.amount?.let { amount = (it.paisa / 100.0).let { v -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString() } }
        p.fee?.let { fee = (it.paisa / 100.0).toString() }
        p.trxId?.let { trx = it }
        p.number?.let { phone = it }
        p.maskedNumber?.let { m ->
            if (p.number == null) {
                phone = m
                suggestions.clear(); suggestions.addAll(MfsSmsParser.matchMasked(m, app.repo.mfs.knownNumbers()).take(5))
            }
        }
        val lower = sms.lowercase()
        when {
            "cash out" in lower || "cashout" in lower -> type = MfsType.CASH_OUT
            "cash in" in lower || "cashin" in lower -> type = MfsType.CASH_IN
            "send money" in lower -> type = MfsType.SEND_MONEY
            "bill" in lower -> type = MfsType.PAY_BILL
            "recharge" in lower || "flexiload" in lower -> type = MfsType.RECHARGE
        }
    }

    val types = w?.let { typesFor(it) } ?: emptyList()
    val t = type?.takeIf { it in types } ?: types.firstOrNull()
    val amountP = amount.toPaisa()
    val n = if (w != null && t != null && amountP != null && amountP > 0) NewMfs(
        walletId = w.id, type = t, customerPhone = phone, amountPaisa = amountP,
        chargePaisa = charge.toPaisa() ?: 0, feePaisa = fee.toPaisa() ?: 0,
        settledPaisa = settled.toPaisa(), via = via, trxId = trx, note = note, staffId = who.staffId,
    ) else null
    val effect = if (w != null && n != null) mfsEffect(w, n) else null
    val lowMsg = stringResource(R.string.mfs_low_balance, "%s")
    val savedMsg = stringResource(R.string.mfs_saved)
    val required = stringResource(R.string.required)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.mfs_new), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (active.isEmpty()) Muted(stringResource(R.string.mfs_no_wallets_sub), 14.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                active.forEach { x -> Chip("${x.label()} · ${x.balancePaisa.taka()}", x.id == w?.id, { wid = x.id }) }
            }
            if (w != null && t != null) Panel(stringResource(R.string.mfs_entry_panel), Brand.BlueGrad, PanelColors.Blue) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { x -> Chip(stringResource(mfsTypeLabel.getValue(x)), x == t, { type = x }, soft = true) }
                }
                if (!t.isBalance) {
                    Input(phone, { phone = it; suggestions.clear() }, label = stringResource(if (t == MfsType.PAY_BILL) R.string.mfs_bill_no else R.string.mfs_customer_no),
                        keyboard = if (t == MfsType.PAY_BILL) KeyboardType.Text else KeyboardType.Phone)
                    suggestions.forEach { s ->
                        T(stringResource(R.string.mfs_use_number, s), 13.sp, FontWeight.SemiBold, Brand.Link,
                            Modifier.clickable { phone = s; suggestions.clear() }.padding(vertical = 4.dp))
                    }
                }
                Input(amount, { amount = it }, label = stringResource(R.string.mfs_amount), keyboard = KeyboardType.Decimal,
                    error = required.takeIf { tried && (amountP == null || amountP <= 0) })
                if (t.hasCharge) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(charge, { charge = it }, Modifier.weight(1f), label = stringResource(R.string.mfs_charge), keyboard = KeyboardType.Decimal)
                    Input(fee, { fee = it }, Modifier.weight(1f), label = stringResource(R.string.mfs_fee), keyboard = KeyboardType.Decimal)
                }
                if (t.isBalance) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        viaLabel.forEach { (v, l) -> Chip(stringResource(l), via == v, { via = v }) }
                    }
                    Input(settled, { settled = it }, label = stringResource(if (t == MfsType.BALANCE_IN) R.string.mfs_paid_for else R.string.mfs_got_for),
                        placeholder = amount, keyboard = KeyboardType.Decimal)
                }
                Input(trx, { trx = it }, label = stringResource(R.string.mfs_trx))
                Input(note, { note = it }, label = stringResource(R.string.cust_note))
            }
            if (effect != null) Panel(stringResource(R.string.mfs_effect), Brand.GreenGrad, PanelColors.Green) {
                SumRow(stringResource(R.string.mfs_wallet_effect, w!!.label()), effect.walletDelta.paisa.signed())
                SumRow(stringResource(R.string.mfs_cash_effect), effect.cashDelta.paisa.signed())
                SumRow(stringResource(R.string.mfs_earning), effect.earning.paisa.taka(), strong = true, color = Brand.DarkGreen)
                if (!t!!.isBalance && !t.hasCharge && effect.earning.isZero) Muted(stringResource(R.string.mfs_set_rate), 12.sp)
            }
            message?.let { T(it, 13.sp, FontWeight.SemiBold, Brand.Red) }
        }
        BottomBar {
            GradButton(stringResource(R.string.mfs_save), Brand.BlueGrad, Modifier.weight(1f), height = 52.dp, enabled = w != null) {
                tried = true
                val entry = n ?: return@GradButton
                scope.launch {
                    when (val r = app.repo.addMfs(entry)) {
                        is MfsResult.Saved -> { Toast.makeText(ctx, "$savedMsg · ${r.number}", Toast.LENGTH_SHORT).show(); onBack() }
                        is MfsResult.LowBalance -> message = lowMsg.replace("%s", r.balancePaisa.taka())
                        MfsResult.NoWallet -> Unit
                    }
                }
            }
        }
    }
}

/** Owner: which wallets the shop runs, agent or personal, number, opening balance and commission rates. */
@Composable
fun MfsWalletsScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { app.repo.ensureWallets() }
    val wallets by remember { app.repo.mfs.wallets() }.collectAsStateWithLifecycle(emptyList())
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.mfs_rates_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Muted(stringResource(R.string.mfs_rates_note), 13.sp)
            wallets.forEach { w -> WalletCard(w) { scope.launch { app.repo.saveWallet(it) } } }
        }
    }
}

@Composable
private fun WalletCard(w: MfsWalletEntity, save: (MfsWalletEntity) -> Unit) {
    val load = w.group() == WalletGroup.LOAD
    var number by remember(w.id) { mutableStateOf(w.number) }
    var balance by remember(w.id) { mutableStateOf(if (w.balancePaisa != 0L) Money(w.balancePaisa).format(false).replace(",", "") else "") }
    var inPct by remember(w.id) { mutableStateOf(w.cashInPct.pct()) }
    var outPct by remember(w.id) { mutableStateOf(w.cashOutPct.pct()) }
    var payPct by remember(w.id) { mutableStateOf(w.paymentPct.pct()) }
    var loadPct by remember(w.id) { mutableStateOf(w.loadPct.pct()) }
    var dirty by remember(w.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), padding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProviderBadge(w, 36)
                T(w.label(), 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Toggle(w.active, { on -> save(w.copy(active = on)) })
            }
            if (w.active) {
                if (!load) Segmented(
                    listOf(stringResource(R.string.mfs_agent), stringResource(R.string.mfs_personal)), if (w.personal) 1 else 0,
                ) { i -> save(w.copy(personal = i == 1)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(number, { number = it; dirty = true }, Modifier.weight(1f), label = stringResource(R.string.mfs_number), keyboard = KeyboardType.Phone, height = 42.dp)
                    Input(balance, { balance = it; dirty = true }, Modifier.weight(1f), label = stringResource(R.string.mfs_balance), keyboard = KeyboardType.Decimal, height = 42.dp)
                }
                if (load) Input(loadPct, { loadPct = it; dirty = true }, label = stringResource(R.string.mfs_rate_load), keyboard = KeyboardType.Decimal, height = 42.dp)
                else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(inPct, { inPct = it; dirty = true }, Modifier.weight(1f), label = stringResource(R.string.mfs_rate_in), keyboard = KeyboardType.Decimal, height = 42.dp)
                    Input(outPct, { outPct = it; dirty = true }, Modifier.weight(1f), label = stringResource(R.string.mfs_rate_out), keyboard = KeyboardType.Decimal, height = 42.dp)
                    Input(payPct, { payPct = it; dirty = true }, Modifier.weight(1f), label = stringResource(R.string.mfs_rate_pay), keyboard = KeyboardType.Decimal, height = 42.dp)
                }
                if (dirty) WhiteButton(stringResource(R.string.mfs_save_wallet), Modifier.fillMaxWidth(), height = 42.dp) {
                    save(
                        w.copy(
                            number = number.trim(), balancePaisa = balance.toPaisa() ?: w.balancePaisa,
                            cashInPct = inPct.toPct(), cashOutPct = outPct.toPct(), paymentPct = payPct.toPct(), loadPct = loadPct.toPct(),
                        )
                    )
                    dirty = false
                }
            }
        }
    }
}

private fun Double.pct() = if (this == 0.0) "" else toBigDecimal().stripTrailingZeros().toPlainString()
private fun String.toPct() = trim().replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
