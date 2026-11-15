package com.bongshaltech.erpinventory.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.report.Col
import com.bongshaltech.erpinventory.core.report.ReportTable
import com.bongshaltech.erpinventory.data.ShopRepo
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.print.ReportPdf
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
import com.bongshaltech.erpinventory.ui.components.ListRow
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Every report in More › All reports. [profit] ones need the "cost & profit" right; [dated] ones take a period. */
enum class ReportKind(val title: Int, val profit: Boolean = false, val dated: Boolean = true) {
    PROFIT_LOSS(R.string.rep_summary, profit = true),
    BY_PRODUCT(R.string.rs_by_product, profit = true),
    BY_CUSTOMER(R.string.rs_by_customer),
    BY_STAFF(R.string.rs_by_staff),
    INVOICE_PROFIT(R.string.rs_invoice_profit, profit = true),
    BY_SUPPLIER(R.string.rs_by_supplier),
    STOCK_VALUE(R.string.rs_stock_value, profit = true, dated = false),
    RECEIVABLES(R.string.rs_receivables, dated = false),
    PAYABLES(R.string.rs_payables, dated = false),
    EXPENSES(R.string.rs_expenses),
    CASH_FLOW(R.string.rs_cash_flow),
    PAYROLL(R.string.rs_payroll),
}

private val periods = listOf(R.string.exp_today, R.string.rep_week, R.string.exp_month, R.string.rep_last_month, R.string.exp_year)

private fun periodBounds(i: Int): Pair<LocalDate, LocalDate> {
    val d = LocalDate.now()
    return when (i) {
        0 -> d to d.plusDays(1)
        1 -> d.minusDays(6) to d.plusDays(1)
        2 -> d.withDayOfMonth(1) to d.plusDays(1)
        3 -> d.withDayOfMonth(1).minusMonths(1) to d.withDayOfMonth(1)
        else -> d.withDayOfYear(1) to d.plusDays(1)
    }
}

private fun LocalDate.ms() = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** More › All reports: the list. */
@Composable
fun ReportListScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.REPORTS)) return NotAllowed(onBack)
    val profit = who.can(StaffMenu.COST_PROFIT)
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.rs_all), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val list = ReportKind.entries.filter { profit || !it.profit }
            items(list) { k ->
                Card(Modifier.fillMaxWidth(), padding = 0.dp) {
                    ListRow(stringResource(k.title), Icons.Outlined.Assessment, Tints.Blue, last = true) { go(Routes.report(k.name)) }
                }
            }
        }
    }
}

/** One report: period chips, the table, and Share PDF / Excel (CSV) / Print in the header. */
@Composable
fun ReportViewScreen(kindName: String, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    val kind = runCatching { ReportKind.valueOf(kindName) }.getOrNull() ?: return NotAllowed(onBack)
    if (who.ready && (!who.can(StaffMenu.REPORTS) || (kind.profit && !who.can(StaffMenu.COST_PROFIT)))) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var period by remember { mutableStateOf(2) }
    var table by remember { mutableStateOf<ReportTable?>(null) }
    LaunchedEffect(period) {
        table = null
        table = withContext(Dispatchers.IO) { buildReport(ctx, app.repo, kind, period) }
    }
    fun fileName() = ctx.getString(kind.title) + "-" + LocalDate.now()
    fun pdf(then: (java.io.File) -> Unit) {
        val t = table ?: return
        scope.launch {
            val profile = app.settings.current().profile
            val f = withContext(Dispatchers.IO) { ReportPdf.write(ctx, t, profile, ctx.getString(R.string.dev_by) + " Bongshal Tech · ERP Inventory", fileName()) }
            then(f)
        }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(kind.title), onBack) {
            HeaderIcon(Icons.Outlined.Print, stringResource(R.string.rs_print)) { pdf { ReportPdf.print(ctx, it, fileName()) } }
            HeaderIcon(Icons.Outlined.Share, stringResource(R.string.rs_share_pdf)) { pdf { ReportPdf.share(ctx, it, "application/pdf") } }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (kind.dated) item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEachIndexed { i, p -> Chip(stringResource(p), period == i, { period = i }) }
                }
            }
            val t = table
            if (t == null) item { Muted(stringResource(R.string.rs_loading), 14.sp) }
            else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Muted(t.subtitle, 13.sp, Modifier.weight(1f))
                        Chip(stringResource(R.string.rs_excel), false, {
                            ReportPdf.share(ctx, ReportPdf.writeCsv(ctx, t, fileName()), "text/csv")
                        }, soft = true)
                    }
                }
                if (t.rows.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
                items(t.rows) { r -> TableCard(t.columns, r, false) }
                t.totals?.let { item { TableCard(t.columns, it, true) } }
                items(t.notes) { Muted(it) }
            }
        }
    }
}

