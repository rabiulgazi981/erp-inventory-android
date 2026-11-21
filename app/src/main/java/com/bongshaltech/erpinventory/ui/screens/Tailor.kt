package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.tailor.Tailor
import com.bongshaltech.erpinventory.data.ShopRepo
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.TailorItemEntity
import com.bongshaltech.erpinventory.data.db.TailorOrderRow
import com.bongshaltech.erpinventory.data.db.TailorWorkerEntity
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.DateBox
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.PhoneInput
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.SlipImage
import com.bongshaltech.erpinventory.ui.components.SumRow
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tag
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.components.Toggle
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.components.dayText
import com.bongshaltech.erpinventory.ui.components.softShadow
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toMilli
import com.bongshaltech.erpinventory.ui.toPaisa
import java.time.LocalDate
import kotlinx.coroutines.launch

private fun today() = LocalDate.now().toEpochDay()

private fun TailorOrderRow.late() = deliveryDay != null && deliveryDay < today() && status != "DELIVERED" && status != "CANCELLED"

private fun Long.plain(): String = if (this == 0L) "" else (this / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()

@Composable
private fun TailorStatusTag(status: String, late: Boolean) = when {
    late -> Tag(stringResource(R.string.tl_late), Color(0xFF991B1B), Tints.Pink.brush)
    status == "READY" -> Tag(Tailor.statusLabel(status), Color(0xFF166534), Tints.Green.brush)
    status == "DELIVERED" -> Tag(Tailor.statusLabel(status), Color(0xFF1E3A8A), Tints.Blue.brush)
    status == "CANCELLED" -> Tag(Tailor.statusLabel(status), Ui.c.muted, Tints.Grey.brush)
    else -> Tag(Tailor.statusLabel(status), Color(0xFF92400E), Tints.Amber.brush)
}

/** More › Tailoring: orders with their delivery dates, what is ready and what is late. */
@Composable
fun TailorScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.SALE)) return NotAllowed(onBack)
    val orders by remember { app.repo.tailor.orders() }.collectAsStateWithLifecycle(emptyList())
    val month = remember { LocalDate.now().withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val report by remember { app.repo.tailor.report(month) }.collectAsStateWithLifecycle(null)
    var filter by rememberSaveable { mutableStateOf("OPEN") }
    val shown = orders.filter {
        when (filter) {
            "OPEN" -> it.status != "DELIVERED" && it.status != "CANCELLED"
            "READY" -> it.status == "READY"
            "LATE" -> it.late()
            "DELIVERED" -> it.status == "DELIVERED"
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.tl_title), onBack) {
            HeaderIcon(Icons.Outlined.Groups, stringResource(R.string.tl_workers)) { go(Routes.TAILOR_WORKERS) }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat(stringResource(R.string.tl_open), orders.count { it.status != "DELIVERED" && it.status != "CANCELLED" }.toString(), Modifier.weight(1f))
                        MiniStat(stringResource(R.string.tl_ready), orders.count { it.status == "READY" }.toString(), Modifier.weight(1f), Brand.DarkGreen)
                        MiniStat(stringResource(R.string.tl_late), orders.count { it.late() }.toString(), Modifier.weight(1f), Brand.Red)
                    }
                }
                report?.let { r ->
                    item {
                        Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                            T(stringResource(R.string.tl_month), 14.sp, FontWeight.Bold)
                            SumRow(stringResource(R.string.tl_making), r.makingPaisa.taka())
                            SumRow(stringResource(R.string.tl_fabric_sold), r.fabricPaisa.taka())
                            SumRow(stringResource(R.string.tl_labour), "− " + r.labourPaisa.taka())
                            SumRow(stringResource(R.string.tl_margin), (r.makingPaisa - r.labourPaisa).taka(), strong = true, color = Brand.DarkGreen)
                        }
                    }
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OPEN" to R.string.tl_open, "READY" to R.string.tl_ready, "LATE" to R.string.tl_late, "DELIVERED" to R.string.tl_delivered, "ALL" to R.string.tl_all)
                            .forEach { (k, l) -> Chip(stringResource(l), filter == k, { filter = k }) }
                    }
                }
                if (shown.isEmpty()) item { Muted(stringResource(R.string.tl_none), 14.sp) }
                items(shown, key = { it.id }) { o -> TailorOrderCard(o) { go(Routes.tailorOrder(o.id)) } }
            }
            Fab(stringResource(R.string.tl_new), Brand.PurpleGrad, Modifier.align(Alignment.BottomEnd)) { go(Routes.TAILOR_NEW) }
        }
    }
}

