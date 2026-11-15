package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.CartLine
import com.bongshaltech.erpinventory.data.NewOrder
import com.bongshaltech.erpinventory.data.OrderLine
import com.bongshaltech.erpinventory.data.PendingDoc
import com.bongshaltech.erpinventory.data.PurchaseLine
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.OrderDocEntity
import com.bongshaltech.erpinventory.data.db.OrderKind
import com.bongshaltech.erpinventory.data.db.OrderStatus
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
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
import com.bongshaltech.erpinventory.ui.toPaisa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val kindTitle = mapOf(
    OrderKind.QUOTE to R.string.ord_quote, OrderKind.SALES_ORDER to R.string.ord_sales_order, OrderKind.PURCHASE_ORDER to R.string.ord_purchase_order,
)

private fun Long.plain(): String = (this / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()

private fun canUse(who: Who, kind: OrderKind) = who.can(if (kind == OrderKind.PURCHASE_ORDER) StaffMenu.PURCHASE else StaffMenu.SALE)

/** More › Quotes & orders: quotations, sales orders and purchase orders. */
@Composable
fun OrdersScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    var tab by remember { mutableStateOf(0) }
    val kind = OrderKind.entries[tab]
    val list by remember(kind) { app.repo.orders.list(kind.name) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.orders), onBack)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Segmented(OrderKind.entries.map { stringResource(kindTitle.getValue(it)) }, tab, { tab = it })
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!canUse(who, kind)) item { Muted(stringResource(R.string.not_allowed), 14.sp) }
                else {
                    if (list.isEmpty()) item { Muted(stringResource(R.string.ord_empty), 14.sp) }
                    items(list, key = { it.id }) { d -> OrderCard(d, fmt) { go(Routes.order(kind, d.id)) } }
                }
            }
            if (canUse(who, kind)) Box(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).height(52.dp).clip(RoundedCornerShape(26.dp))
                    .background(Brand.BlueGrad).clickable { go(Routes.order(kind, 0)) }.padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { T("+ " + stringResource(R.string.ord_new, stringResource(kindTitle.getValue(kind))), 15.sp, FontWeight.Bold, androidx.compose.ui.graphics.Color.White) }
        }
    }
}

@Composable
private fun OrderCard(d: OrderDocEntity, fmt: SimpleDateFormat, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(Icons.Outlined.Description, if (d.status == OrderStatus.OPEN.name) Tints.Blue else Tints.Grey, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(d.number, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(listOfNotNull(d.partyName.ifBlank { null }, fmt.format(Date(d.createdAt)), statusLabel(d)).joinToString(" · "))
            }
            T(d.totalPaisa.taka0(), 14.sp, FontWeight.Bold)
        }
    }
}

@Composable
private fun statusLabel(d: OrderDocEntity): String? = when (d.status) {
    OrderStatus.DONE.name -> stringResource(R.string.ord_done_as, d.doneAs)
    OrderStatus.CANCELLED.name -> stringResource(R.string.ord_cancelled)
    else -> null
}

private data class OLine(val product: ProductEntity, val qty: String, val price: String)

