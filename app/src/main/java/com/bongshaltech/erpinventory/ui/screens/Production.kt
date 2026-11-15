package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.WorkRow
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tag
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

private data class PickLine(val product: ProductEntity, val qty: String, val rate: String = "")

/** More › Manufacturing: saved recipes, and production runs that use up the inputs and add the finished goods. */
@Composable
fun RecipesScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val recipes by remember { app.repo.making.recipes() }.collectAsStateWithLifecycle(emptyList())
    var pick by remember { mutableStateOf<Long?>(null) }
    var make by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var runMsg by remember { mutableStateOf<String?>(null) }
    var out by remember { mutableStateOf<ProductEntity?>(null) }
    var outQty by remember { mutableStateOf("1") }
    val inputs = remember { mutableStateListOf<PickLine>() }
    var note by remember { mutableStateOf("") }
    var newMsg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.manufacturing), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recipes.isEmpty()) item { Muted(stringResource(R.string.mfg_none), 14.sp) }
            items(recipes, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { pick = r.id; runMsg = null }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(r.name + " · " + r.outMilli.qty(), 14.sp, FontWeight.SemiBold, if (pick == r.id) Brand.Blue else Color.Unspecified, maxLines = 1)
                            Muted("← " + (r.inputs ?: ""))
                        }
                        T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { scope.launch { app.repo.making.hideRecipe(r.id) } }.padding(6.dp))
                    }
                    if (pick == r.id) {
                        Input(make, { make = it }, label = stringResource(R.string.mfg_make_qty), keyboard = KeyboardType.Decimal)
                        Input(extra, { extra = it }, label = stringResource(R.string.mfg_extra), keyboard = KeyboardType.Decimal)
                        GradButton(stringResource(R.string.mfg_make), Brand.GreenGrad, Modifier.fillMaxWidth(), enabled = (make.toMilli() ?: 0) > 0) {
                            scope.launch {
                                runMsg = when (val res = app.repo.makeFromRecipe(r.id, make.toMilli() ?: 0, extra.toPaisa() ?: 0, who.staffId)) {
                                    null -> { make = ""; extra = ""; ctx.getString(R.string.ord_saved) }
                                    "" -> ctx.getString(R.string.cash_bank_bad)
                                    else -> ctx.getString(R.string.sale_not_enough, res)
                                }
                            }
                        }
                        runMsg?.let { T(it, 14.sp, FontWeight.SemiBold) }
                    }
                }
            }
            item {
                Panel(stringResource(R.string.mfg_new), Brand.PurpleGrad, PanelColors.Purple) {
                    ProductPick(out, { out = it }, stringResource(R.string.mfg_output))
                    Input(outQty, { outQty = it }, label = stringResource(R.string.mfg_output_qty, out?.unit ?: ""), keyboard = KeyboardType.Decimal)
                    Muted(stringResource(R.string.mfg_inputs))
                    inputs.forEachIndexed { i, l ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            T(l.product.name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                            Input(l.qty, { inputs[i] = l.copy(qty = it) }, Modifier.width(90.dp), keyboard = KeyboardType.Decimal, height = 40.dp,
                                placeholder = l.product.unit, alignEnd = true)
                            T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { inputs.removeAt(i) }.padding(6.dp))
                        }
                    }
                    ProductPick(null, { p -> if (p != null && inputs.none { it.product.id == p.id }) inputs.add(PickLine(p, "1")) }, stringResource(R.string.repack_add_in))
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    GradButton(stringResource(R.string.ord_save), Brand.PurpleGrad, Modifier.fillMaxWidth(), enabled = out != null && inputs.isNotEmpty()) {
                        val o = out ?: return@GradButton
                        scope.launch {
                            val id = app.repo.saveRecipe(o, outQty.toMilli() ?: 0, inputs.map { it.product to (it.qty.toMilli() ?: 0) }, note)
                            newMsg = if (id == null) ctx.getString(R.string.cash_bank_bad) else { out = null; outQty = "1"; inputs.clear(); note = ""; pick = id; null }
                        }
                    }
                    newMsg?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
        }
    }
}