@Composable
internal fun MiniStat(title: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Card(modifier, padding = 12.dp) {
        Muted(title)
        T(value, 20.sp, FontWeight.Bold, color)
    }
}

@Composable
private fun TailorOrderCard(o: TailorOrderRow, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(Icons.Outlined.ContentCut, Tints.Purple, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T((o.customerName ?: "—") + " · " + o.number, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(
                    listOfNotNull(
                        o.garments?.split(',')?.distinct()?.joinToString(", ") { Tailor.garmentLabel(it).substringBefore(" (") },
                        o.deliveryDay?.let { stringResource(R.string.tl_delivery_on, it.dayText()) },
                    ).joinToString(" · ")
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TailorStatusTag(o.status, o.late())
                val due = (o.totalPaisa - o.paidPaisa).coerceAtLeast(0)
                if (due > 0 && o.status != "DELIVERED") T(stringResource(R.string.cust_due_amount, due.taka0()), 12.sp, FontWeight.SemiBold, Brand.Red)
            }
        }
    }
}

/** A round gradient "+ New" button that floats over a list. */
@Composable
internal fun Fab(text: String, grad: androidx.compose.ui.graphics.Brush, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier.padding(16.dp).softShadow(shape, 10.dp).clip(shape).background(grad).clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
        T(text, 15.sp, FontWeight.SemiBold, Color.White)
    }
}

private class ItemDraft(garment: String) {
    var garment by mutableStateOf(garment)
    var name by mutableStateOf("")
    var qty by mutableStateOf("1")
    var making by mutableStateOf("")
    val values = mutableStateMapOf<String, String>()
    var style by mutableStateOf("")
    var save by mutableStateOf(true)
    var fabric by mutableStateOf<ProductEntity?>(null)
    var fabricQty by mutableStateOf("")
    var fabricPrice by mutableStateOf("")
    val qtyN get() = qty.trim().toIntOrNull() ?: 0
    val makingP get() = making.toPaisa() ?: 0
    val fabricM get() = fabricQty.toMilli() ?: 0
    val fabricP get() = fabricPrice.toPaisa() ?: 0
    val total get() = makingP * qtyN + (if (fabric != null) (fabricM * fabricP + 500) / 1000 else 0)
}

