package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.WarehouseEntity
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.scanBarcode
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toMilli
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** Search box that picks one product. */
@Composable
internal fun ProductPick(picked: ProductEntity?, onPick: (ProductEntity?) -> Unit, label: String) {
    val app = app()
    var q by remember { mutableStateOf("") }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    if (picked != null) Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Muted(label)
            T(picked.name, 14.sp, FontWeight.SemiBold)
        }
        T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { onPick(null) }.padding(8.dp))
    } else {
        Input(q, { q = it }, label = label, placeholder = stringResource(R.string.search_product))
        if (q.isNotBlank()) matches.take(5).forEach { p ->
            T(p.name + " · " + p.stockMilli.qty() + " " + p.unit, 14.sp,
                modifier = Modifier.fillMaxWidth().clickable { onPick(p); q = "" }.padding(vertical = 6.dp))
        }
    }
}

private data class JobIn(val product: ProductEntity, val qty: String)

/** More › Repack / make: e.g. one 25 kg sack becomes 25 one-kg packs; the packs carry the sack's cost. */
@Composable
fun RepackScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val inputs = remember { mutableStateListOf<JobIn>() }
    var out by remember { mutableStateOf<ProductEntity?>(null) }
    var outQty by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val jobs by remember { app.repo.stockTools.jobs() }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val cost = inputs.sumOf { ((it.qty.toMilli() ?: 0) * it.product.costPaisa + 500) / 1000 }
    val oq = outQty.toMilli() ?: 0

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.repack), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(stringResource(R.string.repack_in), Brand.OrangeGrad, PanelColors.Orange) {
                    Muted(stringResource(R.string.repack_help))
                    inputs.forEachIndexed { i, l ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            T(l.product.name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                            Input(l.qty, { inputs[i] = l.copy(qty = it) }, Modifier.width(90.dp), keyboard = KeyboardType.Decimal, height = 40.dp,
                                placeholder = l.product.unit, alignEnd = true)
                            T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { inputs.removeAt(i) }.padding(6.dp))
                        }
                    }
                    ProductPick(null, { p -> if (p != null && inputs.none { it.product.id == p.id }) inputs.add(JobIn(p, "1")) }, stringResource(R.string.repack_add_in))
                }
            }
            item {
                Panel(stringResource(R.string.repack_out), Brand.GreenGrad, PanelColors.Green) {
                    ProductPick(out, { out = it }, stringResource(R.string.repack_out_product))
                    Input(outQty, { outQty = it }, label = stringResource(R.string.pur_qty, out?.unit ?: ""), keyboard = KeyboardType.Decimal)
                    if (oq > 0 && cost > 0) Muted(stringResource(R.string.repack_cost_each, (cost * 1000 / oq).taka()))
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    val ready = out != null && oq > 0 && inputs.isNotEmpty() && inputs.all { (it.qty.toMilli() ?: 0) > 0 }
                    GradButton(stringResource(R.string.ord_save), Brand.GreenGrad, Modifier.fillMaxWidth(), enabled = ready) {
                        val o = out ?: return@GradButton
                        scope.launch {
                            val r = app.repo.repack(inputs.map { it.product to (it.qty.toMilli() ?: 0) }, o, oq, note, who.staffId)
                            message = when (r) {
                                null -> { inputs.clear(); out = null; outQty = ""; note = ""; ctx.getString(R.string.ord_saved) }
                                "" -> ctx.getString(R.string.cash_bank_bad)
                                else -> ctx.getString(R.string.sale_not_enough, r)
                            }
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold) }
                }
            }
            items(jobs, key = { it.id }) { j ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row {
                        T(j.outName + " +" + j.outQtyMilli.qty(), 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                        T(j.costPaisa.taka(), 13.sp, color = Ui.c.muted)
                    }
                    Muted(j.number + " · " + fmt.format(Date(j.createdAt)) + (j.inputs?.let { " · ← $it" } ?: ""))
                }
            }
        }
    }
}

