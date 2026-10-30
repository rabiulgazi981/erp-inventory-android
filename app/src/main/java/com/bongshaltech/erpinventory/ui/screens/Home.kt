package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.Session
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.menuSet
import com.bongshaltech.erpinventory.ui.components.BottomNav
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.DevFooter
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tab
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.components.softShadow
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tint
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Who is signed in: their display name and what they may open (null = owner, everything). */
data class Who(val name: String, val allowed: Set<StaffMenu>?, val staffId: Long?, val ready: Boolean = true) {
    fun can(m: StaffMenu) = allowed == null || m in allowed
    val owner get() = allowed == null
}

@Composable
fun rememberWho(): Who {
    val app = app()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val session = settings?.session
    val staff by produceState<Who?>(null, session) {
        value = (session as? Session.Staff)?.let { st -> app.repo.staff.get(st.id)?.let { Who(it.name, it.menuSet(), it.id) } }
    }
    return when {
        session == null -> Who("", emptySet(), null, ready = false)
        session is Session.Staff -> staff ?: Who("", emptySet(), session.id, ready = false)
        else -> Who(settings?.profile?.name.orEmpty(), null, null)
    }
}

fun startOfDay(daysAgo: Long = 0): Long =
    LocalDate.now().minusDays(daysAgo).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Bottom tabs go to the same screens for everyone; screens a staff member may not open show as "not allowed". */
fun tabRoute(t: Tab) = when (t) {
    Tab.HOME -> Routes.HOME
    Tab.SALE -> Routes.SALE
    Tab.PRODUCTS -> Routes.PRODUCTS
    Tab.REPORTS -> Routes.SOON
    Tab.MORE -> Routes.MORE
}

@Composable
fun HomeScreen(go: (String) -> Unit, tab: (Tab) -> Unit) {
    val app = app()
    val who = rememberWho()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val today = remember { startOfDay() }
    val yesterday = remember { startOfDay(1) }
    val total by remember { app.repo.sales.totalSince(today) }.collectAsStateWithLifecycle(0L)
    val totalY by remember { app.repo.sales.totalBetween(yesterday, today) }.collectAsStateWithLifecycle(0L)
    val count by remember { app.repo.sales.countSince(today) }.collectAsStateWithLifecycle(0)
    val cash by remember { app.repo.sales.cashSince(today) }.collectAsStateWithLifecycle(0L)
    val due by remember { app.repo.customers.totalDue() }.collectAsStateWithLifecycle(0L)
    val dueCount by remember { app.repo.customers.dueCount() }.collectAsStateWithLifecycle(0)
    val low by remember { app.repo.products.lowStockCount() }.collectAsStateWithLifecycle(0)
    val recent by remember { app.repo.sales.recent(5) }.collectAsStateWithLifecycle(emptyList())
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val showMoney = who.can(StaffMenu.REPORTS)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Row(
            Modifier.fillMaxWidth().softShadow(RoundedCornerShape(0.dp), 6.dp).background(Ui.c.card).statusBarsPadding()
                .height(56.dp).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(painterResource(R.drawable.logo), "ERP Inventory", Modifier.size(width = 44.dp, height = 26.dp))
            T(settings?.profile?.name.orEmpty(), 16.sp, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            HeaderIcon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan)) { go(Routes.SALE) }
            HeaderIcon(Icons.Outlined.NotificationsNone, stringResource(R.string.notifications)) { go(Routes.SOON) }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                T(stringResource(R.string.home_overview), 20.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                Muted(stringResource(R.string.home_today_date, dateFmt.format(Date())), 13.sp)
            }
            if (showMoney) {
                val change = if (totalY > 0) ((total - totalY) * 100.0 / totalY).roundToInt() else null
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(
                        stringResource(R.string.home_today_sales), total.taka0(),
                        change?.let { stringResource(R.string.home_vs_yesterday, (if (it >= 0) "+" else "") + it) }
                            ?: stringResource(R.string.home_sales_count, count),
                        highlight = true,
                    )
                    Stat(stringResource(R.string.home_cash), cash.taka0(), stringResource(R.string.home_invoices, count))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(stringResource(R.string.home_due), due.taka0(), stringResource(R.string.home_due_customers, dueCount), valueColor = Brand.Amber) {
                    go(Routes.SOON)
                }
                Stat(stringResource(R.string.home_low_stock), stringResource(R.string.home_items, low), stringResource(R.string.home_tap_reorder), valueColor = Brand.Red) {
                    go(Routes.PRODUCTS_LOW)
                }
            }
            Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row {
                    QuickAction(Icons.Outlined.ShoppingCart, Tints.Blue, stringResource(R.string.home_new_sale)) { go(Routes.SALE) }
                    if (who.can(StaffMenu.PRODUCTS)) QuickAction(Icons.Outlined.Add, Tints.Green, stringResource(R.string.product_add)) { go(Routes.productEdit(0)) }
                    if (who.can(StaffMenu.PURCHASE)) QuickAction(Icons.Outlined.Inventory2, Tints.Orange, stringResource(R.string.home_purchase)) { go(Routes.SOON) }
                    if (who.can(StaffMenu.CUSTOMERS)) QuickAction(Icons.Outlined.Payments, Tints.Purple, stringResource(R.string.home_collect_due)) { go(Routes.SOON) }
                }
            }
            Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row {
                        T(stringResource(R.string.home_recent), 15.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                        T(stringResource(R.string.see_all), 13.sp, FontWeight.SemiBold, Brand.Link, Modifier.clickable { go(Routes.SOON) })
                    }
                    if (recent.isEmpty()) Muted(stringResource(R.string.home_no_sales), 13.sp)
                    val walkIn = stringResource(R.string.walk_in)
                    val dueWord = stringResource(R.string.sale_due)
                    recent.forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                T(r.number, 14.sp, FontWeight.SemiBold)
                                Muted(
                                    listOfNotNull(
                                        r.customerName ?: walkIn, timeFmt.format(Date(r.createdAt)),
                                        if (r.duePaisa > 0) "$dueWord ${r.duePaisa.taka0()}" else null,
                                    ).joinToString(" · ")
                                )
                            }
                            T(r.totalPaisa.taka0(), 14.sp, FontWeight.Bold)
                        }
                    }
                }
            }
            DevFooter()
        }
        BottomNav(Tab.HOME, tab)
    }
}

/** Money without paisa when it is a whole amount, like the design's "৳ 24,580". */
fun Long.taka0(): String = taka().removeSuffix(".00")

@Composable
private fun RowScope.Stat(
    title: String,
    value: String,
    sub: String,
    highlight: Boolean = false,
    valueColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    if (highlight) {
        Column(
            Modifier.weight(1f).shadow(10.dp, shape, ambientColor = Brand.Blue, spotColor = Brand.Blue).clip(shape)
                .background(Brand.TotalGrad).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            T(title, 12.sp, color = Color(0xFFE6EEFF))
            T(value, 20.sp, FontWeight.Bold, Color.White, maxLines = 1)
            T(sub, 12.sp, FontWeight.SemiBold, Color.White)
        }
    } else {
        Card(Modifier.weight(1f), padding = 12.dp, onClick = onClick) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Muted(title)
                T(value, 20.sp, FontWeight.Bold, valueColor, maxLines = 1)
                Muted(sub)
            }
        }
    }
}

@Composable
private fun RowScope.QuickAction(icon: ImageVector, tint: Tint, label: String, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TintIcon(icon, tint)
        T(label, 12.sp, FontWeight.Medium, maxLines = 1)
    }
}

