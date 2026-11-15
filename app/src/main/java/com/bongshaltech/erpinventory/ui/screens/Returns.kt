package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Replay
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.ReturnPick
import com.bongshaltech.erpinventory.data.ReturnResult
import com.bongshaltech.erpinventory.data.Returns
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.data.db.ReturnRow
import com.bongshaltech.erpinventory.data.db.ReturnableLine
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
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toMilli
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** A return of goods from a sale ([purchase] false) or back to a supplier ([purchase] true). */
@Composable
fun ReturnScreen(purchase: Boolean, docId: Long, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(if (purchase) StaffMenu.PURCHASE else StaffMenu.SALE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()

    var number by remember { mutableStateOf("") }
    var party by remember { mutableStateOf<String?>(null) }
    var partyDue by remember { mutableStateOf(0L) }
    var subtotal by remember { mutableStateOf(0L) }
    var docTotal by remember { mutableStateOf(0L) }
    var lines by remember { mutableStateOf<List<ReturnableLine>>(emptyList()) }
    val qty = remember { mutableStateMapOf<Long, String>() }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf<ReturnResult.Saved?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(docId, reload) {
        val r = app.repo
        if (purchase) {
            val p = r.purchases.get(docId) ?: return@LaunchedEffect
            number = p.number; subtotal = p.subtotalPaisa; docTotal = p.totalPaisa
            val s = p.supplierId?.let { r.suppliers.get(it) }
            party = s?.name; partyDue = s?.duePaisa ?: 0
            lines = r.returns.purchaseLines(docId)
        } else {
            val s = r.sales.get(docId) ?: return@LaunchedEffect
            number = s.number; subtotal = s.subtotalPaisa; docTotal = s.totalPaisa
            val c = s.customerId?.let { r.customers.get(it) }
            party = c?.name; partyDue = c?.duePaisa ?: 0
            lines = r.returns.saleLines(docId)
        }
        qty.clear()
    }

    val picks = lines.mapNotNull { l -> qty[l.id]?.toMilli()?.takeIf { it > 0 }?.let { ReturnPick(l, it) } }
    val tooMany = picks.any { it.qtyMilli > it.line.leftMilli }
    val split = Returns.split(Returns.value(picks, subtotal, docTotal), partyDue)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(if (purchase) R.string.ret_purchase_title else R.string.ret_sale_title) + if (number.isNotEmpty()) " · $number" else "", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            party?.let { Muted(stringResource(if (purchase) R.string.ret_supplier else R.string.ret_customer, it), 14.sp) }
            Panel(stringResource(R.string.ret_pick), Brand.OrangeGrad, PanelColors.Orange) {
                if (lines.isEmpty()) Muted(stringResource(R.string.sms_working))
                lines.forEach { l ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            T(l.name, 14.sp, FontWeight.SemiBold, maxLines = 2)
                            Muted(
                                stringResource(if (purchase) R.string.ret_bought else R.string.ret_sold, l.qtyMilli.qty()) +
                                    if (l.returnedMilli > 0) " · " + stringResource(R.string.ret_already, l.returnedMilli.qty()) else ""
                            )
                        }
                        if (l.leftMilli > 0) {
                            Chip(stringResource(R.string.ret_all), qty[l.id]?.toMilli() == l.leftMilli, { qty[l.id] = l.leftMilli.qty() }, soft = true)
                            val bad = (qty[l.id]?.toMilli() ?: 0) > l.leftMilli
                            Input(qty[l.id].orEmpty(), { qty[l.id] = it }, Modifier.width(76.dp), placeholder = "0",
                                keyboard = KeyboardType.Decimal, alignEnd = true, error = if (bad) "" else null)
                        } else Muted(stringResource(R.string.ret_none_left))
                    }
                }
            }
            if (picks.isNotEmpty()) Panel(stringResource(R.string.ret_settle), Brand.BlueGrad, PanelColors.Blue) {
                SumRow(stringResource(R.string.ret_value), split.totalPaisa.taka())
                if (split.dueCutPaisa > 0) SumRow(
                    stringResource(if (purchase) R.string.ret_debit_note else R.string.ret_credit_note), split.dueCutPaisa.taka(),
                )
                SumRow(
                    stringResource(if (purchase) R.string.ret_they_pay else R.string.ret_we_pay), split.refundPaisa.taka(),
                    strong = true, color = if (split.refundPaisa > 0) Brand.Red else Ui.c.text,
                )
                if (split.refundPaisa > 0) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (m, label) -> Chip(stringResource(label), method == m, { method = m }) }
                }
                Input(note, { note = it }, label = stringResource(R.string.ret_reason))
            }
            message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
        }
        BottomBar {
            GradButton(
                stringResource(R.string.ret_save) + if (picks.isNotEmpty()) " · ${split.totalPaisa.taka0()}" else "",
                Brand.OrangeGrad, Modifier.weight(1f), enabled = picks.isNotEmpty() && !tooMany,
            ) {
                scope.launch {
                    val r = if (purchase) app.repo.returnPurchase(docId, picks, method, note, who.staffId)
                    else app.repo.returnSale(docId, picks, method, note, who.staffId)
                    when (r) {
                        is ReturnResult.Saved -> { done = r; message = null }
                        is ReturnResult.NotEnoughStock -> message = app.getString(R.string.ret_no_stock, r.productName)
                        ReturnResult.Invalid -> { message = app.getString(R.string.ret_invalid); reload++ }
                    }
                }
            }
        }
    }

    done?.let { d ->
        AlertDialog(
            onDismissRequest = {},
            title = { T(stringResource(R.string.ret_saved, d.number), 18.sp, FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SumRow(stringResource(R.string.ret_value), d.split.totalPaisa.taka())
                    if (d.split.dueCutPaisa > 0) SumRow(stringResource(if (purchase) R.string.ret_debit_note else R.string.ret_credit_note), d.split.dueCutPaisa.taka())
                    if (d.split.refundPaisa > 0) SumRow(stringResource(if (purchase) R.string.ret_they_pay else R.string.ret_we_pay), d.split.refundPaisa.taka(), strong = true)
                }
            },
            confirmButton = { GradButton(stringResource(R.string.sale_done), Brand.BlueGrad, height = 44.dp) { done = null; onBack() } },
            containerColor = Ui.c.card,
        )
    }
}