/** Make or edit a quote / order, then share it or turn it into a sale or purchase. */
@Composable
fun OrderScreen(kind: OrderKind, id: Long, go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !canUse(who, kind)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val po = kind == OrderKind.PURCHASE_ORDER
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)

    var docId by remember { mutableStateOf(id) }
    var doc by remember { mutableStateOf<OrderDocEntity?>(null) }
    val lines = remember { mutableStateListOf<OLine>() }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var supplierId by remember { mutableStateOf<Long?>(null) }
    var discount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    val supMatches by remember(name, supplierId) {
        if (!po || supplierId != null || name.trim().length < 2) kotlinx.coroutines.flow.flowOf(emptyList())
        else app.repo.suppliers.search(name.trim(), false)
    }.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(id) {
        if (id == 0L) return@LaunchedEffect
        val d = app.repo.orders.get(id) ?: return@LaunchedEffect
        doc = d; name = d.partyName; phone = d.partyPhone; supplierId = d.supplierId; note = d.note
        discount = if (d.discountPaisa > 0) d.discountPaisa.plain() else ""
        lines.clear()
        app.repo.orders.items(id).forEach { i ->
            val p = app.repo.products.get(i.productId) ?: ProductEntity(id = i.productId, name = i.name, pricePaisa = i.unitPricePaisa)
            lines.add(OLine(p, i.qtyMilli.qty(), i.unitPricePaisa.plain()))
        }
    }

    val open = doc == null || doc?.status == OrderStatus.OPEN.name
    val ready = lines.map { OrderLine(it.product, it.qty.toMilli() ?: 0, it.price.toPaisa() ?: 0) }
    val subtotal = ready.sumOf { it.totalPaisa }
    val discountPaisa = (discount.toPaisa() ?: 0).coerceIn(0, subtotal)
    val total = subtotal - discountPaisa
    val valid = ready.isNotEmpty() && ready.none { it.qtyMilli <= 0 }

    suspend fun save(): Boolean {
        if (!valid) { message = ctx.getString(R.string.pur_bad_qty); return false }
        val r = app.repo.saveOrder(NewOrder(kind, docId, ready, discountPaisa, name, phone, supplierId, note, who.staffId)) ?: return false
        docId = r.first; doc = app.repo.orders.get(r.first); message = null
        return true
    }

    fun share() {
        val f = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val text = buildString {
            settings?.profile?.name?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            appendLine(ctx.getString(kindTitle.getValue(kind)) + " " + (doc?.number ?: ""))
            appendLine(f.format(Date(doc?.createdAt ?: System.currentTimeMillis())))
            if (name.isNotBlank()) appendLine(name)
            appendLine()
            ready.forEach { appendLine("${it.product.name}  ${it.qtyMilli.qty()} × ${it.unitPricePaisa.taka()} = ${it.totalPaisa.taka()}") }
            appendLine()
            if (discountPaisa > 0) appendLine(ctx.getString(R.string.rc_discount) + ": " + discountPaisa.taka())
            appendLine(ctx.getString(R.string.rc_total) + ": " + total.taka())
            if (note.isNotBlank()) { appendLine(); appendLine(note) }
        }
        runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)) }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(kindTitle.getValue(kind)) + (doc?.number?.let { " · $it" } ?: ""), onBack) {
            if (lines.isNotEmpty()) HeaderIcon(Icons.Outlined.Share, stringResource(R.string.receipt_share)) {
                if (open) scope.launch { if (save()) share() } else share()
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            doc?.let { d -> statusLabel(d)?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.DarkGreen) } }
            Panel(stringResource(if (po) R.string.pur_panel_supplier else R.string.ord_customer), Brand.BlueGrad, PanelColors.Blue) {
                Input(name, { name = it; supplierId = null }, placeholder = stringResource(if (po) R.string.sup_name else R.string.sale_customer_name), height = 42.dp)
                supMatches.take(5).forEach { s ->
                    Muted(s.name + if (s.phone.isNotBlank()) " · ${s.phone}" else "", 14.sp,
                        Modifier.fillMaxWidth().clickable { supplierId = s.id; name = s.name; phone = s.phone }.padding(vertical = 6.dp))
                }
                Input(phone, { phone = it }, placeholder = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone, height = 42.dp)
            }
            Panel(stringResource(R.string.ord_items), Brand.GreenGrad, PanelColors.Green) {
                if (open) {
                    Input(q, { q = it }, placeholder = stringResource(R.string.search_product), height = 42.dp)
                    if (q.isNotBlank()) matches.take(6).forEach { p ->
                        Muted(p.name + " · " + (if (po) p.costPaisa else p.pricePaisa).taka(), 14.sp,
                            Modifier.fillMaxWidth().clickable {
                                lines.add(OLine(p, "1", (if (po) p.costPaisa else p.pricePaisa).plain())); q = ""
                            }.padding(vertical = 6.dp))
                    }
                }
                lines.forEachIndexed { i, l ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        T(l.product.name, 14.sp, FontWeight.SemiBold, Modifier.weight(1f), maxLines = 2)
                        Input(l.qty, { lines[i] = l.copy(qty = it) }, Modifier.width(64.dp), keyboard = KeyboardType.Decimal, alignEnd = true, height = 40.dp)
                        Input(l.price, { lines[i] = l.copy(price = it) }, Modifier.width(84.dp), keyboard = KeyboardType.Decimal, alignEnd = true, height = 40.dp)
                        if (open) Icon(Icons.Outlined.Close, null, tint = Ui.c.muted, modifier = Modifier.size(22.dp).clickable { lines.removeAt(i) })
                    }
                }
                if (lines.isEmpty()) Muted(stringResource(R.string.ord_add_items))
            }
            Panel(stringResource(R.string.rc_total), Brand.OrangeGrad, PanelColors.Orange) {
                SumRow(stringResource(R.string.rc_subtotal), subtotal.taka())
                Input(discount, { discount = it }, label = stringResource(R.string.rc_discount), keyboard = KeyboardType.Decimal, height = 42.dp)
                SumRow(stringResource(R.string.rc_total), total.taka(), strong = true)
                Input(note, { note = it }, label = stringResource(R.string.ord_note))
            }
            message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
            if (open && doc != null) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (kind == OrderKind.QUOTE) Chip(stringResource(R.string.ord_to_sales_order), false, {
                    scope.launch { if (save()) app.repo.quoteToOrder(docId, who.staffId)?.let { go(Routes.order(OrderKind.SALES_ORDER, it)) } }
                })
                Chip(stringResource(R.string.ord_cancel), false, { scope.launch { app.repo.cancelOrder(docId); onBack() } })
            }
        }
        if (open) BottomBar {
            WhiteButton(stringResource(R.string.ord_save)) { scope.launch { if (save()) message = ctx.getString(R.string.ord_saved) } }
            GradButton(stringResource(if (po) R.string.ord_receive else R.string.ord_sell), Brand.BlueGrad, Modifier.weight(1f), enabled = valid) {
                scope.launch {
                    if (!save()) return@launch
                    if (po) {
                        PendingDoc.purchase = PendingDoc.ForPurchase(docId, ready.map { PurchaseLine(it.product, it.qtyMilli, it.unitPricePaisa) }, supplierId, name, phone, discountPaisa)
                        go(Routes.PURCHASE_NEW)
                    } else {
                        PendingDoc.sale = PendingDoc.ForSale(docId, ready.map { CartLine(it.product.copy(pricePaisa = it.unitPricePaisa), it.qtyMilli) }, phone, name, discountPaisa)
                        go(Routes.SALE)
                    }
                }
            }
        }
    }
}
