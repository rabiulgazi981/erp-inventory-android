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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.garment.Garments
import com.bongshaltech.erpinventory.data.SaleResult
import com.bongshaltech.erpinventory.data.ShopRepo
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.GCellEntity
import com.bongshaltech.erpinventory.data.db.GLineEntity
import com.bongshaltech.erpinventory.data.db.GOrderRow
import com.bongshaltech.erpinventory.data.db.GStyleEntity
import com.bongshaltech.erpinventory.data.db.GWorkerEntity
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.DateBox
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.PhoneInput
import com.bongshaltech.erpinventory.ui.components.Segmented
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
import java.time.ZoneId
import kotlinx.coroutines.launch

private fun gToday() = LocalDate.now().toEpochDay()

private fun Long.plainG(): String = if (this == 0L) "" else (this / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()

private fun String.num(): Long = trim().toLongOrNull()?.coerceAtLeast(0) ?: 0

@Composable
private fun GStatusTag(status: String) = when (status) {
    "READY" -> Tag(Garments.statusLabel(status), Color(0xFF166534), Tints.Green.brush)
    "IN_PRODUCTION" -> Tag(Garments.statusLabel(status), Color(0xFF1E3A8A), Tints.Blue.brush)
    "DELIVERED" -> Tag(Garments.statusLabel(status), Color(0xFF115E59), Tints.Teal.brush)
    "CLOSED", "CANCELLED" -> Tag(Garments.statusLabel(status), Ui.c.muted, Tints.Grey.brush)
    else -> Tag(Garments.statusLabel(status), Color(0xFF92400E), Tints.Amber.brush)
}

/** A rounded progress bar in a gradient. */
@Composable
internal fun Progress(pct: Int, grad: Brush = Brand.BlueGrad, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Ui.c.soft)) {
        if (pct > 0) Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(grad))
    }
}