/** More › Serial / IMEI: record numbers for stock, scan them at the sale, look one up for warranty. */
@Composable
fun SerialsScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    val rows by remember(q) { app.repo.stockTools.serials(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    var product by remember { mutableStateOf<ProductEntity?>(null) }
    var text by remember { mutableStateOf("") }
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    fun add(list: List<String>) {
        val p = product ?: return
        scope.launch {
            val n = app.repo.addSerials(p.id, list)
            Toast.makeText(ctx, ctx.getString(R.string.serial_added, n), Toast.LENGTH_SHORT).show()
            text = ""
        }
    }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.serials), onBack) {
            if (product != null) HeaderIcon(Icons.Outlined.QrCodeScanner, stringResource(R.string.serial_scan)) {
                scanBarcode(ctx) { code -> add(listOf(code)) }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(stringResource(R.string.serial_add), Brand.BlueGrad, PanelColors.Blue) {
                    ProductPick(product, { product = it }, stringResource(R.string.serial_product))
                    if (product != null) {
                        Input(text, { text = it }, label = stringResource(R.string.serial_numbers), height = 96.dp)
                        Muted(stringResource(R.string.serial_help))
                        GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = text.isNotBlank()) {
                            add(text.split('\n', ',', ' '))
                        }
                    }
                }
            }
            item { Input(q, { q = it }, placeholder = stringResource(R.string.serial_find)) }
            if (rows.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
            items(rows, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(r.serial, 14.sp, FontWeight.SemiBold)
                            Muted(r.productName)
                        }
                        if (r.saleId != null) Column(horizontalAlignment = Alignment.End) {
                            T(stringResource(R.string.serial_sold), 13.sp, FontWeight.SemiBold, Brand.DarkGreen)
                            Muted(listOfNotNull(r.saleNumber, r.soldAt?.let { fmt.format(Date(it)) }).joinToString(" · "))
                        } else T(stringResource(R.string.serial_in_stock), 13.sp, FontWeight.SemiBold, Brand.Link)
                    }
                }
            }
        }
    }
}

/** More › Warehouses: stock kept away from the shop floor. Sales only take what is in the shop. */
@Composable
fun WarehousesScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val list by remember { app.repo.stockTools.warehouses() }.collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.warehouses), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Muted(stringResource(R.string.warehouse_help), 12.sp) }
            items(list, key = { it.id }) { w ->
                Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { go(Routes.warehouse(w.id)) }) {
                    Row { T(w.name, 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f)); Muted(stringResource(R.string.warehouse_products, w.products), 13.sp) }
                }
            }
            if (who.owner) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Input(name, { name = it }, Modifier.weight(1f), label = stringResource(R.string.warehouse_new))
                    GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.width(96.dp), height = 48.dp, enabled = name.isNotBlank()) {
                        scope.launch { app.repo.stockTools.insertWarehouse(WarehouseEntity(name = name.trim())); name = "" }
                    }
                }
            }
        }
    }
}

@Composable
fun WarehouseScreen(id: Long, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val w by remember(id) { app.repo.stockTools.watchWarehouse(id) }.collectAsStateWithLifecycle(null)
    val lines by remember(id) { app.repo.stockTools.warehouseStock(id) }.collectAsStateWithLifecycle(emptyList())
    var dir by remember { mutableStateOf(0) }
    var product by remember { mutableStateOf<ProductEntity?>(null) }
    var qty by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<Long?>(null) }
    val all by remember { app.repo.stockTools.warehouses() }.collectAsStateWithLifecycle(emptyList())
    val others = all.filter { it.id != id }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(w?.name ?: stringResource(R.string.warehouses), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(stringResource(R.string.warehouse_move), Brand.BlueGrad, PanelColors.Blue) {
                    Segmented(
                        listOf(stringResource(R.string.warehouse_to_here), stringResource(R.string.warehouse_to_shop)) +
                            if (others.isNotEmpty()) listOf(stringResource(R.string.warehouse_to_other)) else emptyList(),
                        dir, { dir = it },
                    )
                    if (dir == 2) androidx.compose.foundation.layout.Row(
                        Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) { others.forEach { o -> com.bongshaltech.erpinventory.ui.components.Chip(o.name, target == o.id, { target = o.id }) } }
                    ProductPick(product, { product = it }, stringResource(R.string.serial_product))
                    Input(qty, { qty = it }, label = stringResource(R.string.pur_qty, product?.unit ?: ""), keyboard = KeyboardType.Decimal)
                    GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = product != null && (qty.toMilli() ?: 0) > 0) {
                        val p = product ?: return@GradButton
                        scope.launch {
                            val ok = if (dir == 2) target?.let { t -> app.repo.transferStock(id, t, p.id, qty.toMilli() ?: 0) } == true
                                else app.repo.moveStock(id, p.id, qty.toMilli() ?: 0, intoWarehouse = dir == 0)
                            message = if (ok) { qty = ""; product = null; ctx.getString(R.string.ord_saved) } else ctx.getString(R.string.warehouse_not_enough)
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold) }
                }
            }
            if (lines.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
            items(lines, key = { it.productId }) { l ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row { T(l.name, 14.sp, modifier = Modifier.weight(1f), maxLines = 1); T(l.qtyMilli.qty() + " " + l.unit, 14.sp, FontWeight.SemiBold) }
                }
            }
        }
    }
}