/** More › Returns: past returns both ways, and starting a new one from a recent sale or purchase. */
@Composable
fun ReturnsScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    var tab by remember { mutableStateOf(0) }
    val list by remember { app.repo.returns.recent(100) }.collectAsStateWithLifecycle(emptyList())
    val sales by remember { app.repo.sales.recent(60) }.collectAsStateWithLifecycle(emptyList())
    val purchases by remember { app.repo.purchases.recent(60) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val canSale = who.can(StaffMenu.SALE)
    val canPurchase = who.can(StaffMenu.PURCHASE)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.returns), onBack)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Segmented(
                listOf(stringResource(R.string.ret_tab_list), stringResource(R.string.ret_tab_sale), stringResource(R.string.ret_tab_purchase)),
                tab, { tab = it },
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (tab) {
                0 -> {
                    if (list.isEmpty()) item { Muted(stringResource(R.string.ret_empty), 14.sp) }
                    items(list, key = { (if (it.isPurchase) "p" else "s") + it.id }) { r -> ReturnRowCard(r, fmt) }
                }
                1 -> {
                    item { Muted(stringResource(R.string.ret_pick_sale), 13.sp) }
                    if (!canSale) item { Muted(stringResource(R.string.not_allowed), 14.sp) }
                    else items(sales, key = { "s" + it.id }) { s ->
                        DocCard(Icons.Outlined.Replay, s.number, listOfNotNull(s.customerName, fmt.format(Date(s.createdAt))), s.totalPaisa) {
                            go(Routes.saleReturn(s.id))
                        }
                    }
                }
                else -> {
                    item { Muted(stringResource(R.string.ret_pick_purchase), 13.sp) }
                    if (!canPurchase) item { Muted(stringResource(R.string.not_allowed), 14.sp) }
                    else items(purchases, key = { "p" + it.id }) { p ->
                        DocCard(Icons.Outlined.LocalShipping, p.number, listOfNotNull(p.supplierName, fmt.format(Date(p.createdAt))), p.totalPaisa) {
                            go(Routes.purchaseReturn(p.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocCard(icon: androidx.compose.ui.graphics.vector.ImageVector, number: String, sub: List<String>, total: Long, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(icon, Tints.Orange, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(number, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(sub.joinToString(" · "))
            }
            T(total.taka0(), 14.sp, FontWeight.Bold)
        }
    }
}

@Composable
private fun ReturnRowCard(r: ReturnRow, fmt: SimpleDateFormat) {
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(if (r.isPurchase) Icons.Outlined.LocalShipping else Icons.Outlined.Replay, if (r.isPurchase) Tints.Orange else Tints.Purple, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(r.number, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(listOfNotNull(r.party, r.fromNumber, fmt.format(Date(r.createdAt))).joinToString(" · "))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(r.totalPaisa.taka0(), 14.sp, FontWeight.Bold)
                if (r.dueCutPaisa > 0) Muted(stringResource(if (r.isPurchase) R.string.ret_debit_short else R.string.ret_credit_short, r.dueCutPaisa.taka0()))
            }
        }
    }
}