/** A row shown as a card: first cell as the title, the rest as label/value lines. */
@Composable
private fun TableCard(cols: List<Col>, cells: List<String>, strong: Boolean) {
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        T(cells.firstOrNull().orEmpty(), 14.sp, if (strong) FontWeight.Bold else FontWeight.SemiBold, maxLines = 2)
        cells.drop(1).forEachIndexed { i, v ->
            if (v.isNotBlank()) Row {
                Muted(cols.getOrNull(i + 1)?.header.orEmpty(), 13.sp, Modifier.weight(1f))
                T(v, 13.sp, if (strong) FontWeight.Bold else FontWeight.Normal, align = TextAlign.End)
            }
        }
    }
}

/** Builds a report in the shop's language. Runs off the main thread. */
suspend fun buildReport(ctx: Context, repo: ShopRepo, kind: ReportKind, period: Int): ReportTable {
    val (a, b) = periodBounds(period)
    val from = a.ms()
    val to = b.ms()
    val df = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val sub = if (kind.dated) df.format(Date(from)) + " – " + df.format(Date(to - 1)) else ctx.getString(R.string.rs_as_of, df.format(Date()))
    val r = repo.reportSet
    fun s(id: Int) = ctx.getString(id)
    fun tk(p: Long) = p.taka()
    val title = s(kind.title)
    return when (kind) {
        ReportKind.PROFIT_LOSS -> {
            val rp = repo.reports
            val gross = rp.sales(from, to).first()
            val returns = rp.saleReturns(from, to).first()
            val cost = rp.costOfSales(from, to).first() - rp.saleReturnCost(from, to).first()
            val disc = rp.discounts(from, to).first()
            val exp = r.expenses(from, to)
            val net = gross - returns
            val gp = net - cost
            val np = gp - exp.sumOf { it.totalPaisa }
            val rows = buildList {
                add(listOf(s(R.string.rep_sales), tk(gross)))
                if (returns > 0) add(listOf(s(R.string.rep_sale_returns), "− " + tk(returns)))
                add(listOf(s(R.string.rep_net_sales), tk(net)))
                add(listOf(s(R.string.rep_discounts), tk(disc)))
                add(listOf(s(R.string.rep_cost_of_sales), "− " + tk(cost)))
                add(listOf(s(R.string.rep_gross_profit), tk(gp)))
                exp.forEach { add(listOf("  " + it.category, "− " + tk(it.totalPaisa))) }
            }
            ReportTable(title, sub, listOf(Col(s(R.string.rs_item)), Col(s(R.string.rs_amount), true)), rows,
                listOf(s(R.string.rep_net_profit), tk(np)), listOf(s(R.string.rs_pl_note)))
        }
        ReportKind.BY_PRODUCT -> {
            val l = r.byProduct(from, to)
            ReportTable(title, sub, listOf(Col(s(R.string.rs_product)), Col(s(R.string.rs_qty), true), Col(s(R.string.rs_revenue), true), Col(s(R.string.rs_cost), true), Col(s(R.string.rs_profit), true)),
                l.map { listOf(it.name, it.qtyMilli.qty(), tk(it.revenuePaisa), tk(it.costPaisa), tk(it.revenuePaisa - it.costPaisa)) },
                listOf(s(R.string.rs_total), "", tk(l.sumOf { it.revenuePaisa }), tk(l.sumOf { it.costPaisa }), tk(l.sumOf { it.revenuePaisa - it.costPaisa })))
        }
        ReportKind.BY_CUSTOMER -> {
            val l = r.byCustomer(from, to)
            ReportTable(title, sub, listOf(Col(s(R.string.rs_customer)), Col(s(R.string.onb_phone)), Col(s(R.string.rs_bills), true), Col(s(R.string.rs_sales), true), Col(s(R.string.rs_paid), true), Col(s(R.string.rs_due_now), true)),
                l.map { listOf(it.name.ifBlank { s(R.string.walk_in) }, it.phone, it.bills.toString(), tk(it.totalPaisa), tk(it.paidPaisa), if (it.name.isBlank()) "" else tk(it.duePaisa)) },
                listOf(s(R.string.rs_total), "", l.sumOf { it.bills }.toString(), tk(l.sumOf { it.totalPaisa }), tk(l.sumOf { it.paidPaisa }), ""))
        }
        ReportKind.BY_STAFF -> {
            val l = r.byStaff(from, to)
            ReportTable(title, sub, listOf(Col(s(R.string.rs_staff)), Col(s(R.string.rs_bills), true), Col(s(R.string.rs_sales), true), Col(s(R.string.rs_paid), true)),
                l.map { listOf(it.name ?: s(R.string.rs_owner), it.bills.toString(), tk(it.totalPaisa), tk(it.paidPaisa)) },
                listOf(s(R.string.rs_total), l.sumOf { it.bills }.toString(), tk(l.sumOf { it.totalPaisa }), tk(l.sumOf { it.paidPaisa })))
        }
        ReportKind.INVOICE_PROFIT -> {
            val l = r.invoices(from, to)
            val d2 = SimpleDateFormat("d MMM", Locale.getDefault())
            ReportTable(title, sub, listOf(Col(s(R.string.rs_invoice)), Col(s(R.string.rs_date)), Col(s(R.string.rs_customer)), Col(s(R.string.rs_revenue), true), Col(s(R.string.rs_cost), true), Col(s(R.string.rs_profit), true)),
                l.map { listOf(it.number, d2.format(Date(it.createdAt)), it.customer ?: s(R.string.walk_in), tk(it.revenuePaisa), tk(it.costPaisa), tk(it.revenuePaisa - it.costPaisa)) },
                listOf(s(R.string.rs_total), "", "", tk(l.sumOf { it.revenuePaisa }), tk(l.sumOf { it.costPaisa }), tk(l.sumOf { it.revenuePaisa - it.costPaisa })))
        }
        ReportKind.BY_SUPPLIER -> {
            val l = r.bySupplier(from, to)
            ReportTable(title, sub, listOf(Col(s(R.string.rs_supplier)), Col(s(R.string.onb_phone)), Col(s(R.string.rs_bills), true), Col(s(R.string.purchases), true), Col(s(R.string.rs_paid), true), Col(s(R.string.rs_due_now), true)),
                l.map { listOf(it.name.ifBlank { "—" }, it.phone, it.bills.toString(), tk(it.totalPaisa), tk(it.paidPaisa), tk(it.duePaisa)) },
                listOf(s(R.string.rs_total), "", l.sumOf { it.bills }.toString(), tk(l.sumOf { it.totalPaisa }), tk(l.sumOf { it.paidPaisa }), ""))
        }
        ReportKind.STOCK_VALUE -> {
            val l = r.stock()
            fun v(q: Long, p: Long) = (q * p + 500) / 1000
            ReportTable(title, sub, listOf(Col(s(R.string.rs_product)), Col(s(R.string.rs_qty), true), Col(s(R.string.rs_cost_each), true), Col(s(R.string.rs_cost_value), true), Col(s(R.string.rs_sale_value), true)),
                l.map { listOf(it.name, it.stockMilli.qty() + " " + it.unit, tk(it.costPaisa), tk(v(it.stockMilli, it.costPaisa)), tk(v(it.stockMilli, it.pricePaisa))) },
                listOf(s(R.string.rs_total), "", "", tk(l.sumOf { v(it.stockMilli, it.costPaisa) }), tk(l.sumOf { v(it.stockMilli, it.pricePaisa) })))
        }
        ReportKind.RECEIVABLES, ReportKind.PAYABLES -> {
            val l = if (kind == ReportKind.RECEIVABLES) r.receivables() else r.payables()
            val cols = buildList {
                add(Col(s(if (kind == ReportKind.RECEIVABLES) R.string.rs_customer else R.string.rs_supplier))); add(Col(s(R.string.onb_phone)))
                if (kind == ReportKind.RECEIVABLES) add(Col(s(R.string.feat_areas)))
                add(Col(s(R.string.rs_due_now), true))
            }
            ReportTable(title, sub, cols,
                l.map { if (kind == ReportKind.RECEIVABLES) listOf(it.name, it.phone, it.area, tk(it.duePaisa)) else listOf(it.name, it.phone, tk(it.duePaisa)) },
                if (kind == ReportKind.RECEIVABLES) listOf(s(R.string.rs_total), "", "", tk(l.sumOf { it.duePaisa })) else listOf(s(R.string.rs_total), "", tk(l.sumOf { it.duePaisa })))
        }
        ReportKind.EXPENSES -> {
            val l = r.expenses(from, to)
            ReportTable(title, sub, listOf(Col(s(R.string.rs_category)), Col(s(R.string.rs_count), true), Col(s(R.string.rs_amount), true)),
                l.map { listOf(it.category, it.count.toString(), tk(it.totalPaisa)) },
                listOf(s(R.string.rs_total), l.sumOf { it.count }.toString(), tk(l.sumOf { it.totalPaisa })))
        }
        ReportKind.CASH_FLOW -> {
            val l = r.cashFlow(from, to)
            val opening = r.cashBefore(from)
            val change = l.sumOf { it.inPaisa - it.outPaisa }
            ReportTable(title, sub, listOf(Col(s(R.string.rs_item)), Col(s(R.string.rs_in), true), Col(s(R.string.rs_out), true)),
                listOf(listOf(s(R.string.rs_opening), tk(opening), "")) +
                    l.map { listOf(kindLabel[it.kind]?.let(::s) ?: it.kind, tk(it.inPaisa), tk(it.outPaisa)) },
                listOf(s(R.string.rs_closing), tk(opening + change), ""), listOf(s(R.string.rs_cash_note)))
        }
        ReportKind.PAYROLL -> {
            val l = r.payroll(from, to)
            ReportTable(title, sub, listOf(Col(s(R.string.rs_employee)), Col(s(R.string.rs_salary), true), Col(s(R.string.hr_advance), true), Col(s(R.string.rs_advance_cut), true)),
                l.map { listOf(it.name, tk(it.salaryPaisa), tk(it.advancePaisa), tk(it.cutPaisa)) },
                listOf(s(R.string.rs_total), tk(l.sumOf { it.salaryPaisa }), tk(l.sumOf { it.advancePaisa }), tk(l.sumOf { it.cutPaisa })))
        }
    }
}
