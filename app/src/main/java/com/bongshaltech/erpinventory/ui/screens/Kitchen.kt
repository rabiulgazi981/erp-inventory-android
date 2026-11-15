package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.GenericSales
import com.bongshaltech.erpinventory.data.db.ItemSales
import com.bongshaltech.erpinventory.data.db.TableSales
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tag
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun startOfToday() = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** More › Kitchen screen: tickets sent from tables, oldest first, moved along New → Cooking → Ready → Served. */
@Composable
fun KitchenScreen(onBack: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val today = remember { startOfToday() }
    // Served tickets stay visible for 30 minutes so a wrong tap can be seen.
    val since = remember(now / 600_000) { System.currentTimeMillis() - 30 * 60_000 }
    val tickets by remember(since) { app.repo.kitchen.active(since) }.collectAsStateWithLifecycle(emptyList())
    val count by remember { app.repo.kitchen.countSince(today) }.collectAsStateWithLifecycle(0)
    val avg by remember { app.repo.kitchen.avgMinutesSince(today) }.collectAsStateWithLifecycle(0L)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.kitchen), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Muted(stringResource(R.string.kitchen_today, count, avg), 13.sp) }
            if (tickets.isEmpty()) item { Muted(stringResource(R.string.kitchen_empty), 14.sp) }
            items(tickets, key = { it.id }) { k ->
                val mins = ((now - k.createdAt) / 60_000).coerceAtLeast(0)
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(k.tableName.ifBlank { "—" }, 18.sp, FontWeight.Bold)
                            Muted(k.number + " · " + stringResource(R.string.kitchen_mins, mins))
                        }
                        when (k.status) {
                            "NEW" -> Tag(stringResource(R.string.kot_new), Color(0xFF1E40AF), Tints.Blue.brush)
                            "COOKING" -> Tag(stringResource(R.string.kot_cooking), Color(0xFF92400E), Tints.Amber.brush)
                            "READY" -> Tag(stringResource(R.string.kot_ready), Color(0xFF166534), Tints.Green.brush)
                            else -> Tag(stringResource(R.string.kot_served), Ui.c.muted, Tints.Grey.brush)
                        }
                    }
                    k.itemsText.lines().filter { it.isNotBlank() }.forEach { line ->
                        val q = line.substringBefore('\t').toLongOrNull() ?: 0
                        T(q.qty() + " × " + line.substringAfter('\t'), 16.sp, FontWeight.SemiBold)
                    }
                    val next = when (k.status) { "NEW" -> "COOKING"; "COOKING" -> "READY"; "READY" -> "SERVED"; else -> null }
                    if (next != null) {
                        val label = when (next) { "COOKING" -> R.string.kot_start; "READY" -> R.string.kot_mark_ready; else -> R.string.kot_mark_served }
                        GradButton(stringResource(label), if (next == "READY") Brand.GreenGrad else Brand.BlueGrad, Modifier.fillMaxWidth(), height = 44.dp) {
                            scope.launch { app.repo.kitchen.setStatus(k.id, next, System.currentTimeMillis()) }
                        }
                    }
                }
            }
        }
    }
}

/** Period chips shared by the two reports: today, 7 days, 30 days. Returns (from, to) in millis. */
@Composable
private fun PeriodPick(sel: Int, onSel: (Int) -> Unit) =
    Segmented(listOf(stringResource(R.string.period_today), stringResource(R.string.period_7), stringResource(R.string.period_30)), sel, onSel)

private fun periodRange(sel: Int): Pair<Long, Long> {
    val days = when (sel) { 0 -> 0L; 1 -> 6L; else -> 29L }
    val from = LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return from to Long.MAX_VALUE
}

/** More › Restaurant report: bills and time per table, and the best-selling items. */
@Composable
fun RestaurantReportScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.REPORTS)) return NotAllowed(onBack)
    var sel by remember { mutableStateOf(0) }
    var tables by remember { mutableStateOf<List<TableSales>>(emptyList()) }
    var items by remember { mutableStateOf<List<ItemSales>>(emptyList()) }
    LaunchedEffect(sel) {
        val (f, t) = periodRange(sel)
        tables = app.repo.kitchen.byTable(f, t)
        items = app.repo.kitchen.topItems(f, t)
    }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.rest_report), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { PeriodPick(sel) { sel = it } }
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Muted(stringResource(R.string.rest_table_bills))
                    T(tables.sumOf { it.bills }.toString() + " · " + tables.sumOf { it.revenuePaisa }.taka(), 18.sp, FontWeight.Bold)
                }
            }
            item { Muted(stringResource(R.string.rest_by_table).uppercase(), 12.sp) }
            if (tables.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
            items(tables) { t ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(t.name, 14.sp, FontWeight.SemiBold)
                            Muted(stringResource(R.string.rest_table_line, t.bills, t.avgMinutes))
                        }
                        T(t.revenuePaisa.taka(), 14.sp, FontWeight.Bold)
                    }
                }
            }
            item { Muted(stringResource(R.string.rest_top_items).uppercase(), 12.sp) }
            items(items) { i -> ItemRow(i.name, i.qtyMilli.qty(), i.revenuePaisa.taka()) }
        }
    }
}

/** More › Pharmacy report: stock and sales grouped by generic name. */
@Composable
fun PharmacyReportScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.REPORTS)) return NotAllowed(onBack)
    var sel by remember { mutableStateOf(2) }
    var rows by remember { mutableStateOf<List<GenericSales>>(emptyList()) }
    LaunchedEffect(sel) {
        val (f, t) = periodRange(sel)
        rows = app.repo.kitchen.byGeneric(f, t)
    }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.pharma_report), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { PeriodPick(sel) { sel = it } }
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Muted(stringResource(R.string.pharma_stock_value))
                    T(rows.sumOf { it.stockValuePaisa }.taka(), 18.sp, FontWeight.Bold)
                    Muted(stringResource(R.string.pharma_sales) + " " + rows.sumOf { it.salesPaisa }.taka())
                }
            }
            items(rows) { r ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(r.generic, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(stringResource(R.string.pharma_line, r.medicines, r.stockMilli.qty(), r.stockValuePaisa.taka(), r.soldMilli.qty()))
                        }
                        T(r.salesPaisa.taka(), 14.sp, FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(name: String, qty: String, amount: String) {
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            Muted(qty + "  ")
            T(amount, 14.sp, FontWeight.Bold)
        }
    }
}
