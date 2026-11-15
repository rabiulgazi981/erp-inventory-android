package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.sheet.ProductImport
import com.bongshaltech.erpinventory.core.sheet.ProductRow
import com.bongshaltech.erpinventory.core.sheet.Sheet
import com.bongshaltech.erpinventory.data.ShopRepo
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.print.BtPrinter
import com.bongshaltech.erpinventory.print.EscPos
import com.bongshaltech.erpinventory.print.LabelItem
import com.bongshaltech.erpinventory.print.LabelRenderer
import com.bongshaltech.erpinventory.print.ReceiptRenderer
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.SumRow
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** More › Import products: from an Excel (.xlsx) or CSV sheet. */
@Composable
fun ImportScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<ProductRow>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ShopRepo.ImportResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        result = null; error = null; rows = null
        scope.launch {
            val parsed = withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { s ->
                        val b = s.readBytes(); require(b.size <= 20_000_000); b
                    } ?: error("empty")
                    ProductImport.rows(Sheet.read(bytes))
                }.getOrNull()
            }
            if (parsed == null) error = ctx.getString(R.string.import_bad) else rows = parsed
        }
    }
    fun template() {
        val dir = File(ctx.cacheDir, "receipts").apply { mkdirs() }
        val f = File(dir, "products-template.csv").apply { writeText("﻿" + ProductImport.TEMPLATE) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        runCatching {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
        }
    }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.import_products), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(stringResource(R.string.import_products), Brand.GreenGrad, PanelColors.Green) {
                    Muted(stringResource(R.string.import_help))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradButton(stringResource(R.string.import_pick), Brand.GreenGrad, Modifier.weight(1f)) {
                            pick.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream"))
                        }
                        WhiteButton(stringResource(R.string.import_template), Modifier.weight(1f)) { template() }
                    }
                    error?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
            rows?.let { list ->
                item {
                    Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                        SumRow(stringResource(R.string.import_found), list.size.toString(), strong = true)
                        list.take(5).forEach { Muted(it.name + (it.price.takeIf { p -> p.isNotBlank() }?.let { p -> " · ৳$p" } ?: "")) }
                        if (list.size > 5) Muted("…")
                        GradButton(stringResource(R.string.import_go, list.size), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = !busy && list.isNotEmpty()) {
                            busy = true
                            scope.launch { result = app.repo.importProducts(list); rows = null; busy = false }
                        }
                    }
                }
            }
            result?.let { r ->
                item {
                    Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                        SumRow(stringResource(R.string.import_added), r.added.toString(), strong = true, color = Brand.DarkGreen)
                        SumRow(stringResource(R.string.import_updated), r.updated.toString())
                        if (r.skipped.isNotEmpty()) Muted(stringResource(R.string.import_skipped, r.skipped.take(20).joinToString(", ")))
                    }
                }
            }
        }
    }
}

private data class LabelPick(val product: ProductEntity, val copies: String)

