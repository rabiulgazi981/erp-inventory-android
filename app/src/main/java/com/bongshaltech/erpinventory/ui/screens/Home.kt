package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.Session
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.menuSet
import com.bongshaltech.erpinventory.ui.components.DevFooter
import com.bongshaltech.erpinventory.ui.components.StatCard
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** Who is signed in: their display name and what they may open (null = owner, everything). */
data class Who(val name: String, val allowed: Set<StaffMenu>?, val staffId: Long?) {
    fun can(m: StaffMenu) = allowed == null || m in allowed
}

@Composable
fun rememberWho(): Who {
    val app = app()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val s = settings ?: return Who("", emptySet(), null)
    val session = s.session
    val staff by produceState<Who?>(null, session) {
        value = (session as? Session.Staff)?.let { st -> app.repo.staff.get(st.id)?.let { Who(it.name, it.menuSet(), it.id) } }
    }
    return if (session is Session.Staff) staff ?: Who("", emptySet(), session.id) else Who(s.profile.name, null, null)
}

private fun startOfToday(): Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
fun HomeScreen(go: (String) -> Unit) {
    val app = app()
    val who = rememberWho()
    val today = remember { startOfToday() }
    val total by remember { app.repo.sales.totalSince(today) }.collectAsStateWithLifecycle(0L)
    val count by remember { app.repo.sales.countSince(today) }.collectAsStateWithLifecycle(0)
    val due by remember { app.repo.customers.totalDue() }.collectAsStateWithLifecycle(0L)
    val low by remember { app.repo.products.lowStockCount() }.collectAsStateWithLifecycle(0)
    val recent by remember { app.repo.sales.recent(20) }.collectAsStateWithLifecycle(emptyList())
    val timeFmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            if (who.can(StaffMenu.SALE)) ExtendedFloatingActionButton(
                onClick = { go(Routes.SALE) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.home_new_sale)) },
                containerColor = Brand.Blue, contentColor = androidx.compose.ui.graphics.Color.White,
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(pad).statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.hello, who.name), style = MaterialTheme.typography.headlineSmall)
            }
            if (who.allowed == null || who.can(StaffMenu.REPORTS)) item {
                StatCard(
                    stringResource(R.string.home_today_sales), total.taka(), stringResource(R.string.home_today_count, count),
                    Brand.SalesGradient, Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(stringResource(R.string.home_due), due.taka(), null, Brand.DueGradient, Modifier.weight(1f))
                    StatCard(stringResource(R.string.home_low_stock), low.toString(), null, Brand.StockGradient, Modifier.weight(1f)) {
                        go(Routes.PRODUCTS)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (who.can(StaffMenu.SALE)) Tile(Icons.Default.PointOfSale, stringResource(R.string.home_new_sale), Modifier.weight(1f)) { go(Routes.SALE) }
                    if (who.can(StaffMenu.PRODUCTS)) Tile(Icons.Default.Inventory2, stringResource(R.string.home_products), Modifier.weight(1f)) { go(Routes.PRODUCTS) }
                    if (who.can(StaffMenu.CUSTOMERS)) Tile(Icons.Default.People, stringResource(R.string.home_customers), Modifier.weight(1f)) { go(Routes.SOON) }
                    if (who.can(StaffMenu.REPORTS)) Tile(Icons.Default.QueryStats, stringResource(R.string.home_reports), Modifier.weight(1f)) { go(Routes.SOON) }
                    Tile(Icons.Default.MoreHoriz, stringResource(R.string.home_more), Modifier.weight(1f)) { go(Routes.MORE) }
                }
            }
            item { Text(stringResource(R.string.home_recent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            if (recent.isEmpty()) item { Text(stringResource(R.string.home_no_sales), style = MaterialTheme.typography.bodyMedium) }
            items(recent, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.number, style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(r.customerName, timeFmt.format(Date(r.createdAt))).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(r.totalPaisa.taka(), style = MaterialTheme.typography.titleSmall)
                }
            }
            item { Spacer(Modifier.height(8.dp)); DevFooter(Modifier.navigationBarsPadding().padding(bottom = 64.dp)) }
        }
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brand.Blue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Brand.Blue)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
    }
}