/** New tailoring order: who it is for, each piece with its measurements, the making charge, advance and delivery date. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TailorNewScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.SALE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var phone by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val drafts = remember { mutableStateListOf<ItemDraft>() }
    var discount by rememberSaveable { mutableStateOf("") }
    var advance by rememberSaveable { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var day by rememberSaveable { mutableStateOf<Long?>(today() + 7) }
    var note by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val digits = phone.filter(Char::isDigit)
    val customer by produceState<com.bongshaltech.erpinventory.data.db.CustomerEntity?>(null, digits) {
        value = if (digits.length >= 11) app.repo.customers.byPhone(digits) else null
    }
    val saved by produceState(emptyList<com.bongshaltech.erpinventory.data.db.TailorMeasureEntity>(), customer?.id) {
        value = customer?.id?.let { app.repo.tailor.measures(it) } ?: emptyList()
    }
    LaunchedEffect(customer?.id) { customer?.let { if (name.isBlank()) name = it.name } }
    fun fill(d: ItemDraft) {
        saved.firstOrNull { it.garment == d.garment }?.let { m ->
            Tailor.decode(m.measurements).forEach { (k, v) -> if (d.values[k].isNullOrBlank()) d.values[k] = v }
        }
    }
    LaunchedEffect(saved) { drafts.forEach { fill(it) } }
    val subtotal = drafts.sumOf { it.total }
    val disc = (discount.toPaisa() ?: 0).coerceIn(0, subtotal)
    val total = subtotal - disc
    val adv = (advance.toPaisa() ?: 0).coerceAtLeast(0)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.tl_new), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Panel(stringResource(R.string.tl_customer), Brand.BlueGrad, PanelColors.Blue) {
                    PhoneInput(phone, { phone = it }, label = stringResource(R.string.mobile), placeholder = "01XXXXXXXXX", onName = { if (name.isBlank()) name = it })
                    Input(name, { name = it }, label = stringResource(R.string.staff_name))
                    if (customer != null) Muted(
                        if (saved.isEmpty()) stringResource(R.string.tl_old_customer)
                        else stringResource(R.string.tl_saved_sizes, saved.joinToString(", ") { Tailor.garmentLabel(it.garment).substringBefore(" (") })
                    )
                }
            }
            drafts.forEachIndexed { i, d -> item(key = "item$i") { ItemCard(d, onRemove = { drafts.removeAt(i) }) } }
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    T(stringResource(R.string.tl_add_piece), 14.sp, FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tailor.GARMENTS.forEach { g ->
                            Chip(g.label.substringBefore(" ("), false, { drafts.add(ItemDraft(g.key).also { fill(it) }) })
                        }
                    }
                }
            }
            if (drafts.isNotEmpty()) item {
                Panel(stringResource(R.string.tl_bill), Brand.GreenGrad, PanelColors.Green) {
                    SumRow(stringResource(R.string.sale_subtotal), subtotal.taka())
                    Input(discount, { discount = it }, label = stringResource(R.string.tl_discount), keyboard = KeyboardType.Decimal, placeholder = "0")
                    SumRow(stringResource(R.string.tl_total), total.taka(), strong = true)
                    Input(advance, { advance = it }, label = stringResource(R.string.tl_advance), keyboard = KeyboardType.Decimal, placeholder = "0")
                    if (adv > 0) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        methods.forEach { (m, l) -> Chip(stringResource(l), method == m, { method = m }) }
                    }
                    if (total - adv > 0) SumRow(stringResource(R.string.tl_due_later), (total - adv).taka(), color = Brand.Red)
                    DateBox(day, { day = it }, stringResource(R.string.tl_delivery))
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                }
            }
            message?.let { item { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) } }
        }
        BottomBar {
            GradButton(stringResource(R.string.tl_save), Brand.PurpleGrad, Modifier.weight(1f), enabled = drafts.isNotEmpty() && !busy) {
                busy = true
                scope.launch {
                    val r = app.repo.saveTailorOrder(
                        ShopRepo.NewTailorOrder(
                            phone, name, drafts.map {
                                ShopRepo.TailorLine(it.garment, it.name, it.qtyN, it.makingP, it.values.filterValues { v -> v.isNotBlank() }, it.style, it.save,
                                    it.fabric, it.fabricM, it.fabricP)
                            }, disc, adv.coerceAtMost(total), method, day, note, who.staffId,
                        )
                    )
                    busy = false
                    message = when (r) {
                        is ShopRepo.TailorResult.Saved -> { onBack(); go(Routes.tailorOrder(r.orderId)); null }
                        ShopRepo.TailorResult.NeedsCustomer -> ctx.getString(R.string.tl_need_phone)
                        ShopRepo.TailorResult.Invalid -> ctx.getString(R.string.tl_invalid)
                        is ShopRepo.TailorResult.NotEnoughStock -> ctx.getString(R.string.sale_not_enough, r.name)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemCard(d: ItemDraft, onRemove: () -> Unit) {
    val g = Tailor.garment(d.garment)
    Card(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TintIcon(Icons.Outlined.ContentCut, Tints.Purple, 36.dp, 18.dp, 10.dp)
            T(g.label, 15.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
            T("✕", 16.sp, color = Ui.c.muted, modifier = Modifier.clickable(onClick = onRemove).padding(6.dp))
        }
        if (d.garment == "OTHER") Input(d.name, { d.name = it }, label = stringResource(R.string.tl_other_name), placeholder = "ফতুয়া, শেরওয়ানি…")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Input(d.qty, { d.qty = it }, Modifier.weight(1f), label = stringResource(R.string.tl_pieces), keyboard = KeyboardType.Number)
            Input(d.making, { d.making = it }, Modifier.weight(1.4f), label = stringResource(R.string.tl_making_each), keyboard = KeyboardType.Decimal, placeholder = "0")
        }
        Muted(stringResource(R.string.tl_measure_inch))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
            g.fields.forEach { f ->
                Input(d.values[f].orEmpty(), { d.values[f] = it }, Modifier.weight(1f), label = Tailor.fieldLabel(f).substringBefore(" ("),
                    keyboard = KeyboardType.Decimal, height = 40.dp)
            }
        }
        Input(d.style, { d.style = it }, label = stringResource(R.string.tl_style), placeholder = stringResource(R.string.tl_style_hint))
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(stringResource(R.string.tl_keep_sizes), 13.sp, modifier = Modifier.weight(1f))
            Toggle(d.save, { d.save = it })
        }
        var shopCloth by remember { mutableStateOf(d.fabric != null) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(stringResource(R.string.tl_shop_cloth), 13.sp, modifier = Modifier.weight(1f))
            Toggle(shopCloth, { shopCloth = it; if (!it) d.fabric = null })
        }
        if (shopCloth) {
            ProductPick(d.fabric, { p -> d.fabric = p; if (p != null) d.fabricPrice = p.pricePaisa.plain() }, stringResource(R.string.tl_cloth))
            if (d.fabric != null) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Input(d.fabricQty, { d.fabricQty = it }, Modifier.weight(1f), label = stringResource(R.string.tl_cloth_qty, d.fabric?.unit ?: ""), keyboard = KeyboardType.Decimal)
                Input(d.fabricPrice, { d.fabricPrice = it }, Modifier.weight(1f), label = stringResource(R.string.tl_cloth_price), keyboard = KeyboardType.Decimal)
            }
        }
        if (d.total > 0) SumRow(stringResource(R.string.tl_line_total), d.total.taka(), strong = true, color = Brand.Purple)
    }
}

/** One tailoring order: status, pieces and measurements, work given to tailors, delivery. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TailorOrderScreen(id: Long, go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.SALE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val o by remember(id) { app.repo.tailor.watchOrder(id) }.collectAsStateWithLifecycle(null)
    val pieces by remember(id) { app.repo.tailor.items(id) }.collectAsStateWithLifecycle(emptyList())
    val tasks by remember(id) { app.repo.tailor.tasks(id) }.collectAsStateWithLifecycle(emptyList())
    val workers by remember { app.repo.tailor.workers() }.collectAsStateWithLifecycle(emptyList())
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    var payNow by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var askCancel by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val order = o ?: run {
        Column(Modifier.fillMaxSize().background(Ui.c.bg)) { Header(stringResource(R.string.tl_title), onBack) }
        return
    }
    val due = (order.totalPaisa - order.paidPaisa).coerceAtLeast(0).coerceAtMost(order.customerDuePaisa.coerceAtLeast(0))
    val closed = order.status == "DELIVERED" || order.status == "CANCELLED"

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(order.number, onBack) {
            HeaderIcon(Icons.Outlined.Share, stringResource(R.string.tl_slip)) {
                val shop = settings?.profile?.name.orEmpty()
                val bmp = SlipImage.render(
                    shop.ifBlank { ctx.getString(R.string.tl_title) },
                    listOfNotNull(order.number, order.customerName, order.customerPhone, order.deliveryDay?.let { ctx.getString(R.string.tl_delivery_on, it.dayText()) }).joinToString(" · "),
                    pieces.map { it ->
                        val m = Tailor.decode(it.measurements)
                        (Tailor.garmentLabel(it.garment, it.garmentName) + " × " + it.qty) to
                            (Tailor.garment(it.garment).fields.filter { f -> !m[f].isNullOrBlank() }.map { f -> Tailor.fieldLabel(f) to m.getValue(f) } +
                                (if (it.style.isNotBlank()) listOf(ctx.getString(R.string.tl_style) to it.style) else emptyList()))
                    },
                    "ERP Inventory · bongshaltech.com",
                )
                shareImage(ctx, bmp, "tailor-${order.number}")
            }
            HeaderIcon(Icons.AutoMirrored.Outlined.ReceiptLong, stringResource(R.string.tl_view_bill)) { go(Routes.receipt(order.saleId)) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(Modifier.fillMaxWidth().softShadow(RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Brand.PurpleGrad).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T(order.customerName ?: "—", 18.sp, FontWeight.Bold, Color.White)
                        T(listOfNotNull(order.customerPhone, order.deliveryDay?.let { ctx.getString(R.string.tl_delivery_on, it.dayText()) }).joinToString(" · "), 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column { T(stringResource(R.string.tl_total), 12.sp, color = Color.White.copy(alpha = 0.8f)); T(order.totalPaisa.taka0(), 17.sp, FontWeight.Bold, Color.White) }
                            Column { T(stringResource(R.string.tl_paid), 12.sp, color = Color.White.copy(alpha = 0.8f)); T(order.paidPaisa.taka0(), 17.sp, FontWeight.Bold, Color.White) }
                            Column { T(stringResource(R.string.tl_due), 12.sp, color = Color.White.copy(alpha = 0.8f)); T(due.taka0(), 17.sp, FontWeight.Bold, Color.White) }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(stringResource(R.string.tl_status), 14.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                        TailorStatusTag(order.status, order.late())
                    }
                    if (!closed) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tailor.WORK_STATUSES.forEach { s -> Chip(Tailor.statusLabel(s), order.status == s, { scope.launch { app.repo.setTailorStatus(order.id, s) } }) }
                    }
                    if (!closed) DateBox(order.deliveryDay, { d -> scope.launch { app.repo.tailor.setDelivery(order.id, d, order.note) } }, stringResource(R.string.tl_delivery))
                    if (order.note.isNotBlank()) Muted(order.note, 13.sp)
                }
            }
            items(pieces, key = { it.id }) { p -> PieceCard(p) }
            item { TaskPanel(order.id, pieces, tasks, workers, closed) }
            if (!closed) item {
                Panel(stringResource(R.string.tl_deliver), Brand.GreenGrad, PanelColors.Green) {
                    if (due > 0) {
                        Input(payNow, { payNow = it }, label = stringResource(R.string.tl_take_now), keyboard = KeyboardType.Decimal, placeholder = due.plain())
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            methods.forEach { (m, l) -> Chip(stringResource(l), method == m, { method = m }) }
                        }
                    } else Muted(stringResource(R.string.tl_all_paid))
                    GradButton(stringResource(R.string.tl_deliver_btn), Brand.GreenGrad, Modifier.fillMaxWidth()) {
                        val pay = if (payNow.isBlank()) due else (payNow.toPaisa() ?: 0).coerceIn(0, due)
                        scope.launch {
                            message = if (app.repo.deliverTailor(order.id, pay, method, who.staffId)) null else ctx.getString(R.string.tl_invalid)
                        }
                    }
                    if (who.owner) WhiteButton(stringResource(R.string.tl_cancel), Modifier.fillMaxWidth()) { askCancel = true }
                }
            }
            message?.let { item { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) } }
        }
    }
    if (askCancel) AlertDialog(
        onDismissRequest = { askCancel = false },
        title = { T(stringResource(R.string.tl_cancel), 17.sp, FontWeight.Bold) },
        text = { T(stringResource(R.string.tl_cancel_ask), 14.sp) },
        confirmButton = {
            T(stringResource(R.string.tl_cancel), 15.sp, FontWeight.SemiBold, Brand.Red, Modifier.clickable {
                askCancel = false
                scope.launch { message = if (app.repo.cancelTailor(order.id, "")) null else ctx.getString(R.string.tl_invalid) }
            }.padding(10.dp))
        },
        dismissButton = { T(stringResource(R.string.back), 15.sp, modifier = Modifier.clickable { askCancel = false }.padding(10.dp)) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PieceCard(it: TailorItemEntity) {
    val m = Tailor.decode(it.measurements)
    Card(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(Tailor.garmentLabel(it.garment, it.garmentName) + " × " + it.qty, 15.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
            T((it.makingPaisa * it.qty).taka0(), 14.sp, FontWeight.SemiBold, Brand.Purple)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Tailor.garment(it.garment).fields.filter { f -> !m[f].isNullOrBlank() }.forEach { f ->
                Tag(Tailor.fieldLabel(f).substringBefore(" (") + " " + m.getValue(f), Color(0xFF4C1D95), Tints.Purple.brush)
            }
        }
        if (it.style.isNotBlank()) Muted(it.style, 13.sp)
        if (it.fabricProductId != null) Muted(stringResource(R.string.tl_cloth) + ": " + it.fabricName + " · " + it.fabricMilli.qty(), 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskPanel(orderId: Long, items: List<TailorItemEntity>, tasks: List<com.bongshaltech.erpinventory.data.db.TailorTaskRow>, workers: List<TailorWorkerEntity>, closed: Boolean) {
    val app = app()
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<Long?>(null) }
    var worker by remember { mutableStateOf<Long?>(null) }
    var task by remember { mutableStateOf("SEWING") }
    var qty by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    val pickedItem = items.firstOrNull { it.id == item } ?: items.firstOrNull()
    val pickedWorker = workers.firstOrNull { it.id == worker && it.active }
    LaunchedEffect(pickedItem?.id, pickedWorker?.id, task) {
        if (pickedItem != null && pickedWorker != null) {
            val r = Tailor.rateFor(Tailor.decodeRates(pickedWorker.rates), pickedItem.garment, task)
            rate = r.plain()
        }
        if (pickedItem != null && qty.isBlank()) qty = pickedItem.qty.toString()
    }
    Panel(stringResource(R.string.tl_work), Brand.OrangeGrad, PanelColors.Orange) {
        tasks.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T((t.workerName ?: "—") + " · " + Tailor.taskLabel(t.task).substringBefore(" ("), 14.sp, FontWeight.SemiBold)
                    Muted(items.firstOrNull { it.id == t.itemId }?.let { Tailor.garmentLabel(it.garment, it.garmentName).substringBefore(" (") }.orEmpty() + " × " + t.qty)
                }
                T(t.amountPaisa.taka0(), 14.sp, FontWeight.SemiBold)
                if (!closed) T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { scope.launch { app.repo.tailor.deleteTask(t.id) } }.padding(6.dp))
            }
        }
        if (tasks.isEmpty()) Muted(stringResource(R.string.tl_no_work))
        if (!closed) {
            if (workers.none { it.active }) Muted(stringResource(R.string.tl_need_worker))
            else {
                if (items.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { i -> Chip(Tailor.garmentLabel(i.garment, i.garmentName).substringBefore(" ("), pickedItem?.id == i.id, { item = i.id; qty = i.qty.toString() }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    workers.filter { it.active }.forEach { w -> Chip(w.name, worker == w.id, { worker = w.id }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tailor.TASKS.forEach { (k, l) -> Chip(l.substringBefore(" ("), task == k, { task = k }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Input(qty, { qty = it }, Modifier.weight(1f), label = stringResource(R.string.tl_pieces), keyboard = KeyboardType.Number)
                    Input(rate, { rate = it }, Modifier.weight(1f), label = stringResource(R.string.tl_rate), keyboard = KeyboardType.Decimal, placeholder = "0")
                }
                GradButton(stringResource(R.string.tl_give_work), Brand.OrangeGrad, Modifier.fillMaxWidth(), enabled = pickedItem != null && pickedWorker != null) {
                    val i = pickedItem ?: return@GradButton
                    val w = pickedWorker ?: return@GradButton
                    scope.launch {
                        if (app.repo.addTailorTask(orderId, i.id, w.id, task, qty.trim().toIntOrNull() ?: 0, rate.toPaisa() ?: 0)) worker = null
                    }
                }
            }
        }
    }
}

/** Tailors who work by the piece: their rates, what they earned, what was paid and what is still owed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TailorWorkersScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.EXPENSE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val workers by remember { app.repo.tailor.workers() }.collectAsStateWithLifecycle(emptyList())
    val balances by remember { app.repo.tailor.balances() }.collectAsStateWithLifecycle(emptyList())
    var open by remember { mutableStateOf<Long?>(null) }
    var edit by remember { mutableStateOf<TailorWorkerEntity?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.tl_workers), onBack)
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Muted(stringResource(R.string.tl_workers_help)) }
                if (adding || edit != null) item { WorkerForm(edit) { adding = false; edit = null } }
                if (workers.isEmpty() && !adding) item { Muted(stringResource(R.string.tl_no_workers), 14.sp) }
                items(workers, key = { it.id }) { w ->
                    val b = balances.firstOrNull { it.workerId == w.id }
                    val owed = Tailor.owed(b?.earnedPaisa ?: 0, b?.paidPaisa ?: 0)
                    WorkerCard(
                        w.name, w.phone.ifBlank { null }, b?.earnedPaisa ?: 0, b?.paidPaisa ?: 0, owed, open == w.id, w.active,
                        onClick = { open = if (open == w.id) null else w.id },
                        onEdit = { edit = w; adding = false },
                        onPay = { advance, amount, m, note -> scope.launch {
                            val n = app.repo.payTailorWorker(w.id, advance, amount, m, note, who.staffId)
                            Toast.makeText(ctx, if (n != null) ctx.getString(R.string.tl_paid_ok, n) else ctx.getString(R.string.cash_bank_bad), Toast.LENGTH_SHORT).show()
                        } },
                    )
                }
            }
            if (!adding && edit == null) Fab(stringResource(R.string.tl_add_worker), Brand.OrangeGrad, Modifier.align(Alignment.BottomEnd)) { adding = true }
        }
    }
}

/** Shared by tailors and garments workers: earnings, payments, and a pay / advance box. */
@Composable
internal fun WorkerCard(
    name: String, sub: String?, earned: Long, paid: Long, owed: Long, open: Boolean, active: Boolean,
    onClick: () -> Unit, onEdit: () -> Unit, onPay: (advance: Boolean, amount: Long, PayMethod, String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var advance by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.bongshaltech.erpinventory.ui.components.Initials(name, 42.dp)
            Column(Modifier.weight(1f)) {
                T(name, 15.sp, FontWeight.SemiBold, if (active) Color.Unspecified else Ui.c.muted, maxLines = 1)
                sub?.let { Muted(it) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Muted(stringResource(if (owed >= 0) R.string.tl_owed else R.string.tl_advance_left))
                T(kotlin.math.abs(owed).taka0(), 15.sp, FontWeight.Bold, if (owed > 0) Brand.Red else Brand.DarkGreen)
            }
        }
        if (open) {
            SumRow(stringResource(R.string.tl_earned), earned.taka())
            SumRow(stringResource(R.string.tl_paid), paid.taka())
            Segmented(listOf(stringResource(R.string.tl_pay_wages), stringResource(R.string.tl_pay_advance)), if (advance) 1 else 0, { advance = it == 1 })
            Input(amount, { amount = it }, label = stringResource(R.string.tl_amount), keyboard = KeyboardType.Decimal, placeholder = if (owed > 0 && !advance) owed.plain() else "0")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { (m, l) -> Chip(stringResource(l), method == m, { method = m }) }
            }
            Input(note, { note = it }, label = stringResource(R.string.cust_note))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WhiteButton(stringResource(R.string.tl_edit), Modifier.weight(1f), height = 46.dp, onClick = onEdit)
                GradButton(stringResource(R.string.tl_pay), Brand.GreenGrad, Modifier.weight(1f), height = 46.dp) {
                    val a = if (amount.isBlank() && !advance) owed.coerceAtLeast(0) else amount.toPaisa() ?: 0
                    if (a > 0) { onPay(advance, a, method, note); amount = ""; note = "" }
                }
            }
        }
    }
}

