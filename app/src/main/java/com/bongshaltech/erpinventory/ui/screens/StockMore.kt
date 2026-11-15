package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.bongshaltech.erpinventory.core.stock.Reorder
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.ProductEntity
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
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toMilli
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Reasons for a stock correction; the key is saved, the label is shown. */
private val adjustReasons = listOf(
    "DAMAGED" to R.string.adj_damaged, "LOST" to R.string.adj_lost, "EXPIRED" to R.string.adj_expired,
    "FOUND" to R.string.adj_found, "COUNT" to R.string.adj_count, "OWN_USE" to R.string.adj_own_use,
)

/** Stock adjustment: correct the shop's stock with a reason, and see past corrections. */
@Composable
fun StockAdjustScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val rows by remember { app.repo.stockMore.adjustments() }.collectAsStateWithLifecycle(emptyList())
    var product by remember { mutableStateOf<ProductEntity?>(null) }
    var dir by remember { mutableStateOf(1) }
    var qty by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("DAMAGED") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val labelOf = adjustReasons.toMap()
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.adj_title), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(stringResource(R.string.adj_new), Brand.OrangeGrad, PanelColors.Orange) {
                    ProductPick(product, { product = it }, stringResource(R.string.serial_product))
                    product?.let { Muted(stringResource(R.string.adj_now, it.stockMilli.qty() + " " + it.unit)) }
                    Segmented(listOf(stringResource(R.string.adj_add), stringResource(R.string.adj_remove)), dir, { dir = it; reason = if (it == 0) "FOUND" else "DAMAGED" })
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        adjustReasons.filter { (k, _) -> if (dir == 0) k in setOf("FOUND", "COUNT") else k != "FOUND" }
                            .forEach { (k, label) -> Chip(stringResource(label), reason == k, { reason = k }) }
                    }
                    Input(qty, { qty = it }, label = stringResource(R.string.pur_qty, product?.unit ?: ""), keyboard = KeyboardType.Decimal)
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    GradButton(stringResource(R.string.ord_save), Brand.OrangeGrad, Modifier.fillMaxWidth(), enabled = product != null && (qty.toMilli() ?: 0) > 0) {
                        val p = product ?: return@GradButton
                        val q = qty.toMilli() ?: 0
                        scope.launch {
                            val ok = app.repo.adjustStock(p.id, if (dir == 0) q else -q, reason, note, who.staffId)
                            message = if (ok) { qty = ""; note = ""; product = null; ctx.getString(R.string.ord_saved) } else ctx.getString(R.string.warehouse_not_enough)
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold) }
                }
            }
            if (rows.isNotEmpty()) item { T(stringResource(R.string.adj_history), 15.sp, FontWeight.Bold) }
            items(rows, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(r.productName, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                        T((if (r.qtyMilli > 0) "+" else "−") + kotlin.math.abs(r.qtyMilli).qty() + " " + r.unit, 14.sp, FontWeight.Bold,
                            if (r.qtyMilli > 0) Brand.DarkGreen else Brand.Red)
                    }
                    Muted(listOfNotNull(labelOf[r.reason]?.let { stringResource(it) } ?: r.reason, r.note.ifBlank { null }, fmt.format(r.createdAt)).joinToString(" · "))
                }
            }
        }
    }
}

/** Reorder list: everything at or under its low-stock mark, with how much to buy. Shareable as text for the supplier. */
@Composable
fun ReorderScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val list by remember { app.repo.stockMore.reorder() }.collectAsStateWithLifecycle(emptyList())
    val title = stringResource(R.string.reorder)
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(title, onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Muted(stringResource(R.string.reorder_note), 13.sp) }
            if (list.isEmpty()) item { Muted(stringResource(R.string.reorder_empty), 14.sp) }
            items(list, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(p.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(stringResource(R.string.reorder_have, p.stockMilli.qty() + " " + p.unit, p.lowStockMilli.qty()))
                        }
                        T(stringResource(R.string.reorder_buy, Reorder.suggest(p.stockMilli, p.lowStockMilli).qty() + " " + p.unit), 14.sp, FontWeight.Bold, Brand.Orange)
                    }
                }
            }
        }
        if (list.isNotEmpty()) BottomBar {
            GradButton(stringResource(R.string.receipt_share), Brand.OrangeGrad, Modifier.weight(1f)) {
                val text = title + "\n" + list.joinToString("\n") { "• ${it.name}: ${Reorder.suggest(it.stockMilli, it.lowStockMilli).qty()} ${it.unit}" }
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
            }
        }
    }
}