/** More › Garments: production orders, styles with costing, the floor (lines and workers) and the month's report. */
@Composable
fun GarmentsScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(0) }
    val orders by remember { app.repo.garments.orders() }.collectAsStateWithLifecycle(emptyList())
    val styles by remember { app.repo.garments.styles() }.collectAsStateWithLifecycle(emptyList())
    val lines by remember { app.repo.garments.lines() }.collectAsStateWithLifecycle(emptyList())
    val workers by remember { app.repo.garments.workers() }.collectAsStateWithLifecycle(emptyList())
    val balances by remember { app.repo.garments.balances() }.collectAsStateWithLifecycle(emptyList())
    val today by remember { app.repo.garments.lineDay(gToday()) }.collectAsStateWithLifecycle(emptyList())
    val month = remember { LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val report by remember { app.repo.garments.report(month) }.collectAsStateWithLifecycle(null)
    val output by remember { app.repo.garments.outputSince(month) }.collectAsStateWithLifecycle(null)
    var openWorker by remember { mutableStateOf<Long?>(null) }
    var editLine by remember { mutableStateOf<GLineEntity?>(null) }
    var editWorker by remember { mutableStateOf<GWorkerEntity?>(null) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.gm_title), onBack)
        Box(Modifier.fillMaxWidth().background(Ui.c.card).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Segmented(listOf(stringResource(R.string.gm_orders), stringResource(R.string.gm_styles), stringResource(R.string.gm_floor), stringResource(R.string.gm_report)), tab, { tab = it })
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (tab) {
                    0 -> {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MiniStat(stringResource(R.string.gm_running), orders.count { it.status in Garments.OPEN }.toString(), Modifier.weight(1f), Brand.Blue)
                                MiniStat(stringResource(R.string.gm_pieces_left), orders.filter { it.status in Garments.OPEN }.sumOf { it.ordered - it.packed }.toString(), Modifier.weight(1f), Brand.Amber)
                                MiniStat(stringResource(R.string.gm_ready_send), orders.sumOf { (it.packed - it.delivered).coerceAtLeast(0) }.toString(), Modifier.weight(1f), Brand.DarkGreen)
                            }
                        }
                        if (styles.isEmpty()) item { Muted(stringResource(R.string.gm_need_style), 14.sp) }
                        else if (orders.isEmpty()) item { Muted(stringResource(R.string.gm_no_orders), 14.sp) }
                        items(orders, key = { it.id }) { o -> GOrderCard(o) { go(Routes.garmentOrder(o.id)) } }
                    }
                    1 -> {
                        if (styles.isEmpty()) item { Muted(stringResource(R.string.gm_no_styles), 14.sp) }
                        items(styles, key = { it.id }) { s ->
                            Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { go(Routes.garmentStyle(s.id)) }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TintIcon(Icons.Outlined.Checkroom, Tints.Pink, 40.dp, 20.dp, 10.dp)
                                    Column(Modifier.weight(1f)) {
                                        T(s.code + " · " + s.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                                        Muted(s.sizes + " · " + s.colors)
                                    }
                                    T(s.pricePaisa.taka0(), 14.sp, FontWeight.Bold)
                                }
                            }
                        }
                    }
                    2 -> {
                        item { T(stringResource(R.string.gm_lines_today), 15.sp, FontWeight.Bold) }
                        if (lines.isEmpty()) item { Muted(stringResource(R.string.gm_no_lines)) }
                        items(lines, key = { "l" + it.id }) { l ->
                            val done = today.firstOrNull { it.lineId == l.id }?.qty ?: 0
                            Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { editLine = l }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        T(l.name, 14.sp, FontWeight.SemiBold, if (l.active) Color.Unspecified else Ui.c.muted)
                                        Muted(listOf(l.supervisor, stringResource(R.string.gm_workers_n, l.workers)).filter { it.isNotBlank() }.joinToString(" · "))
                                    }
                                    T("$done / ${l.dailyTarget}", 15.sp, FontWeight.Bold, if (l.dailyTarget > 0 && done >= l.dailyTarget) Brand.DarkGreen else Brand.Blue)
                                }
                                Progress(Garments.pct(done, l.dailyTarget), if (l.dailyTarget > 0 && done >= l.dailyTarget) Brand.GreenGrad else Brand.BlueGrad)
                            }
                        }
                        item { LineForm(editLine) { editLine = null } }
                        item { T(stringResource(R.string.gm_workers), 15.sp, FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                        items(workers, key = { "w" + it.id }) { w ->
                            val b = balances.firstOrNull { it.workerId == w.id }
                            WorkerCard(
                                w.name, listOfNotNull(w.role.ifBlank { null }, lines.firstOrNull { it.id == w.lineId }?.name).joinToString(" · ").ifBlank { null },
                                b?.earnedPaisa ?: 0, b?.paidPaisa ?: 0, (b?.earnedPaisa ?: 0) - (b?.paidPaisa ?: 0), openWorker == w.id, w.active,
                                onClick = { openWorker = if (openWorker == w.id) null else w.id },
                                onEdit = { editWorker = w },
                                onPay = { advance, amount, m, note -> scope.launch {
                                    val n = app.repo.payGarmentWorker(w.id, advance, amount, m, note, who.staffId)
                                    Toast.makeText(ctx, if (n != null) ctx.getString(R.string.tl_paid_ok, n) else ctx.getString(R.string.cash_bank_bad), Toast.LENGTH_SHORT).show()
                                } },
                            )
                        }
                        item { GWorkerForm(editWorker, lines) { editWorker = null } }
                    }
                    else -> {
                        report?.let { r ->
                            item {
                                Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                                    T(stringResource(R.string.tl_month), 15.sp, FontWeight.Bold)
                                    SumRow(stringResource(R.string.gm_r_orders), r.orders.toString())
                                    SumRow(stringResource(R.string.gm_r_open), r.open.toString())
                                    SumRow(stringResource(R.string.gm_r_pieces), r.pieces.toString())
                                    SumRow(stringResource(R.string.gm_r_packed), r.packed.toString())
                                    SumRow(stringResource(R.string.gm_r_delivered), r.delivered.toString())
                                    SumRow(stringResource(R.string.gm_r_sales), r.deliveredPaisa.taka(), strong = true, color = Brand.DarkGreen)
                                    SumRow(stringResource(R.string.gm_r_wip), r.wipPaisa.taka())
                                    if (r.writeOffPaisa > 0) SumRow(stringResource(R.string.gm_r_writeoff), r.writeOffPaisa.taka(), color = Brand.Red)
                                }
                            }
                        }
                        output?.let { s ->
                            item {
                                Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                                    T(stringResource(R.string.gm_r_output), 15.sp, FontWeight.Bold)
                                    SumRow(stringResource(R.string.gm_r_done), s.qty.toString())
                                    SumRow(stringResource(R.string.gm_r_rejects), s.rejects.toString(), color = if (s.rejects > 0) Brand.Red else Color.Unspecified)
                                    if (s.qty + s.rejects > 0) SumRow(stringResource(R.string.gm_r_reject_rate), "${s.rejects * 1000 / (s.qty + s.rejects) / 10.0}%")
                                    SumRow(stringResource(R.string.gm_r_piece_pay), s.amountPaisa.taka(), strong = true)
                                }
                            }
                        }
                    }
                }
            }
            when (tab) {
                0 -> if (styles.isNotEmpty()) Fab(stringResource(R.string.gm_new_order), Brand.BlueGrad, Modifier.align(Alignment.BottomEnd)) { go(Routes.GARMENT_ORDER_NEW) }
                1 -> Fab(stringResource(R.string.gm_new_style), Brand.PinkGrad, Modifier.align(Alignment.BottomEnd)) { go(Routes.garmentStyle(0)) }
            }
        }
    }
}

@Composable
private fun GOrderCard(o: GOrderRow, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(Icons.Outlined.Checkroom, Tints.Indigo, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f)) {
                T(o.number + " · " + (o.styleCode ?: "") + " " + (o.styleName ?: ""), 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(listOfNotNull(o.buyerName ?: stringResource(R.string.gm_own_stock), o.buyerPo.ifBlank { null }, o.deliveryDay?.let { stringResource(R.string.tl_delivery_on, it.dayText()) }).joinToString(" · "))
            }
            GStatusTag(o.status)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Progress(Garments.pct(o.packed, o.ordered), Brand.BlueGrad, Modifier.weight(1f))
            Muted(stringResource(R.string.gm_packed_of, o.packed, o.ordered))
        }
    }
}

