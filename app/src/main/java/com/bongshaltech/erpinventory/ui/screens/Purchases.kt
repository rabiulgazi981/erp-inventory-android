package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.AlertDialog
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
import com.bongshaltech.erpinventory.data.NewPurchase
import com.bongshaltech.erpinventory.data.PurchaseLine
import com.bongshaltech.erpinventory.data.PurchaseResult
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.CustomerTxn
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.PurchaseRow
import com.bongshaltech.erpinventory.data.db.SupplierEntity
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
import com.bongshaltech.erpinventory.ui.components.Initials
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
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
import com.bongshaltech.erpinventory.ui.toPaisa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** Round floating button used at the bottom right of list screens. */
@Composable
private fun Fab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Row(
        modifier.padding(16.dp).height(52.dp)
            .shadow(10.dp, shape, ambientColor = Brand.Orange, spotColor = Brand.Orange).clip(shape)
            .background(Brand.OrangeGrad).clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
        T(text, 15.sp, FontWeight.SemiBold, Color.White, shadow = true)
    }
}

@Composable
fun PurchasesScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PURCHASE)) return NotAllowed(onBack)
    val list by remember { app.repo.purchases.recent(100) }.collectAsStateWithLifecycle(emptyList())
    val month by remember { app.repo.purchases.totalSince(startOfMonth()) }.collectAsStateWithLifecycle(0L)
    val owed by remember { app.repo.suppliers.totalDue() }.collectAsStateWithLifecycle(0L)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.purchases), onBack) {
            HeaderIcon(Icons.Outlined.LocalShipping, stringResource(R.string.suppliers)) { go(Routes.SUPPLIERS) }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(Modifier.weight(1f), padding = 12.dp) {
                            Muted(stringResource(R.string.pur_this_month))
                            T(month.taka0(), 18.sp, FontWeight.Bold)
                        }
                        Card(Modifier.weight(1f), padding = 12.dp, onClick = { go(Routes.SUPPLIERS) }) {
                            Muted(stringResource(R.string.pur_we_owe))
                            T(owed.taka0(), 18.sp, FontWeight.Bold, if (owed > 0) Brand.Red else Ui.c.text)
                        }
                    }
                }
                if (list.isEmpty()) item { Muted(stringResource(R.string.pur_empty), 14.sp) }
                items(list, key = { it.id }) { PurchaseRowCard(it) }
            }
            Fab(stringResource(R.string.pur_new), { go(Routes.PURCHASE_NEW) }, Modifier.align(Alignment.BottomEnd))
        }
    }
}

private fun startOfMonth(): Long = java.time.LocalDate.now().withDayOfMonth(1)
    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
private fun PurchaseRowCard(p: PurchaseRow) {
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(Icons.Outlined.LocalShipping, Tints.Orange, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(p.number, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(listOfNotNull(p.supplierName, fmt.format(Date(p.createdAt)), stringResource(R.string.pur_items, p.items)).joinToString(" · "))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(p.totalPaisa.taka0(), 14.sp, FontWeight.Bold)
                if (p.duePaisa > 0) T(stringResource(R.string.cust_due_amount, p.duePaisa.taka0()), 12.sp, FontWeight.SemiBold, Brand.Red)
            }
        }
    }
}

/** A line being typed on the purchase screen: quantity and buying price stay as text until saved. */
private data class PLine(val product: ProductEntity, val qty: String, val cost: String)

