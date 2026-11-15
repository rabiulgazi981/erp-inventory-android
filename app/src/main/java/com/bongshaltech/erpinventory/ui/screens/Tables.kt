package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.PendingDoc
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.DiningTableEntity
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.SumRow
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** More › Tables: each table's running order, for restaurants and cafés. */
@Composable
fun TablesScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.SALE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val tables by remember { app.repo.tables.tables() }.collectAsStateWithLifecycle(emptyList())
    var newName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.tables), onBack)
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp), Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tables.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Muted(stringResource(R.string.tables_empty), 14.sp) }
            items(tables, key = { it.id }) { t ->
                val busy = t.orderId != null && t.items > 0
                val shape = RoundedCornerShape(14.dp)
                Column(
                    Modifier.height(92.dp).clip(shape).background(if (busy) androidx.compose.ui.graphics.Color(0xFFFFF7ED) else Ui.c.card)
                        .border(1.5.dp, if (busy) androidx.compose.ui.graphics.Color(0xFFC2410C) else Ui.c.border, shape)
                        .clickable { go(Routes.table(t.id)) }.padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    T(t.name, 16.sp, FontWeight.Bold, maxLines = 1)
                    if (busy) {
                        Muted(stringResource(R.string.tables_items, t.items))
                        T(t.totalPaisa.taka0(), 14.sp, FontWeight.SemiBold, Brand.Orange)
                    } else Muted(stringResource(R.string.tables_free))
                }
            }
            if (who.owner) item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Input(newName, { newName = it }, Modifier.weight(1f), label = stringResource(R.string.tables_add), placeholder = "T" + (tables.size + 1))
                    GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.width(96.dp), height = 48.dp) {
                        val n = newName.trim().ifEmpty { "T" + (tables.size + 1) }
                        scope.launch { app.repo.tables.insertTable(DiningTableEntity(name = n)); newName = "" }
                    }
                }
            }
        }
    }
}

@Composable
fun TableScreen(tableId: Long, go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.SALE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val table by remember(tableId) { app.repo.tables.watchTable(tableId) }.collectAsStateWithLifecycle(null)
    val order by remember(tableId) { app.repo.tables.watchOpenOrder(tableId) }.collectAsStateWithLifecycle(null)
    val orderId = order?.id ?: 0L
    val items by remember(orderId) { app.repo.tables.items(orderId) }.collectAsStateWithLifecycle(emptyList())
    var q by remember { mutableStateOf("") }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    var confirmHide by remember { mutableStateOf(false) }
    val name = table?.name.orEmpty()
    val total = items.sumOf { (it.unitPricePaisa * it.qtyMilli + 500) / 1000 }
    val unsent = items.any { it.qtyMilli > it.sentMilli }
    val shop = app.settings.settings.collectAsStateWithLifecycle(null).value?.profile?.name.orEmpty()

    fun kot() = scope.launch {
        val (number, lines) = app.repo.sendKot(orderId) ?: return@launch
        val time = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date())
        val text = buildString {
            if (shop.isNotBlank()) appendLine(shop)
            appendLine("KOT $number · $name")
            appendLine(time)
            appendLine("----------------")
            lines.forEach { (n, q) -> appendLine("${q.qty()}  x  $n") }
        }
        runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)) }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(name.ifEmpty { stringResource(R.string.tables) }, onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Input(q, { q = it }, placeholder = stringResource(R.string.search_product))
            }
            if (q.isNotBlank()) items(matches.take(6), key = { "p" + it.id }) { p ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { scope.launch { app.repo.addToTable(tableId, p); q = "" } }) {
                    Row { T(p.name, 14.sp, modifier = Modifier.weight(1f), maxLines = 1); T(p.pricePaisa.taka0(), 14.sp, FontWeight.SemiBold) }
                }
            }
            if (items.isEmpty() && q.isBlank()) item { Muted(stringResource(R.string.tables_order_empty), 14.sp) }
            items(items, key = { it.id }) { i ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(i.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            if (i.qtyMilli > i.sentMilli) Muted(stringResource(R.string.tables_new_for_kitchen, (i.qtyMilli - i.sentMilli).qty()))
                        }
                        Box(Modifier.size(32.dp).clickable {
                            scope.launch { if (i.qtyMilli <= 1000) app.repo.tables.deleteItem(i.id) else app.repo.tables.setQty(i.id, i.qtyMilli - 1000) }
                        }, contentAlignment = Alignment.Center) { T("−", 18.sp) }
                        T(i.qtyMilli.qty(), 14.sp, FontWeight.SemiBold, modifier = Modifier.width(28.dp), align = TextAlign.Center)
                        Box(Modifier.size(32.dp).clickable { scope.launch { app.repo.tables.setQty(i.id, i.qtyMilli + 1000) } }, contentAlignment = Alignment.Center) { T("+", 18.sp) }
                        T(((i.unitPricePaisa * i.qtyMilli + 500) / 1000).taka0(), 14.sp, FontWeight.Bold, modifier = Modifier.width(70.dp), align = TextAlign.End)
                    }
                }
            }
            if (items.isNotEmpty()) item { SumRow(stringResource(R.string.sale_total), total.taka0(), strong = true) }
            if (items.isEmpty() && order == null && who.owner) item {
                if (confirmHide) Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Muted(stringResource(R.string.tables_remove_sure), modifier = Modifier.weight(1f))
                    T(stringResource(R.string.tables_remove), 13.sp, FontWeight.Bold, Brand.Red,
                        modifier = Modifier.clickable { scope.launch { app.repo.tables.hideTable(tableId); onBack() } }.padding(6.dp))
                } else T(stringResource(R.string.tables_remove), 13.sp, FontWeight.SemiBold, Brand.Red, modifier = Modifier.clickable { confirmHide = true }.padding(vertical = 6.dp))
            }
        }
        if (items.isNotEmpty()) Row(Modifier.fillMaxWidth().background(Ui.c.card).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WhiteButton(stringResource(R.string.tables_kot), Modifier.weight(1f)) { if (unsent) kot() }
            GradButton(stringResource(R.string.tables_bill) + " · " + total.taka0(), Brand.BlueGrad, Modifier.weight(1f)) {
                scope.launch {
                    app.repo.tableToSale(orderId, name)?.let { PendingDoc.sale = it; go(Routes.SALE) }
                }
            }
        }
    }
}
