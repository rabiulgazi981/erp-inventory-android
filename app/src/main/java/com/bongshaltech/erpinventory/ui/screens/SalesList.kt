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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.SaleItemEntity
import com.bongshaltech.erpinventory.data.db.SaleRow
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home › See all: recent sales; tap one to see what was in it, tap again for the memo. */
@Composable
fun SalesListScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.SALE)) return NotAllowed(onBack)
    val sales by remember { app.repo.sales.recent(300) }.collectAsStateWithLifecycle(emptyList())
    var q by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    val fmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    val walkIn = stringResource(R.string.walk_in)
    val shown = q.trim().let { t -> if (t.isEmpty()) sales else sales.filter { it.number.contains(t, true) || (it.customerName ?: "").contains(t, true) } }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.sales_all), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Input(q, { q = it }, placeholder = stringResource(R.string.sales_search)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat(stringResource(R.string.sales_count), shown.size.toString(), Modifier.weight(1f))
                    MiniStat(stringResource(R.string.sales_total), shown.sumOf { it.totalPaisa }.taka0(), Modifier.weight(1.4f), Brand.DarkGreen)
                }
            }
            if (shown.isEmpty()) item { Muted(stringResource(R.string.home_no_sales), 14.sp) }
            items(shown, key = { it.id }) { s -> SaleCard(s, open == s.id, walkIn, fmt.format(Date(s.createdAt)), { open = if (open == s.id) null else s.id }) { go(Routes.receipt(s.id)) } }
        }
    }
}

@Composable
private fun SaleCard(s: SaleRow, open: Boolean, walkIn: String, time: String, onClick: () -> Unit, onMemo: () -> Unit) {
    val app = app()
    val lines by produceState(emptyList<SaleItemEntity>(), open, s.id) { if (open) value = app.repo.sales.items(s.id) }
    Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(Icons.Outlined.ShoppingCart, Tints.Blue, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f)) {
                T(s.number, 14.sp, FontWeight.SemiBold)
                Muted(listOf(s.customerName ?: walkIn, time, stringResource(R.string.sales_items, s.items)).joinToString(" · "))
            }
            Column(horizontalAlignment = Alignment.End) {
                T(s.totalPaisa.taka0(), 15.sp, FontWeight.Bold)
                if (s.duePaisa > 0) T(stringResource(R.string.cust_due_amount, s.duePaisa.taka0()), 12.sp, FontWeight.SemiBold, Brand.Red)
            }
        }
        if (open) {
            lines.forEach { l ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(l.name, 13.sp, modifier = Modifier.weight(1f), maxLines = 2)
                    Muted(l.qtyMilli.qty() + " × " + l.unitPricePaisa.taka0(), 12.sp, Modifier.padding(horizontal = 8.dp))
                    T(l.lineTotalPaisa.taka0(), 13.sp, FontWeight.SemiBold)
                }
            }
            T(stringResource(R.string.sales_open_memo), 13.sp, FontWeight.SemiBold, Brand.Link, Modifier.clickable(onClick = onMemo).padding(vertical = 4.dp))
        }
    }
}