@Composable
fun PurchaseNewScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PURCHASE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()

    var sq by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    val supMatches by remember(sq) { app.repo.suppliers.search(sq.trim(), false) }.collectAsStateWithLifecycle(emptyList())

    var q by remember { mutableStateOf("") }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    val lines = remember { mutableStateListOf<PLine>() }
    var discount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var message by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<PurchaseResult.Saved?>(null) }

    val ready = lines.map { PurchaseLine(it.product, it.qty.toMilli() ?: 0, it.cost.toPaisa() ?: 0) }
    val subtotal = ready.sumOf { it.totalPaisa }
    val discountPaisa = (discount.toPaisa() ?: 0).coerceIn(0, subtotal)
    val total = subtotal - discountPaisa
    val paidPaisa = if (paid.isBlank()) total else paid.toPaisa() ?: 0
    val badLine = ready.any { it.qtyMilli <= 0 }
    val dueNeeds = stringResource(R.string.pur_due_needs_supplier)
    val badQty = stringResource(R.string.pur_bad_qty)

    fun reset() {
        lines.clear(); discount = ""; paid = ""; method = PayMethod.CASH; message = null
        supplier = null; sq = ""; newName = ""; newPhone = ""
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.pur_new), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Panel(stringResource(R.string.pur_panel_supplier), Brand.BlueGrad, PanelColors.Blue) {
                val s = supplier
                if (s != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Initials(s.name, 36.dp, 13.sp)
                        Column(Modifier.weight(1f)) {
                            T(s.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(listOf(s.phone, stringResource(R.string.prev_due, s.duePaisa.taka0())).filter { it.isNotBlank() }.joinToString(" · "))
                        }
                        Icon(Icons.Outlined.Close, stringResource(R.string.pur_change_supplier), tint = Ui.c.muted,
                            modifier = Modifier.size(22.dp).clickable { supplier = null })
                    }
                } else {
                    Input(sq, { sq = it }, placeholder = stringResource(R.string.pur_find_supplier), height = 42.dp)
                    if (sq.isNotBlank()) supMatches.take(5).forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ui.c.soft)
                                .clickable { supplier = m; sq = "" }.padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            T(m.name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                            Muted(m.phone)
                        }
                    }
                    Muted(stringResource(R.string.pur_or_new_supplier))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Input(newName, { newName = it }, Modifier.weight(1f), placeholder = stringResource(R.string.sup_name), height = 42.dp)
                        Input(newPhone, { newPhone = it }, Modifier.weight(1f), placeholder = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone, height = 42.dp)
                    }
                }
            }
            Panel(stringResource(R.string.panel_product), Brand.GreenGrad, PanelColors.Green) {
                Input(q, { q = it }, placeholder = stringResource(R.string.search_product), height = 42.dp)
                if (q.isNotBlank()) {
                    if (matches.isEmpty()) Muted(stringResource(R.string.no_match), 13.sp)
                    matches.take(8).forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ui.c.soft).clickable {
                                if (lines.none { it.product.id == p.id }) {
                                    lines.add(PLine(p, "1", if (p.costPaisa > 0) (p.costPaisa / 100.0).toBigDecimal().stripTrailingZeros().toPlainString() else ""))
                                }
                                q = ""
                            }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                T(p.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                                Muted(stringResource(R.string.stock_in, p.stockMilli.qty()))
                            }
                            Muted(stringResource(R.string.pur_last_cost, p.costPaisa.taka0()))
                        }
                    }
                    T(
                        stringResource(R.string.product_add), 13.sp, FontWeight.SemiBold, Brand.Link,
                        Modifier.clickable { go(Routes.productEdit(0)) }.padding(vertical = 4.dp),
                    )
                }
                if (lines.isEmpty() && q.isBlank()) Muted(stringResource(R.string.pur_cart_empty), 13.sp)
                lines.forEachIndexed { i, l ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Ui.c.border, RoundedCornerShape(12.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T(l.product.name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                            Icon(Icons.Outlined.Close, stringResource(R.string.pur_remove), tint = Ui.c.muted,
                                modifier = Modifier.size(20.dp).clickable { lines.removeAt(i) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            Input(l.qty, { lines[i] = l.copy(qty = it) }, Modifier.weight(1f), label = stringResource(R.string.pur_qty, l.product.unit),
                                keyboard = KeyboardType.Decimal, height = 40.dp)
                            Input(l.cost, { lines[i] = l.copy(cost = it) }, Modifier.weight(1f), label = stringResource(R.string.pur_unit_cost),
                                keyboard = KeyboardType.Decimal, height = 40.dp)
                            T(ready[i].totalPaisa.taka0(), 14.sp, FontWeight.Bold, modifier = Modifier.width(78.dp).padding(bottom = 10.dp),
                                align = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }
                }
            }
            Panel(stringResource(R.string.panel_amount), Brand.OrangeGrad, PanelColors.Orange) {
                SumRow(stringResource(R.string.sale_subtotal), subtotal.taka0())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(stringResource(R.string.sale_discount), 14.sp, color = Ui.c.muted, modifier = Modifier.weight(1f))
                    Input(discount, { discount = it }, Modifier.width(90.dp), keyboard = KeyboardType.Decimal, height = 34.dp, alignEnd = true, placeholder = "0")
                }
                SumRow(stringResource(R.string.sale_total), total.taka(), strong = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (m, label) -> Chip(stringResource(label), method == m, { method = m }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(stringResource(R.string.pur_paid), 14.sp, color = Ui.c.muted, modifier = Modifier.weight(1f))
                    Input(paid, { paid = it }, Modifier.width(110.dp), keyboard = KeyboardType.Decimal, height = 34.dp, alignEnd = true,
                        placeholder = total.taka0().removePrefix("৳ "))
                }
                if (paidPaisa < total) SumRow(stringResource(R.string.pur_due_to_supplier), (total - paidPaisa).taka0(), strong = true, color = Brand.Red)
                message?.let { T(it, 13.sp, FontWeight.SemiBold, Brand.Red) }
            }
        }
        BottomBar {
            GradButton(
                stringResource(R.string.pur_save) + if (lines.isNotEmpty()) " · ${total.taka0()}" else "",
                Brand.OrangeGrad, Modifier.weight(1f), enabled = lines.isNotEmpty(),
            ) {
                if (badLine) { message = badQty; return@GradButton }
                scope.launch {
                    val r = app.repo.savePurchase(
                        NewPurchase(
                            lines = ready, discountPaisa = discountPaisa, paidPaisa = paidPaisa, method = method,
                            supplierId = supplier?.id, supplierName = if (supplier == null) newName else "", supplierPhone = newPhone,
                            staffId = who.staffId,
                        )
                    )
                    when (r) {
                        is PurchaseResult.Saved -> { saved = r; message = null }
                        PurchaseResult.DueNeedsSupplier -> message = dueNeeds
                    }
                }
            }
        }
    }

    saved?.let { s ->
        AlertDialog(
            onDismissRequest = {},
            title = { T(stringResource(R.string.pur_saved, s.number), 18.sp, FontWeight.Bold) },
            text = { T(s.totalPaisa.taka(), 26.sp, FontWeight.Bold, Brand.Orange) },
            confirmButton = { GradButton(stringResource(R.string.pur_new), Brand.OrangeGrad, height = 44.dp) { reset(); saved = null } },
            dismissButton = { WhiteButton(stringResource(R.string.sale_done), height = 44.dp) { saved = null; onBack() } },
            containerColor = Ui.c.card,
        )
    }
}

