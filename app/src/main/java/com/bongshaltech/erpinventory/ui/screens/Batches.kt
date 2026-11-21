package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.db.search
import com.bongshaltech.erpinventory.core.stock.Expiry
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.ui.components.Card
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** More › Batches & expiry: what expires soon, what has expired, and writing it off. */
@Composable
fun BatchesScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.PRODUCTS)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }
    var tab by remember { mutableStateOf(0) }
    val until = when (tab) { 0 -> today + 90; 1 -> today - 1; else -> -1L }
    val rows by remember(until) { app.repo.batches.list(until) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    var confirm by remember { mutableStateOf<Long?>(null) }

    var q by remember { mutableStateOf("") }
    var product by remember { mutableStateOf<ProductEntity?>(null) }
    var batchNo by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.batches), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Segmented(listOf(stringResource(R.string.batch_soon), stringResource(R.string.batch_expired), stringResource(R.string.batch_all)), tab, { tab = it; confirm = null })
            }
            if (rows.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
            items(rows, key = { it.id }) { b ->
                val expired = b.expiryDay != null && b.expiryDay < today
                val days = b.expiryDay?.let { it - today }
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            T(b.productName, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(listOfNotNull(
                                b.batchNo.takeIf { it.isNotBlank() }?.let { stringResource(R.string.batch_no) + " " + it },
                                b.qtyMilli.qty() + " " + b.unit,
                            ).joinToString(" · "))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            T(b.expiryDay?.let { LocalDate.ofEpochDay(it).format(fmt) } ?: "—", 13.sp, FontWeight.SemiBold,
                                if (expired) Brand.Red else if (days != null && days <= 30) Brand.Red else Ui.c.text)
                            if (days != null) Muted(if (expired) stringResource(R.string.batch_expired) else stringResource(R.string.batch_days_left, days.toInt()))
                        }
                    }
                    if (expired && who.owner) {
                        if (confirm == b.id) Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Muted(stringResource(R.string.batch_writeoff_sure), modifier = Modifier.weight(1f))
                            T(stringResource(R.string.batch_writeoff), 13.sp, FontWeight.Bold, Brand.Red,
                                modifier = Modifier.clickable { scope.launch { app.repo.writeOffBatch(b.id); confirm = null } }.padding(6.dp))
                        } else T(stringResource(R.string.batch_writeoff), 13.sp, FontWeight.SemiBold, Brand.Red,
                            modifier = Modifier.clickable { confirm = b.id }.padding(vertical = 4.dp))
                    }
                }
            }
            item {
                Panel(stringResource(R.string.batch_label_stock), Brand.BlueGrad, PanelColors.Blue) {
                    Muted(stringResource(R.string.batch_label_help))
                    val picked = product
                    if (picked == null) {
                        Input(q, { q = it }, placeholder = stringResource(R.string.search_product))
                        if (q.isNotBlank()) matches.take(5).forEach { p ->
                            T(p.name + " · " + p.stockMilli.qty() + " " + p.unit, 14.sp,
                                modifier = Modifier.fillMaxWidth().clickable { product = p; q = "" }.padding(vertical = 6.dp))
                        }
                    } else Row(verticalAlignment = Alignment.CenterVertically) {
                        T(picked.name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { product = null }.padding(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Input(batchNo, { batchNo = it }, Modifier.weight(1f), label = stringResource(R.string.batch_no))
                        Input(expiry, { expiry = it }, Modifier.weight(1f), label = stringResource(R.string.batch_expiry), placeholder = "12/27",
                            error = stringResource(R.string.batch_expiry_bad).takeIf { expiry.isNotBlank() && Expiry.parse(expiry) == null })
                    }
                    Input(qty, { qty = it }, label = stringResource(R.string.pur_qty, picked?.unit ?: ""), keyboard = KeyboardType.Decimal)
                    val ok = picked != null && (qty.toMilli() ?: 0) > 0 && (expiry.isBlank() || Expiry.parse(expiry) != null) && (batchNo.isNotBlank() || expiry.isNotBlank())
                    GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.fillMaxWidth(), enabled = ok) {
                        val p = picked ?: return@GradButton
                        scope.launch {
                            if (app.repo.addBatch(p.id, batchNo, Expiry.parse(expiry)?.toEpochDay(), qty.toMilli() ?: 0)) {
                                product = null; batchNo = ""; expiry = ""; qty = ""
                            }
                        }
                    }
                }
            }
        }
    }
}