@Composable
private fun LineForm(l: GLineEntity?, onDone: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var name by remember(l) { mutableStateOf(l?.name.orEmpty()) }
    var sup by remember(l) { mutableStateOf(l?.supervisor.orEmpty()) }
    var target by remember(l) { mutableStateOf(l?.dailyTarget?.takeIf { it > 0 }?.toString().orEmpty()) }
    var count by remember(l) { mutableStateOf(l?.workers?.takeIf { it > 0 }?.toString().orEmpty()) }
    var active by remember(l) { mutableStateOf(l?.active ?: true) }
    Panel(stringResource(if (l == null) R.string.gm_add_line else R.string.gm_edit_line), Brand.TealGrad, PanelColors.Blue) {
        Input(name, { name = it }, label = stringResource(R.string.gm_line_name), placeholder = "Line 1")
        Input(sup, { sup = it }, label = stringResource(R.string.gm_supervisor))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Input(target, { target = it }, Modifier.weight(1f), label = stringResource(R.string.gm_target), keyboard = KeyboardType.Number)
            Input(count, { count = it }, Modifier.weight(1f), label = stringResource(R.string.gm_worker_count), keyboard = KeyboardType.Number)
        }
        if (l != null) Row(verticalAlignment = Alignment.CenterVertically) {
            T(stringResource(R.string.tl_active), 14.sp, modifier = Modifier.weight(1f)); Toggle(active, { active = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (l != null) WhiteButton(stringResource(R.string.back), Modifier.weight(1f), height = 46.dp, onClick = onDone)
            GradButton(stringResource(R.string.ord_save), Brand.TealGrad, Modifier.weight(1f), height = 46.dp, enabled = name.isNotBlank()) {
                scope.launch {
                    app.repo.garments.saveLine((l ?: GLineEntity(name = "")).copy(name = name.trim(), supervisor = sup.trim(), dailyTarget = target.num(), workers = count.num().toInt(), active = active))
                    name = ""; sup = ""; target = ""; count = ""; onDone()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GWorkerForm(w: GWorkerEntity?, lines: List<GLineEntity>, onDone: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var name by remember(w) { mutableStateOf(w?.name.orEmpty()) }
    var phone by remember(w) { mutableStateOf(w?.phone.orEmpty()) }
    var role by remember(w) { mutableStateOf(w?.role.orEmpty()) }
    var line by remember(w) { mutableStateOf(w?.lineId) }
    var active by remember(w) { mutableStateOf(w?.active ?: true) }
    Panel(stringResource(if (w == null) R.string.tl_add_worker else R.string.tl_edit), Brand.OrangeGrad, PanelColors.Orange) {
        Input(name, { name = it }, label = stringResource(R.string.staff_name))
        PhoneInput(phone, { phone = it }, label = stringResource(R.string.mobile), onName = { if (name.isBlank()) name = it })
        Input(role, { role = it }, label = stringResource(R.string.gm_role), placeholder = stringResource(R.string.gm_role_hint))
        if (lines.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.filter { it.active }.forEach { l -> Chip(l.name, line == l.id, { line = if (line == l.id) null else l.id }) }
        }
        if (w != null) Row(verticalAlignment = Alignment.CenterVertically) {
            T(stringResource(R.string.tl_active), 14.sp, modifier = Modifier.weight(1f)); Toggle(active, { active = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (w != null) WhiteButton(stringResource(R.string.back), Modifier.weight(1f), height = 46.dp, onClick = onDone)
            GradButton(stringResource(R.string.ord_save), Brand.OrangeGrad, Modifier.weight(1f), height = 46.dp, enabled = name.isNotBlank()) {
                scope.launch {
                    app.repo.garments.saveWorker((w ?: GWorkerEntity(name = "")).copy(name = name.trim(), phone = phone.filter(Char::isDigit), role = role.trim(), lineId = line, active = active))
                    name = ""; phone = ""; role = ""; line = null; onDone()
                }
            }
        }
    }
}

private class MatRow(val product: ProductEntity, perPiece: String, wastage: String) {
    var perPiece by mutableStateOf(perPiece)
    var wastage by mutableStateOf(wastage)
}

private class OpRow(name: String, stage: String, rate: String) {
    var name by mutableStateOf(name)
    var stage by mutableStateOf(stage)
    var rate by mutableStateOf(rate)
}

private class CostRow(label: String, amount: String) {
    var label by mutableStateOf(label)
    var amount by mutableStateOf(amount)
}

/** A style (design): sizes, colours, price, the material and labour per piece, and what one piece costs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GarmentStyleScreen(id: Long, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(id == 0L) }
    var old by remember { mutableStateOf<GStyleEntity?>(null) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val sizes = remember { mutableStateListOf<String>() }
    var extraSize by remember { mutableStateOf("") }
    var colors by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    val mats = remember { mutableStateListOf<MatRow>() }
    val ops = remember { mutableStateListOf<OpRow>() }
    val others = remember { mutableStateListOf<CostRow>() }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(id) {
        if (id == 0L) {
            Garments.OPERATIONS.forEach { ops.add(OpRow(it.name, it.stage, "")) }
            return@LaunchedEffect
        }
        val s = app.repo.garments.style(id) ?: return@LaunchedEffect
        old = s; code = s.code; name = s.name; colors = s.colors; price = s.pricePaisa.plainG(); active = s.active
        sizes.addAll(Garments.list(s.sizes))
        Garments.decodeMaterials(s.materials).forEach { m ->
            app.repo.products.get(m.productId)?.let { p -> mats.add(MatRow(p, (m.perPieceMilli / 1000.0).toBigDecimal().stripTrailingZeros().toPlainString(), (m.wastageBp / 100.0).toBigDecimal().stripTrailingZeros().toPlainString())) }
        }
        Garments.decodeOps(s.operations).forEach { ops.add(OpRow(it.name, it.stage, it.ratePaisa.plainG())) }
        Garments.decodeOther(s.otherCosts).forEach { others.add(CostRow(it.label, it.paisa.plainG())) }
        loaded = true
    }
    fun materials() = mats.map { Garments.Material(it.product.id, it.perPiece.toMilli() ?: 0, ((it.wastage.toMilli() ?: 0) / 10).toInt()) }
    fun operations() = ops.map { Garments.Operation(it.name, it.stage, it.rate.toPaisa() ?: 0) }
    fun otherCosts() = others.map { Garments.OtherCost(it.label, it.amount.toPaisa() ?: 0) }
    val costing = Garments.costing(materials(), operations(), otherCosts(), price.toPaisa() ?: 0) { pid -> mats.firstOrNull { it.product.id == pid }?.product?.costPaisa ?: 0 }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(if (id == 0L) R.string.gm_new_style else R.string.gm_edit_style), onBack)
        if (!loaded) return@Column
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Panel(stringResource(R.string.gm_style_info), Brand.PinkGrad, PanelColors.Purple) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(code, { code = it }, Modifier.weight(1f), label = stringResource(R.string.gm_style_code), placeholder = "ST-101")
                        Input(name, { name = it }, Modifier.weight(1.6f), label = stringResource(R.string.gm_style_name), placeholder = "Polo T-shirt")
                    }
                    T(stringResource(R.string.var_sizes), 13.sp, FontWeight.SemiBold, Ui.c.label)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (Garments.SIZES + sizes.filter { it !in Garments.SIZES }).forEach { sz ->
                            Chip(sz, sz in sizes, { if (sz in sizes) sizes.remove(sz) else sizes.add(sz) })
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Input(extraSize, { extraSize = it }, Modifier.weight(1f), placeholder = stringResource(R.string.gm_other_size), height = 42.dp)
                        WhiteButton("+", Modifier.width(56.dp), height = 42.dp) { extraSize.trim().takeIf { it.isNotEmpty() && it !in sizes }?.let { sizes.add(it) }; extraSize = "" }
                    }
                    Input(colors, { colors = it }, label = stringResource(R.string.var_colors), placeholder = "Red, Navy, White")
                    Input(price, { price = it }, label = stringResource(R.string.gm_price), keyboard = KeyboardType.Decimal)
                    if (id != 0L) Row(verticalAlignment = Alignment.CenterVertically) {
                        T(stringResource(R.string.tl_active), 14.sp, modifier = Modifier.weight(1f)); Toggle(active, { active = it })
                    }
                }
            }
            item {
                Panel(stringResource(R.string.gm_materials), Brand.OrangeGrad, PanelColors.Orange) {
                    Muted(stringResource(R.string.gm_materials_help))
                    mats.forEachIndexed { i, m ->
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                T(m.product.name, 13.sp, FontWeight.SemiBold, maxLines = 1)
                                Muted(m.product.costPaisa.taka0() + " / " + m.product.unit)
                            }
                            Input(m.perPiece, { m.perPiece = it }, Modifier.width(78.dp), label = stringResource(R.string.gm_per_piece), keyboard = KeyboardType.Decimal, height = 40.dp, alignEnd = true)
                            Input(m.wastage, { m.wastage = it }, Modifier.width(62.dp), label = stringResource(R.string.gm_waste), keyboard = KeyboardType.Decimal, height = 40.dp, alignEnd = true, placeholder = "0")
                            T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { mats.removeAt(i) }.padding(8.dp))
                        }
                    }
                    ProductPick(null, { p -> if (p != null && mats.none { it.product.id == p.id }) mats.add(MatRow(p, "1", "5")) }, stringResource(R.string.gm_add_material))
                }
            }
            item {
                Panel(stringResource(R.string.gm_operations), Brand.BlueGrad, PanelColors.Blue) {
                    Muted(stringResource(R.string.gm_operations_help))
                    ops.forEachIndexed { i, op ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Input(op.name, { op.name = it }, Modifier.weight(1f), height = 40.dp)
                                Input(op.rate, { op.rate = it }, Modifier.width(84.dp), keyboard = KeyboardType.Decimal, height = 40.dp, alignEnd = true, placeholder = "৳ 0")
                                T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { ops.removeAt(i) }.padding(8.dp))
                            }
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Garments.STAGES.forEach { (k, l) -> Chip(l.substringBefore(" ("), op.stage == k, { op.stage = k }, soft = true) }
                            }
                        }
                    }
                    WhiteButton(stringResource(R.string.gm_add_operation), Modifier.fillMaxWidth(), height = 42.dp) { ops.add(OpRow("", "SEWING", "")) }
                }
            }
            item {
                Panel(stringResource(R.string.gm_other_costs), Brand.TealGrad, PanelColors.Green) {
                    others.forEachIndexed { i, c ->
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Input(c.label, { c.label = it }, Modifier.weight(1f), height = 40.dp, placeholder = stringResource(R.string.gm_other_hint))
                            Input(c.amount, { c.amount = it }, Modifier.width(90.dp), keyboard = KeyboardType.Decimal, height = 40.dp, alignEnd = true, placeholder = "৳ 0")
                            T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { others.removeAt(i) }.padding(8.dp))
                        }
                    }
                    WhiteButton(stringResource(R.string.gm_add_cost), Modifier.fillMaxWidth(), height = 42.dp) { others.add(CostRow("", "")) }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().softShadow(RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Brand.TotalGrad).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        T(stringResource(R.string.gm_costing), 15.sp, FontWeight.Bold, Color.White)
                        CostLine(stringResource(R.string.gm_c_material), costing.materialPaisa.taka())
                        CostLine(stringResource(R.string.gm_c_labour), costing.labourPaisa.taka())
                        CostLine(stringResource(R.string.gm_c_other), costing.otherPaisa.taka())
                        CostLine(stringResource(R.string.gm_c_total), costing.totalPaisa.taka(), true)
                        CostLine(stringResource(R.string.gm_c_profit), costing.profitPaisa.taka() + (costing.marginBp?.let { "  (${it / 100.0}%)" } ?: ""), true)
                    }
                }
            }
            message?.let { item { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) } }
        }
        BottomBar {
            GradButton(stringResource(R.string.ord_save), Brand.PinkGrad, Modifier.weight(1f), enabled = code.isNotBlank() && name.isNotBlank() && sizes.isNotEmpty() && Garments.list(colors).isNotEmpty()) {
                scope.launch {
                    val st = (old ?: GStyleEntity(code = "", name = "", sizes = "", colors = "", pricePaisa = 0)).copy(
                        code = code, name = name, sizes = sizes.joinToString(", "), colors = colors, pricePaisa = price.toPaisa() ?: 0,
                        materials = Garments.encodeMaterials(materials()), operations = Garments.encodeOps(operations()), otherCosts = Garments.encodeOther(otherCosts()), active = active,
                    )
                    if (app.repo.saveStyle(st) != null) onBack() else message = ctx.getString(R.string.gm_style_bad)
                }
            }
        }
    }
}

@Composable
private fun CostLine(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        T(label, 14.sp, if (strong) FontWeight.Bold else FontWeight.Normal, Color.White.copy(alpha = if (strong) 1f else 0.85f), Modifier.weight(1f))
        T(value, 14.sp, if (strong) FontWeight.Bold else FontWeight.SemiBold, Color.White)
    }
}

/** New production order: a style, who it is for, and how many of each size and colour. */
@Composable
fun GarmentOrderNewScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val styles by remember { app.repo.garments.styles() }.collectAsStateWithLifecycle(emptyList())
    var styleId by remember { mutableStateOf<Long?>(null) }
    val style = styles.firstOrNull { it.id == styleId }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var po by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var day by remember { mutableStateOf<Long?>(gToday() + 30) }
    var note by remember { mutableStateOf("") }
    val grid = remember { mutableStateMapOf<String, String>() }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(styleId) { style?.let { price = it.pricePaisa.plainG(); grid.clear() } }
    val sizes = style?.let { Garments.list(it.sizes) } ?: emptyList()
    val colors = style?.let { Garments.list(it.colors) } ?: emptyList()
    val pieces = grid.values.sumOf { it.num() }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.gm_new_order), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Panel(stringResource(R.string.gm_pick_style), Brand.PinkGrad, PanelColors.Purple) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        styles.filter { it.active }.forEach { s -> Chip(s.code + " · " + s.name, styleId == s.id, { styleId = s.id }) }
                    }
                }
            }
            if (style != null) {
                item {
                    Panel(stringResource(R.string.gm_qty_grid), Brand.BlueGrad, PanelColors.Blue) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(70.dp))
                            sizes.forEach { sz -> T(sz, 12.sp, FontWeight.Bold, modifier = Modifier.weight(1f), align = androidx.compose.ui.text.style.TextAlign.Center) }
                        }
                        colors.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                T(c, 12.sp, FontWeight.SemiBold, modifier = Modifier.width(66.dp), maxLines = 1)
                                sizes.forEach { sz ->
                                    val k = "$sz|$c"
                                    Input(grid[k].orEmpty(), { grid[k] = it.filter(Char::isDigit) }, Modifier.weight(1f), keyboard = KeyboardType.Number, height = 38.dp, alignEnd = true, placeholder = "0")
                                }
                            }
                        }
                        SumRow(stringResource(R.string.gm_total_pieces), pieces.toString(), strong = true)
                    }
                }
                item {
                    Panel(stringResource(R.string.gm_buyer), Brand.GreenGrad, PanelColors.Green) {
                        Muted(stringResource(R.string.gm_buyer_help))
                        PhoneInput(phone, { phone = it }, label = stringResource(R.string.mobile), onName = { if (name.isBlank()) name = it })
                        Input(name, { name = it }, label = stringResource(R.string.staff_name))
                        Input(po, { po = it }, label = stringResource(R.string.gm_po))
                        Input(price, { price = it }, label = stringResource(R.string.gm_unit_price), keyboard = KeyboardType.Decimal)
                        if (pieces > 0) SumRow(stringResource(R.string.gm_order_value), ((price.toPaisa() ?: 0) * pieces).taka(), strong = true, color = Brand.DarkGreen)
                        DateBox(day, { day = it }, stringResource(R.string.tl_delivery))
                        Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    }
                }
            }
            message?.let { item { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) } }
        }
        BottomBar {
            GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.weight(1f), enabled = style != null && pieces > 0) {
                val s = style ?: return@GradButton
                val cells = grid.mapNotNull { (k, v) -> v.num().takeIf { it > 0 }?.let { Garments.Cell(k.substringBefore('|'), k.substringAfter('|'), it) } }
                scope.launch {
                    val n = app.repo.newGarmentOrder(s.id, phone, name, po, day, price.toPaisa() ?: s.pricePaisa, cells, note)
                    if (n != null) onBack() else message = ctx.getString(R.string.tl_invalid)
                }
            }
        }
    }
}