@Composable
fun SuppliersScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PURCHASE)) return NotAllowed(onBack)
    var q by rememberSaveable { mutableStateOf("") }
    var dueOnly by rememberSaveable { mutableStateOf(false) }
    val list by remember(q, dueOnly) { app.repo.suppliers.search(q.trim(), dueOnly) }.collectAsStateWithLifecycle(emptyList())
    val all by remember { app.repo.suppliers.count() }.collectAsStateWithLifecycle(0)
    val owed by remember { app.repo.suppliers.totalDue() }.collectAsStateWithLifecycle(0L)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.suppliers), onBack)
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Muted(stringResource(R.string.pur_we_owe))
                        T(owed.taka0(), 22.sp, FontWeight.Bold, if (owed > 0) Brand.Red else Ui.c.text)
                    }
                }
                item { SearchBox(q, { q = it }, stringResource(R.string.cust_search)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.chip_all, all), !dueOnly, { dueOnly = false })
                        Chip(stringResource(R.string.sup_owed_only), dueOnly, { dueOnly = true })
                    }
                }
                if (list.isEmpty()) item { Muted(stringResource(if (all == 0) R.string.sup_empty else R.string.cust_no_match), 14.sp) }
                items(list, key = { it.id }) { s ->
                    Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { go(Routes.supplier(s.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Initials(s.name)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                T(s.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                                Muted(s.phone.ifBlank { "—" })
                            }
                            if (s.duePaisa > 0) T(stringResource(R.string.sup_we_owe_amount, s.duePaisa.taka0()), 13.sp, FontWeight.Bold, Brand.Red)
                            else T(stringResource(R.string.cust_no_due), 12.sp, FontWeight.SemiBold, Brand.DarkGreen)
                        }
                    }
                }
            }
            Fab(stringResource(R.string.sup_add), { go(Routes.supplierEdit(0)) }, Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
fun SupplierDetailScreen(id: Long, go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    val scope = rememberCoroutineScope()
    val s by remember(id) { app.repo.suppliers.watch(id) }.collectAsStateWithLifecycle(null)
    val history by remember(id) { app.repo.suppliers.history(id) }.collectAsStateWithLifecycle(emptyList())
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf(PayMethod.CASH) }
    var note by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.sup_paid_saved)
    val tooMuch = stringResource(R.string.cust_amount_too_much)
    val required = stringResource(R.string.required)
    if (who.ready && !who.can(StaffMenu.PURCHASE)) return NotAllowed(onBack)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(s?.name.orEmpty(), onBack) {
            HeaderIcon(Icons.Outlined.Edit, stringResource(R.string.sup_edit)) { go(Routes.supplierEdit(id)) }
        }
        val sup = s ?: return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Initials(sup.name, 52.dp, 18.sp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T(sup.name, 16.sp, FontWeight.Bold, maxLines = 1)
                        Muted(listOf(sup.phone, sup.address).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { "—" })
                    }
                    if (sup.phone.isNotBlank()) Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Brand.GreenGrad)
                            .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sup.phone}"))) } },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Call, stringResource(R.string.cust_call), tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Muted(stringResource(R.string.pur_we_owe), 14.sp, Modifier.weight(1f))
                    T(sup.duePaisa.taka0(), 22.sp, FontWeight.Bold, if (sup.duePaisa > 0) Brand.Red else Brand.DarkGreen)
                }
            }
            if (sup.duePaisa > 0) Panel(stringResource(R.string.sup_pay), Brand.OrangeGrad, PanelColors.Orange) {
                val amountP = amount.toPaisa()
                Input(
                    amount, { amount = it }, label = stringResource(R.string.sup_amount), keyboard = KeyboardType.Decimal,
                    placeholder = sup.duePaisa.taka0(),
                    error = when {
                        tried && (amountP == null || amountP <= 0) -> required
                        amountP != null && amountP > sup.duePaisa -> tooMuch
                        else -> null
                    },
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (m, label) -> Chip(stringResource(label), method == m, { method = m }) }
                }
                Input(note, { note = it }, label = stringResource(R.string.cust_note))
                GradButton(stringResource(R.string.sup_pay_save), Brand.OrangeGrad, Modifier.fillMaxWidth()) {
                    tried = true
                    val p = amount.toPaisa()
                    if (p != null && p > 0 && p <= sup.duePaisa) scope.launch {
                        val number = app.repo.paySupplier(sup.id, p, method, note, who.staffId)
                        if (number != null) {
                            Toast.makeText(ctx, "$savedMsg · $number", Toast.LENGTH_SHORT).show()
                            amount = ""; note = ""; method = PayMethod.CASH; tried = false
                        }
                    }
                }
            }
            T(stringResource(R.string.sup_history), 15.sp, FontWeight.Bold)
            if (history.isEmpty()) Muted(stringResource(R.string.cust_history_empty))
            history.forEach { SupplierTxnRow(it) }
        }
    }
}