/** More › Factory or Repair: work sent to a supplier, and goods coming back. */
@Composable
fun WorkOrdersScreen(repair: Boolean, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PURCHASE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val orders by remember { app.repo.making.orders(if (repair) "REPAIR" else "FACTORY") }.collectAsStateWithLifecycle(emptyList())
    val suppliers by remember { app.repo.suppliers.search("", false) }.collectAsStateWithLifecycle(emptyList())
    var open by remember { mutableStateOf<Long?>(null) }
    var supplier by remember { mutableStateOf<Long?>(null) }
    val lines = remember { mutableStateListOf<PickLine>() }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val grad = if (repair) Brand.OrangeGrad else Brand.BlueGrad

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(if (repair) R.string.repair else R.string.factory), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Muted(stringResource(if (repair) R.string.repair_help else R.string.factory_help)) }
            items(orders, key = { it.id }) { o ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { open = if (open == o.id) null else o.id }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T((o.supplierName ?: "—") + " · " + o.number, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(fmt.format(Date(o.createdAt)) + if (o.chargePaisa > 0) " · " + o.chargePaisa.taka() else "")
                        }
                        StatusTag(o.status)
                    }
                    if (open == o.id) WorkDetail(o, repair)
                }
            }
            item {
                Panel(stringResource(if (repair) R.string.repair_new else R.string.factory_new), grad, if (repair) PanelColors.Orange else PanelColors.Blue) {
                    if (suppliers.isEmpty()) Muted(stringResource(R.string.work_need_supplier))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suppliers.forEach { s -> Chip(s.name, supplier == s.id, { supplier = s.id }) }
                    }
                    lines.forEachIndexed { i, l ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            T(l.product.name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                            Input(l.qty, { lines[i] = l.copy(qty = it) }, Modifier.width(80.dp), keyboard = KeyboardType.Decimal, height = 40.dp,
                                placeholder = l.product.unit, alignEnd = true)
                            if (!repair) Input(l.rate, { lines[i] = l.copy(rate = it) }, Modifier.width(90.dp), keyboard = KeyboardType.Decimal, height = 40.dp,
                                placeholder = stringResource(R.string.work_rate), alignEnd = true)
                            T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { lines.removeAt(i) }.padding(6.dp))
                        }
                    }
                    ProductPick(null, { p -> if (p != null && lines.none { it.product.id == p.id }) lines.add(PickLine(p, "1")) }, stringResource(R.string.work_add))
                    Input(note, { note = it }, label = stringResource(if (repair) R.string.repair_problem else R.string.cust_note))
                    GradButton(stringResource(R.string.ord_save), grad, Modifier.fillMaxWidth(), enabled = supplier != null && lines.isNotEmpty()) {
                        val sid = supplier ?: return@GradButton
                        scope.launch {
                            val r = app.repo.newWorkOrder(repair, sid, lines.map { Triple(it.product, it.qty.toMilli() ?: 0, it.rate.toPaisa() ?: 0) }, note, who.staffId)
                            message = when (r) {
                                null -> { lines.clear(); note = ""; null }
                                "" -> ctx.getString(R.string.cash_bank_bad)
                                else -> ctx.getString(R.string.sale_not_enough, r)
                            }
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: String) = when (status) {
    "DONE" -> Tag(stringResource(R.string.work_done), Color(0xFF166534), Tints.Green.brush)
    "CANCELLED" -> Tag(stringResource(R.string.work_cancelled), Ui.c.muted, Tints.Grey.brush)
    else -> Tag(stringResource(R.string.work_open), Color(0xFF92400E), Tints.Amber.brush)
}

@Composable
private fun WorkDetail(o: WorkRow, repair: Boolean) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    val scope = rememberCoroutineScope()
    val lines by remember(o.id) { app.repo.making.watchLines(o.id) }.collectAsStateWithLifecycle(emptyList())
    val got = remember(o.id) { mutableStateMapOf<Long, String>() }
    val scrap = remember(o.id) { mutableStateMapOf<Long, String>() }
    var charge by remember(o.id) { mutableStateOf("") }
    var message by remember(o.id) { mutableStateOf<String?>(null) }
    if (o.note.isNotBlank()) Muted(o.note)
    lines.forEach { l ->
        val left = l.qtyMilli - l.doneMilli - l.scrapMilli
        Column(Modifier.padding(top = 6.dp)) {
            T(l.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
            Muted(stringResource(R.string.work_line, l.qtyMilli.qty(), l.doneMilli.qty(), l.scrapMilli.qty()) +
                if (!repair && l.ratePaisa > 0) " · " + l.ratePaisa.taka() else "")
            if (o.status == "OPEN" && left > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Input(got[l.id] ?: "", { got[l.id] = it }, Modifier.weight(1f), label = stringResource(R.string.work_got), keyboard = KeyboardType.Decimal, height = 40.dp)
                Input(scrap[l.id] ?: "", { scrap[l.id] = it }, Modifier.weight(1f), label = stringResource(R.string.work_scrap), keyboard = KeyboardType.Decimal, height = 40.dp)
            }
        }
    }
    if (o.status == "OPEN") {
        if (repair) Input(charge, { charge = it }, label = stringResource(R.string.repair_charge), keyboard = KeyboardType.Decimal)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradButton(stringResource(R.string.work_receive), Brand.GreenGrad, Modifier.weight(1f)) {
                scope.launch {
                    val map = lines.associate { it.id to ((got[it.id]?.toMilli() ?: 0) to (scrap[it.id]?.toMilli() ?: 0)) }
                    val ok = app.repo.receiveWork(o.id, map, charge.toPaisa() ?: 0, who.staffId)
                    message = if (ok) { got.clear(); scrap.clear(); charge = ""; ctx.getString(R.string.ord_saved) } else ctx.getString(R.string.cash_bank_bad)
                }
            }
            Chip(stringResource(R.string.work_cancel), false, { scope.launch { app.repo.cancelWork(o.id) } }, soft = true)
        }
        Muted(stringResource(R.string.work_due_note))
    }
    message?.let { T(it, 14.sp, FontWeight.SemiBold) }
}