/** More › Barcode labels: print stickers with name, price and barcode on the receipt printer, or share them. */
@Composable
fun LabelsScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val picks = remember { mutableStateListOf<LabelPick>() }
    var q by remember { mutableStateOf("") }
    var showShop by remember { mutableStateOf(true) }
    var style by remember { mutableStateOf(com.bongshaltech.erpinventory.print.LabelStyle.BARCODE) }
    var busy by remember { mutableStateOf(false) }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    val total = picks.sumOf { it.copies.toIntOrNull()?.coerceIn(0, 200) ?: 0 }

    suspend fun build(): android.graphics.Bitmap? {
        val items = picks.flatMap { pk ->
            val fresh = app.repo.products.get(pk.product.id) ?: pk.product
            val code = if (style == com.bongshaltech.erpinventory.print.LabelStyle.SHELF) fresh.barcode.orEmpty() else app.repo.ensureBarcode(fresh)
            List(pk.copies.toIntOrNull()?.coerceIn(0, 200) ?: 0) { LabelItem(fresh.name, fresh.pricePaisa.taka(), code) }
        }
        if (items.isEmpty()) return null
        val s = settings
        return withContext(Dispatchers.Default) {
            LabelRenderer.render(items, ReceiptRenderer.widthFor(s?.paperMm ?: 58), if (showShop) s?.profile?.name.orEmpty() else "", style)
        }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.labels), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                val styles = com.bongshaltech.erpinventory.print.LabelStyle.entries
                Segmented(
                    listOf(stringResource(R.string.labels_barcode), stringResource(R.string.labels_qr), stringResource(R.string.labels_shelf)),
                    styles.indexOf(style),
                ) { style = styles[it] }
            }
            item {
                Input(q, { q = it }, placeholder = stringResource(R.string.search_product))
                if (q.isNotBlank()) matches.take(6).forEach { p ->
                    T(p.name + " · " + p.pricePaisa.taka(), 14.sp, modifier = Modifier.fillMaxWidth().clickable {
                        if (picks.none { it.product.id == p.id }) picks.add(LabelPick(p, "1")); q = ""
                    }.padding(vertical = 8.dp))
                }
            }
            if (picks.isEmpty()) item { Muted(stringResource(R.string.labels_help), 13.sp) }
            items(picks.toList(), key = { it.product.id }) { pk ->
                val i = picks.indexOfFirst { it.product.id == pk.product.id }
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            T(pk.product.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(pk.product.barcode ?: stringResource(R.string.labels_new_code))
                        }
                        Input(pk.copies, { if (i >= 0) picks[i] = pk.copy(copies = it.filter(Char::isDigit).take(3)) }, Modifier.width(70.dp),
                            keyboard = KeyboardType.Number, height = 40.dp, alignEnd = true)
                        T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { if (i >= 0) picks.removeAt(i) }.padding(6.dp))
                    }
                }
            }
            if (picks.isNotEmpty()) item {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Chip(stringResource(R.string.labels_show_shop), showShop, { showShop = !showShop })
                }
            }
        }
        if (picks.isNotEmpty()) Row(Modifier.fillMaxWidth().background(Ui.c.card).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WhiteButton(stringResource(R.string.receipt_share), Modifier.weight(1f)) {
                if (!busy) scope.launch { busy = true; build()?.let { shareImage(ctx, it, "labels") }; busy = false }
            }
            GradButton(stringResource(R.string.labels_print, total), Brand.BlueGrad, Modifier.weight(1f), enabled = !busy && total > 0) {
                val address = settings?.printerAddress.orEmpty()
                if (address.isBlank() || !BtPrinter.hasPermission(ctx)) {
                    Toast.makeText(ctx, ctx.getString(R.string.labels_no_printer), Toast.LENGTH_LONG).show()
                    return@GradButton
                }
                scope.launch {
                    busy = true
                    build()?.let { bmp ->
                        val bytes = withContext(Dispatchers.Default) { EscPos.image(bmp, cut = (settings?.paperMm ?: 58) >= 80) }
                        toastResult(ctx, BtPrinter.print(ctx, address, bytes))
                    }
                    busy = false
                }
            }
        }
    }
}

private val actionLabel = mapOf(
    "SALE" to R.string.mv_sale, "PURCHASE" to R.string.mv_purchase, "SALE_RETURN" to R.string.mv_sale_return,
    "PURCHASE_RETURN" to R.string.mv_purchase_return, "EXPENSE" to R.string.mv_expense, "MONEY" to R.string.cash_bank,
    "SALARY" to R.string.hr_salary, "PRODUCT_NEW" to R.string.audit_product_new, "PRICE_CHANGE" to R.string.audit_price,
    "COST_CHANGE" to R.string.audit_cost, "STOCK_EDIT" to R.string.audit_stock, "WRITE_OFF" to R.string.batch_writeoff,
    "REPACK" to R.string.repack, "STOCK_MOVE" to R.string.warehouse_move, "IMPORT" to R.string.import_products,
    "DAY_CLOSE" to R.string.cash_day_close, "DAY_OPEN" to R.string.cash_day_open,
    "PROMO_NEW" to R.string.audit_promo_new, "PROMO_ON" to R.string.audit_promo_on, "PROMO_OFF" to R.string.audit_promo_off,
    "SALE_DELETE" to R.string.audit_sale_delete, "SALE_DELETE_ASK" to R.string.audit_sale_delete_ask, "SALE_DELETE_NO" to R.string.audit_sale_delete_no,
    "STOCK_ADJUST" to R.string.adj_title,
)

/** More › Activity log: who did what. Owner only. */
@Composable
fun AuditScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val staff by remember { app.repo.staff.all() }.collectAsStateWithLifecycle(emptyList())
    var filter by remember { mutableStateOf(-1L) }
    var q by remember { mutableStateOf("") }
    val rows by remember(filter, q) { app.repo.audit.list(filter, q.trim()) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val owner = stringResource(R.string.audit_owner)
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.audit), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.batch_all), filter == -1L, { filter = -1L })
                    Chip(owner, filter == 0L, { filter = 0L })
                    staff.forEach { s -> Chip(s.name, filter == s.id, { filter = s.id }) }
                }
            }
            item { Input(q, { q = it }, placeholder = stringResource(R.string.audit_search)) }
            if (rows.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
            items(rows, key = { it.id }) { r ->
                val warn = r.action in setOf("PRICE_CHANGE", "COST_CHANGE", "STOCK_EDIT", "WRITE_OFF")
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row {
                        T(actionLabel[r.action]?.let { stringResource(it) } ?: r.action, 14.sp, FontWeight.SemiBold,
                            if (warn) Brand.Orange else Ui.c.text, modifier = Modifier.weight(1f))
                        Muted(fmt.format(Date(r.at)))
                    }
                    T(r.detail, 13.sp)
                    Muted(r.staffName ?: owner)
                }
            }
        }
    }
}