@Composable
private fun SupplierTxnRow(t: CustomerTxn) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(stringResource(if (t.isPayment) R.string.sup_txn_paid else R.string.sup_txn_purchase) + " · " + t.number, 13.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(fmt.format(Date(t.createdAt)))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T((if (t.isPayment) "− " else "") + t.amountPaisa.taka0(), 14.sp, FontWeight.Bold, if (t.isPayment) Brand.DarkGreen else Ui.c.text)
                if (!t.isPayment && t.duePaisa > 0) T(stringResource(R.string.cust_due_amount, t.duePaisa.taka0()), 12.sp, FontWeight.SemiBold, Brand.Red)
            }
        }
    }
}

@Composable
fun SupplierEditScreen(id: Long, onDone: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<SupplierEntity?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val required = stringResource(R.string.required)

    LaunchedEffect(id) {
        if (id != 0L) app.repo.suppliers.get(id)?.let { s -> loaded = s; name = s.name; phone = s.phone; address = s.address }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(if (id == 0L) R.string.sup_add else R.string.sup_edit), onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Panel(stringResource(R.string.pur_panel_supplier), Brand.BlueGrad, PanelColors.Blue) {
                Input(name, { name = it }, label = stringResource(R.string.sup_name), error = required.takeIf { tried && name.isBlank() })
                Input(phone, { phone = it.filter(Char::isDigit).take(14) }, label = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone)
                Input(address, { address = it }, label = stringResource(R.string.cust_address))
            }
        }
        BottomBar {
            GradButton(stringResource(R.string.sup_save), Brand.BlueGrad, Modifier.weight(1f), height = 52.dp) {
                tried = true
                if (name.isBlank()) return@GradButton
                val base = loaded ?: SupplierEntity(name = "")
                scope.launch { app.repo.saveSupplier(base.copy(name = name, phone = phone, address = address)); onDone() }
            }
        }
    }
}