/** One production order: progress by stage, material, output entry, delivery and closing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GarmentOrderScreen(id: Long, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val o by remember(id) { app.repo.garments.watchOrder(id) }.collectAsStateWithLifecycle(null)
    val cells by remember(id) { app.repo.garments.cells(id) }.collectAsStateWithLifecycle(emptyList())
    val stages by remember(id) { app.repo.garments.stageTotals(id) }.collectAsStateWithLifecycle(emptyList())
    val outputs by remember(id) { app.repo.garments.outputs(id) }.collectAsStateWithLifecycle(emptyList())
    val issues by remember(id) { app.repo.garments.issues(id) }.collectAsStateWithLifecycle(emptyList())
    val lines by remember { app.repo.garments.lines() }.collectAsStateWithLifecycle(emptyList())
    val workers by remember { app.repo.garments.workers() }.collectAsStateWithLifecycle(emptyList())
    val style by produceState<GStyleEntity?>(null, o?.styleId) { value = o?.styleId?.let { app.repo.garments.style(it) } }
    var section by rememberSaveable { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val order = o ?: run {
        Column(Modifier.fillMaxSize().background(Ui.c.bg)) { Header(stringResource(R.string.gm_title), onBack) }
        return
    }
    val open = order.status in Garments.OPEN

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(order.number, onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(Modifier.fillMaxWidth().softShadow(RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Brand.TotalGrad).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T((order.styleCode ?: "") + " · " + (order.styleName ?: ""), 17.sp, FontWeight.Bold, Color.White, Modifier.weight(1f))
                            GStatusTag(order.status)
                        }
                        T(listOfNotNull(order.buyerName ?: stringResource(R.string.gm_own_stock), order.buyerPo.ifBlank { null }, order.deliveryDay?.let { stringResource(R.string.tl_delivery_on, it.dayText()) }).joinToString(" · "), 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            HeroNum(stringResource(R.string.gm_ordered), order.ordered.toString())
                            HeroNum(stringResource(R.string.gm_packed), order.packed.toString())
                            HeroNum(stringResource(R.string.gm_sent), order.delivered.toString())
                            HeroNum(stringResource(R.string.gm_wip), order.wipPaisa.taka0())
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    T(stringResource(R.string.gm_progress), 14.sp, FontWeight.Bold)
                    Garments.STAGES.forEach { (k, l) ->
                        val done = stages.firstOrNull { it.stage == k }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                T(l.substringBefore(" ("), 13.sp, modifier = Modifier.weight(1f))
                                Muted("${done?.qty ?: 0} / ${order.ordered}" + if ((done?.rejects ?: 0) > 0) " · ✕${done?.rejects}" else "")
                            }
                            Progress(Garments.pct(done?.qty ?: 0, order.ordered), if (k == "PACKING") Brand.GreenGrad else Brand.BlueGrad)
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    T(stringResource(R.string.gm_sizes_table), 14.sp, FontWeight.Bold)
                    Row { listOf(R.string.gm_size_color, R.string.gm_ordered, R.string.gm_packed, R.string.gm_sent).forEachIndexed { i, r -> Muted(stringResource(r), modifier = Modifier.weight(if (i == 0) 1.6f else 1f)) } }
                    cells.forEach { c ->
                        Row {
                            T(c.size + " · " + c.color, 13.sp, FontWeight.SemiBold, modifier = Modifier.weight(1.6f), maxLines = 1)
                            T(c.qty.toString(), 13.sp, modifier = Modifier.weight(1f))
                            T(c.packed.toString(), 13.sp, color = if (c.packed >= c.qty) Brand.DarkGreen else Color.Unspecified, modifier = Modifier.weight(1f))
                            T(c.delivered.toString(), 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (open) item {
                Segmented(listOf(stringResource(R.string.gm_add_output), stringResource(R.string.gm_material), stringResource(R.string.gm_deliver)), section, { section = it; message = null })
            }
            if (open) item {
                when (section) {
                    0 -> OutputPanel(order.id, style, cells, lines, workers) { message = it }
                    1 -> MaterialPanel(order.id, issues.groupBy { it.productId }.map { (_, l) -> l.first().name to l.sumOf { it.qtyMilli } }) { message = it }
                    else -> DeliverPanel(order.id, cells, order.unitPricePaisa, order.customerId != null) { message = it }
                }
            }
            message?.let { item { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) } }
            if (outputs.isNotEmpty()) item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    T(stringResource(R.string.gm_output_log), 14.sp, FontWeight.Bold)
                    outputs.take(30).forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                T(Garments.stageLabel(g.stage).substringBefore(" (") + (if (g.operation.isNotBlank()) " · " + g.operation else ""), 13.sp, FontWeight.SemiBold, maxLines = 1)
                                Muted(listOfNotNull(g.day.dayText(), g.workerName, g.lineName, if (g.size.isNotBlank()) g.size + " " + g.color else null).joinToString(" · "))
                            }
                            T(g.qty.toString() + if (g.rejects > 0) " (✕${g.rejects})" else "", 13.sp, FontWeight.Bold)
                        }
                    }
                }
            }
            if (open) item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (order.status == "CONFIRMED" && order.wipPaisa == 0L) WhiteButton(stringResource(R.string.gm_cancel_order), Modifier.weight(1f)) {
                        scope.launch { if (app.repo.closeGarmentOrder(order.id, true)) onBack() }
                    } else WhiteButton(stringResource(R.string.gm_close_order), Modifier.weight(1f)) {
                        scope.launch { message = if (app.repo.closeGarmentOrder(order.id, false)) null else ctx.getString(R.string.tl_invalid) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroNum(label: String, value: String) {
    Column {
        T(label, 11.sp, color = Color.White.copy(alpha = 0.8f))
        T(value, 16.sp, FontWeight.Bold, Color.White)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutputPanel(orderId: Long, style: GStyleEntity?, cells: List<GCellEntity>, lines: List<GLineEntity>, workers: List<GWorkerEntity>, onMessage: (String?) -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf("SEWING") }
    var line by remember { mutableStateOf<Long?>(null) }
    var worker by remember { mutableStateOf<Long?>(null) }
    var op by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var cell by remember { mutableStateOf<Long?>(null) }
    var qty by remember { mutableStateOf("") }
    var rejects by remember { mutableStateOf("") }
    val ops = style?.let { Garments.decodeOps(it.operations) }.orEmpty().filter { it.stage == stage }
    Panel(stringResource(R.string.gm_add_output), Brand.BlueGrad, PanelColors.Blue) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Garments.STAGES.forEach { (k, l) -> Chip(l.substringBefore(" ("), stage == k, { stage = k; op = ""; rate = "" }) }
        }
        if (ops.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ops.forEach { x -> Chip(x.name + if (x.ratePaisa > 0) " · " + x.ratePaisa.taka0() else "", op == x.name, { op = x.name; rate = x.ratePaisa.plainG() }, soft = true) }
        }
        if (stage == "PACKING") {
            Muted(stringResource(R.string.gm_pack_help))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.filter { it.packed < it.qty }.forEach { c -> Chip("${c.size} · ${c.color} (${c.qty - c.packed})", cell == c.id, { cell = c.id }) }
            }
        }
        if (lines.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.filter { it.active }.forEach { l -> Chip(l.name, line == l.id, { line = if (line == l.id) null else l.id }, soft = true) }
        }
        if (workers.isNotEmpty()) {
            Muted(stringResource(R.string.gm_worker_optional))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                workers.filter { it.active && (line == null || it.lineId == null || it.lineId == line) }.forEach { w ->
                    Chip(w.name, worker == w.id, { worker = if (worker == w.id) null else w.id; if (line == null) line = w.lineId })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Input(qty, { qty = it.filter(Char::isDigit) }, Modifier.weight(1f), label = stringResource(R.string.gm_good_pcs), keyboard = KeyboardType.Number)
            Input(rejects, { rejects = it.filter(Char::isDigit) }, Modifier.weight(1f), label = stringResource(R.string.gm_reject_pcs), keyboard = KeyboardType.Number, placeholder = "0")
            if (worker != null) Input(rate, { rate = it }, Modifier.weight(1f), label = stringResource(R.string.tl_rate), keyboard = KeyboardType.Decimal, placeholder = "0")
        }
        GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = qty.num() + rejects.num() > 0 && (stage != "PACKING" || cell != null)) {
            val c = cells.firstOrNull { it.id == cell }
            scope.launch {
                val r = app.repo.addOutput(orderId, stage, line, worker, op, if (worker != null) rate.toPaisa() ?: 0 else 0, c?.size.orEmpty(), c?.color.orEmpty(), qty.num(), rejects.num(), "")
                when (r) {
                    ShopRepo.OutputResult.Saved -> { qty = ""; rejects = ""; cell = null; onMessage(null); Toast.makeText(ctx, ctx.getString(R.string.ord_saved), Toast.LENGTH_SHORT).show() }
                    ShopRepo.OutputResult.Invalid -> onMessage(ctx.getString(R.string.tl_invalid))
                    is ShopRepo.OutputResult.TooMany -> onMessage(ctx.getString(R.string.gm_too_many, r.left))
                }
            }
        }
    }
}

@Composable
private fun MaterialPanel(orderId: Long, issued: List<Pair<String, Long>>, onMessage: (String?) -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<ProductEntity?>(null) }
    var qty by remember { mutableStateOf("") }
    var back by remember { mutableStateOf(false) }
    Panel(stringResource(R.string.gm_material), Brand.OrangeGrad, PanelColors.Orange) {
        issued.filter { it.second != 0L }.forEach { (n, q) -> SumRow(n, q.qty()) }
        if (issued.isEmpty()) Muted(stringResource(R.string.gm_no_material))
        Segmented(listOf(stringResource(R.string.gm_issue), stringResource(R.string.gm_return)), if (back) 1 else 0, { back = it == 1 })
        ProductPick(product, { product = it }, stringResource(R.string.gm_add_material))
        Input(qty, { qty = it }, label = stringResource(R.string.tl_cloth_qty, product?.unit ?: ""), keyboard = KeyboardType.Decimal)
        GradButton(stringResource(R.string.ord_save), Brand.OrangeGrad, Modifier.fillMaxWidth(), enabled = product != null && (qty.toMilli() ?: 0) > 0) {
            val p = product ?: return@GradButton
            scope.launch {
                if (app.repo.issueMaterial(orderId, p, qty.toMilli() ?: 0, back)) { product = null; qty = ""; onMessage(null) }
                else onMessage(ctx.getString(if (back) R.string.gm_return_bad else R.string.sale_not_enough, p.name))
            }
        }
    }
}

@Composable
private fun DeliverPanel(orderId: Long, cells: List<GCellEntity>, unitPrice: Long, hasBuyer: Boolean, onMessage: (String?) -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    val scope = rememberCoroutineScope()
    val send = remember { mutableStateMapOf<Long, String>() }
    var discount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    val ready = cells.filter { it.packed > it.delivered }
    val pcs = ready.sumOf { c -> send[c.id]?.num()?.coerceAtMost(c.packed - c.delivered) ?: 0 }
    val total = (pcs * unitPrice - (discount.toPaisa() ?: 0)).coerceAtLeast(0)
    Panel(stringResource(R.string.gm_deliver), Brand.GreenGrad, PanelColors.Green) {
        if (ready.isEmpty()) Muted(stringResource(R.string.gm_nothing_ready))
        ready.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("${c.size} · ${c.color}", 13.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Muted(stringResource(R.string.gm_ready_n, c.packed - c.delivered), modifier = Modifier.padding(end = 8.dp))
                Input(send[c.id].orEmpty(), { send[c.id] = it.filter(Char::isDigit) }, Modifier.width(80.dp), keyboard = KeyboardType.Number, height = 38.dp, alignEnd = true, placeholder = "0")
            }
        }
        if (ready.isNotEmpty()) {
            WhiteButton(stringResource(R.string.gm_send_all), Modifier.fillMaxWidth(), height = 40.dp) { ready.forEach { send[it.id] = (it.packed - it.delivered).toString() } }
            Input(discount, { discount = it }, label = stringResource(R.string.tl_discount), keyboard = KeyboardType.Decimal, placeholder = "0")
            SumRow(stringResource(R.string.tl_total), total.taka(), strong = true)
            Input(paid, { paid = it }, label = stringResource(R.string.gm_paid_now), keyboard = KeyboardType.Decimal, placeholder = total.plainG())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { (m, l) -> Chip(stringResource(l), method == m, { method = m }) }
            }
            GradButton(stringResource(R.string.gm_deliver), Brand.GreenGrad, Modifier.fillMaxWidth(), enabled = pcs > 0) {
                val pay = if (paid.isBlank()) total else (paid.toPaisa() ?: 0).coerceIn(0, total)
                scope.launch {
                    val r = app.repo.deliverGarments(orderId, ready.associate { it.id to (send[it.id]?.num() ?: 0) }, discount.toPaisa() ?: 0, pay, method, who.staffId)
                    onMessage(
                        when (r) {
                            is SaleResult.Saved -> { send.clear(); discount = ""; paid = ""; Toast.makeText(ctx, r.number, Toast.LENGTH_SHORT).show(); null }
                            SaleResult.DueNeedsCustomer -> ctx.getString(R.string.gm_due_needs_buyer)
                            is SaleResult.NotEnoughStock -> ctx.getString(R.string.sale_not_enough, r.productName)
                            is SaleResult.OverCreditLimit -> ctx.getString(R.string.tl_invalid)
                            else -> ctx.getString(R.string.tl_invalid)
                        }
                    )
                }
            }
            if (!hasBuyer) Muted(stringResource(R.string.gm_no_buyer_cash))
        }
    }
}