@Composable
private fun WorkerForm(w: TailorWorkerEntity?, onDone: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var name by remember(w) { mutableStateOf(w?.name.orEmpty()) }
    var phone by remember(w) { mutableStateOf(w?.phone.orEmpty()) }
    var active by remember(w) { mutableStateOf(w?.active ?: true) }
    val rates = remember(w) {
        mutableStateMapOf<String, String>().apply {
            Tailor.decodeRates(w?.rates.orEmpty()).forEach { put(it.garment + "|" + it.task, it.paisa.plain()) }
        }
    }
    Panel(stringResource(if (w == null) R.string.tl_add_worker else R.string.tl_edit), Brand.OrangeGrad, PanelColors.Orange) {
        Input(name, { name = it }, label = stringResource(R.string.staff_name))
        PhoneInput(phone, { phone = it }, label = stringResource(R.string.mobile), onName = { if (name.isBlank()) name = it })
        T(stringResource(R.string.tl_rates), 14.sp, FontWeight.Bold)
        Muted(stringResource(R.string.tl_rates_help))
        Row {
            Spacer(Modifier.weight(1.3f))
            Tailor.TASKS.forEach { (_, l) -> Muted(l.substringBefore(" ("), modifier = Modifier.weight(1f), align = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Tailor.GARMENTS.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                T(g.label.substringBefore(" ("), 13.sp, modifier = Modifier.weight(1.3f), maxLines = 1)
                Tailor.TASKS.forEach { (t, _) ->
                    val k = g.key + "|" + t
                    Input(rates[k].orEmpty(), { rates[k] = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal, height = 38.dp, alignEnd = true, placeholder = "0")
                }
            }
        }
        if (w != null) Row(verticalAlignment = Alignment.CenterVertically) {
            T(stringResource(R.string.tl_active), 14.sp, modifier = Modifier.weight(1f))
            Toggle(active, { active = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WhiteButton(stringResource(R.string.back), Modifier.weight(1f), onClick = onDone)
            GradButton(stringResource(R.string.ord_save), Brand.GreenGrad, Modifier.weight(1f), enabled = name.isNotBlank()) {
                val list = rates.mapNotNull { (k, v) ->
                    val p = v.toPaisa() ?: return@mapNotNull null
                    if (p <= 0) return@mapNotNull null
                    Tailor.Rate(k.substringBefore('|'), k.substringAfter('|'), p)
                }
                scope.launch {
                    app.repo.tailor.saveWorker((w ?: TailorWorkerEntity(name = "")).copy(name = name.trim(), phone = phone.filter(Char::isDigit), rates = Tailor.encodeRates(list), active = active))
                    onDone()
                }
            }
        }
    }
}
