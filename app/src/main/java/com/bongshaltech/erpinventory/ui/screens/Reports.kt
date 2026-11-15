package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.ui.components.BottomNav
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.SumRow
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tab
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Range { TODAY, YESTERDAY, WEEK, MONTH, LAST_MONTH, YEAR }

/** [from, to) in epoch millis for a range, using the phone's time zone. */
private fun Range.bounds(): Pair<Long, Long> {
    val d = LocalDate.now()
    val (a, b) = when (this) {
        Range.TODAY -> d to d.plusDays(1)
        Range.YESTERDAY -> d.minusDays(1) to d
        Range.WEEK -> d.minusDays(6) to d.plusDays(1)
        Range.MONTH -> d.withDayOfMonth(1) to d.plusDays(1)
        Range.LAST_MONTH -> d.withDayOfMonth(1).minusMonths(1) to d.withDayOfMonth(1)
        Range.YEAR -> d.withDayOfYear(1) to d.plusDays(1)
    }
    val z = ZoneId.systemDefault()
    return a.atStartOfDay(z).toInstant().toEpochMilli() to b.atStartOfDay(z).toInstant().toEpochMilli()
}

private val rangeLabels = listOf(
    Range.TODAY to R.string.exp_today, Range.YESTERDAY to R.string.rep_yesterday, Range.WEEK to R.string.rep_week,
    Range.MONTH to R.string.exp_month, Range.LAST_MONTH to R.string.rep_last_month, Range.YEAR to R.string.exp_year,
)

@Composable
fun ReportsScreen(tab: (Tab) -> Unit) {
    val app = app()
    val who = rememberWho()
    var range by rememberSaveable { mutableStateOf(Range.TODAY) }
    val (from, to) = remember(range) { range.bounds() }
    val r = app.repo.reports
    val sales by remember(from, to) { r.sales(from, to) }.collectAsStateWithLifecycle(0L)
    val count by remember(from, to) { r.saleCount(from, to) }.collectAsStateWithLifecycle(0)
    val discounts by remember(from, to) { r.discounts(from, to) }.collectAsStateWithLifecycle(0L)
    val cost by remember(from, to) { r.costOfSales(from, to) }.collectAsStateWithLifecycle(0L)
    val purchases by remember(from, to) { r.purchases(from, to) }.collectAsStateWithLifecycle(0L)
    val expenses by remember(from, to) { r.expenses(from, to) }.collectAsStateWithLifecycle(0L)
    val collected by remember(from, to) { r.dueCollected(from, to) }.collectAsStateWithLifecycle(0L)
    val dueGiven by remember(from, to) { r.dueGiven(from, to) }.collectAsStateWithLifecycle(0L)
    val methodsTotal by remember(from, to) { r.byMethod(from, to) }.collectAsStateWithLifecycle(emptyList())
    val top by remember(from, to) { r.topProducts(from, to) }.collectAsStateWithLifecycle(emptyList())
    val days by remember(from, to) { r.byDay(from, to) }.collectAsStateWithLifecycle(emptyList())
    val customerDue by remember { app.repo.customers.totalDue() }.collectAsStateWithLifecycle(0L)
    val supplierDue by remember { app.repo.suppliers.totalDue() }.collectAsStateWithLifecycle(0L)

    val gross = sales - cost
    val net = gross - expenses
    val showProfit = who.can(StaffMenu.COST_PROFIT)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.nav_reports))
        if (who.ready && !who.can(StaffMenu.REPORTS)) {
            Muted(stringResource(R.string.not_allowed), 15.sp, Modifier.weight(1f).padding(24.dp))
        } else Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rangeLabels.forEach { (rg, label) -> Chip(stringResource(label), range == rg, { range = rg }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Big(stringResource(R.string.rep_sales), sales.taka0(), pluralStringResourceSafe(count))
                if (showProfit) Big(stringResource(R.string.rep_net_profit), net.taka0(), stringResource(R.string.rep_after_expenses),
                    if (net >= 0) Brand.DarkGreen else Brand.Red)
                else Big(stringResource(R.string.rep_expenses), expenses.taka0(), "")
            }
            Panel(stringResource(R.string.rep_summary), Brand.BlueGrad, PanelColors.Blue) {
                SumRow(stringResource(R.string.rep_sales), sales.taka0())
                SumRow(stringResource(R.string.rep_discounts), discounts.taka0())
                if (showProfit) {
                    SumRow(stringResource(R.string.rep_cost_of_sales), cost.taka0())
                    SumRow(stringResource(R.string.rep_gross_profit), gross.taka0(), strong = true, color = if (gross >= 0) Brand.DarkGreen else Brand.Red)
                }
                SumRow(stringResource(R.string.rep_expenses), expenses.taka0())
                if (showProfit) SumRow(stringResource(R.string.rep_net_profit), net.taka0(), strong = true, color = if (net >= 0) Brand.DarkGreen else Brand.Red)
                SumRow(stringResource(R.string.purchases), purchases.taka0())
            }
            Panel(stringResource(R.string.rep_money), Brand.GreenGrad, PanelColors.Green) {
                methodsTotal.forEach { m ->
                    val label = methods.firstOrNull { it.first.name == m.method }?.second ?: R.string.pay_cash
                    SumRow(stringResource(label), m.totalPaisa.taka0())
                }
                if (methodsTotal.isEmpty()) Muted(stringResource(R.string.rep_no_sales))
                SumRow(stringResource(R.string.rep_due_given), dueGiven.taka0())
                SumRow(stringResource(R.string.rep_due_collected), collected.taka0())
                SumRow(stringResource(R.string.rep_customers_owe), customerDue.taka0(), strong = true, color = Brand.Amber)
                SumRow(stringResource(R.string.pur_we_owe), supplierDue.taka0(), strong = true, color = Brand.Red)
            }
            Panel(stringResource(R.string.rep_top_products), Brand.OrangeGrad, PanelColors.Orange) {
                if (top.isEmpty()) Muted(stringResource(R.string.rep_no_sales))
                top.forEachIndexed { i, p ->
                    Row {
                        T("${i + 1}. ${p.name}", 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Muted("× ${p.qtyMilli.qty()}  ", 13.sp)
                        T(p.totalPaisa.taka0(), 14.sp, FontWeight.SemiBold)
                    }
                }
            }
            if (days.size > 1) Panel(stringResource(R.string.rep_by_day), Brand.PurpleGrad, PanelColors.Purple) {
                val inFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
                val outFmt = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }
                days.forEach { d ->
                    val label = runCatching { LocalDate.parse(d.day, inFmt).format(outFmt) }.getOrDefault(d.day)
                    Row {
                        T(label, 14.sp, modifier = Modifier.weight(1f))
                        Muted("${d.count}  ", 13.sp)
                        T(d.totalPaisa.taka0(), 14.sp, FontWeight.SemiBold)
                    }
                }
            }
        }
        BottomNav(Tab.REPORTS, tab)
    }
}

@Composable
private fun pluralStringResourceSafe(count: Int) =
    androidx.compose.ui.res.pluralStringResource(R.plurals.home_invoices, count, count)

@Composable
private fun RowScope.Big(label: String, value: String, sub: String, color: Color = Color.Unspecified) {
    Card(Modifier.weight(1f), padding = 12.dp) {
        Muted(label)
        T(value, 20.sp, FontWeight.Bold, color, maxLines = 1)
        if (sub.isNotEmpty()) Muted(sub)
    }
}
