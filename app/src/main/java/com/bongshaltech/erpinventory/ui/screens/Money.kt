package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Handshake
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
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.LedgerEntry
import com.bongshaltech.erpinventory.data.db.MoveKind
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.SumRow
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val kindLabel = mapOf(
    "SALE" to R.string.mv_sale, "DUE_IN" to R.string.mv_due_in, "PURCHASE" to R.string.mv_purchase,
    "SUPPLIER_PAY" to R.string.mv_supplier_pay, "EXPENSE" to R.string.mv_expense, "SALE_RETURN" to R.string.mv_sale_return,
    "PURCHASE_RETURN" to R.string.mv_purchase_return, "MFS" to R.string.mv_mfs,
    MoveKind.OPENING.name to R.string.mv_opening, MoveKind.TRANSFER.name to R.string.mv_transfer,
    MoveKind.CAPITAL_IN.name to R.string.mv_capital_in, MoveKind.DRAWING.name to R.string.mv_drawing,
    MoveKind.LOAN_TAKEN.name to R.string.mv_loan_taken, MoveKind.LOAN_REPAID.name to R.string.mv_loan_repaid,
    MoveKind.LOAN_GIVEN.name to R.string.mv_loan_given, MoveKind.LOAN_RECEIVED.name to R.string.mv_loan_received,
    MoveKind.VOUCHER_SOLD.name to R.string.mv_voucher_sold,
)

@Composable
private fun MethodChips(selected: PayMethod?, onPick: (PayMethod) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.forEach { (m, label) -> Chip(stringResource(label), selected == m, { onPick(m) }) }
    }
}

/** More › Cash & bank: what is in the drawer, bank and each wallet, plus transfers and owner's money. */
@Composable
fun CashBankScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val balances by remember { app.repo.money.balances() }.collectAsStateWithLifecycle(emptyList())
    var action by remember { mutableStateOf(0) }
    var from by remember { mutableStateOf<PayMethod?>(PayMethod.CASH) }
    var to by remember { mutableStateOf<PayMethod?>(PayMethod.BANK) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val kinds = listOf(MoveKind.TRANSFER, MoveKind.CAPITAL_IN, MoveKind.DRAWING, MoveKind.OPENING)
    val kind = kinds[action]
    val needsFrom = kind == MoveKind.TRANSFER || kind == MoveKind.DRAWING
    val needsTo = kind != MoveKind.DRAWING

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.cash_bank), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            methods.forEach { (m, label) ->
                val b = balances.firstOrNull { it.method == m.name }?.balancePaisa ?: 0
                Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { go(Routes.ledger(m)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TintIcon(Icons.Outlined.AccountBalance, Tints.Blue, 40.dp, 20.dp, 10.dp)
                        T(stringResource(label), 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        T(b.taka(), 16.sp, FontWeight.Bold, if (b < 0) Brand.Red else Ui.c.text)
                    }
                }
            }
            SumRow(stringResource(R.string.cash_bank_total), balances.sumOf { it.balancePaisa }.taka(), strong = true)
            Muted(stringResource(R.string.cash_bank_help), 12.sp)
            Panel(stringResource(R.string.cash_bank_move), Brand.BlueGrad, PanelColors.Blue) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Segmented(kinds.map { stringResource(kindLabel.getValue(it.name)) }, action, { action = it; message = null })
                }
                if (needsFrom) { Muted(stringResource(R.string.cash_bank_from)); MethodChips(from) { from = it } }
                if (needsTo) { Muted(stringResource(R.string.cash_bank_to)); MethodChips(to) { to = it } }
                Input(amount, { amount = it }, label = stringResource(R.string.cust_amount), keyboard = KeyboardType.Decimal)
                Input(note, { note = it }, label = stringResource(R.string.cust_note))
                GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.fillMaxWidth()) {
                    val p = amount.toPaisa() ?: 0
                    scope.launch {
                        val n = app.repo.moveMoney(kind, if (needsFrom) from else null, if (needsTo) to else null, p, note, who.staffId)
                        message = if (n == null) ctx.getString(R.string.cash_bank_bad) else { amount = ""; note = ""; ctx.getString(R.string.ret_saved, n) }
                    }
                }
                message?.let { T(it, 14.sp, FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
fun LedgerScreen(method: PayMethod, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val rows by remember(method) { app.repo.money.ledger(method.name) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val label = methods.firstOrNull { it.first == method }?.second ?: R.string.pay_cash
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.ledger_of, stringResource(label)), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rows.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
            items(rows) { r -> LedgerRow(r, fmt) }
        }
    }
}

@Composable
private fun LedgerRow(r: LedgerEntry, fmt: SimpleDateFormat) {
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(stringResource(kindLabel[r.kind] ?: R.string.mv_transfer), 14.sp, FontWeight.SemiBold)
                Muted(r.number + " · " + fmt.format(Date(r.createdAt)))
            }
            T((if (r.delta >= 0) "+ " else "− ") + kotlin.math.abs(r.delta).taka(), 14.sp, FontWeight.Bold, if (r.delta >= 0) Brand.DarkGreen else Brand.Red)
        }
    }
}

/** More › Loans: money borrowed and lent, and payments against them. */
@Composable
fun LoansScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val loans by remember { app.repo.money.loans() }.collectAsStateWithLifecycle(emptyList())
    var given by remember { mutableStateOf(0) }
    var party by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.loans), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(Modifier.weight(1f), padding = 12.dp) {
                        Muted(stringResource(R.string.loan_we_owe)); T(loans.filter { !it.given }.sumOf { it.outstandingPaisa }.taka0(), 18.sp, FontWeight.Bold, Brand.Red)
                    }
                    Card(Modifier.weight(1f), padding = 12.dp) {
                        Muted(stringResource(R.string.loan_they_owe)); T(loans.filter { it.given }.sumOf { it.outstandingPaisa }.taka0(), 18.sp, FontWeight.Bold, Brand.DarkGreen)
                    }
                }
            }
            items(loans, key = { it.id }) { l ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { go(Routes.loan(l.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TintIcon(Icons.Outlined.Handshake, if (l.given) Tints.Green else Tints.Orange, 40.dp, 20.dp, 10.dp)
                        Column(Modifier.weight(1f)) {
                            T(l.party, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(stringResource(if (l.given) R.string.loan_given else R.string.loan_taken) + " · " + l.principalPaisa.taka0())
                        }
                        T(l.outstandingPaisa.taka0(), 14.sp, FontWeight.Bold)
                    }
                }
            }
            item {
                Panel(stringResource(R.string.loan_new), Brand.OrangeGrad, PanelColors.Orange) {
                    Segmented(listOf(stringResource(R.string.loan_taken), stringResource(R.string.loan_given)), given, { given = it })
                    Input(party, { party = it }, label = stringResource(R.string.loan_party))
                    Input(phone, { phone = it }, label = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone)
                    Input(amount, { amount = it }, label = stringResource(R.string.loan_amount), keyboard = KeyboardType.Decimal)
                    MethodChips(method) { method = it }
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    GradButton(stringResource(R.string.ord_save), Brand.OrangeGrad, Modifier.fillMaxWidth()) {
                        scope.launch {
                            val id = app.repo.newLoan(given == 1, party, phone, amount.toPaisa() ?: 0, method, note, who.staffId)
                            message = if (id == null) ctx.getString(R.string.cash_bank_bad) else { party = ""; phone = ""; amount = ""; note = ""; null }
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
        }
    }
}

@Composable
fun LoanScreen(id: Long, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val loan by remember(id) { app.repo.money.watchLoan(id) }.collectAsStateWithLifecycle(null)
    val moves by remember(id) { app.repo.money.loanMoves(id) }.collectAsStateWithLifecycle(emptyList())
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var message by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val l = loan
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(l?.party ?: stringResource(R.string.loans), onBack)
        if (l != null) LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Muted(stringResource(if (l.given) R.string.loan_given else R.string.loan_taken) + " · " + fmt.format(Date(l.createdAt)))
                    SumRow(stringResource(R.string.loan_amount), l.principalPaisa.taka())
                    SumRow(stringResource(R.string.loan_left), l.outstandingPaisa.taka(), strong = true)
                }
            }
            if (l.outstandingPaisa > 0) item {
                Panel(stringResource(if (l.given) R.string.loan_receive else R.string.loan_pay), Brand.BlueGrad, PanelColors.Blue) {
                    Input(amount, { amount = it }, label = stringResource(R.string.cust_amount), keyboard = KeyboardType.Decimal)
                    MethodChips(method) { method = it }
                    GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.fillMaxWidth()) {
                        scope.launch {
                            val n = app.repo.loanPayment(id, amount.toPaisa() ?: 0, method, "", who.staffId)
                            message = if (n == null) ctx.getString(R.string.cash_bank_bad) else { amount = ""; null }
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
            items(moves, key = { it.id }) { m ->
                SumRow(stringResource(kindLabel[m.kind] ?: R.string.mv_transfer) + " · " + fmt.format(Date(m.createdAt)), m.amountPaisa.taka())
            }
        }
    }
}

/** More › Day open / close: count the drawer when the day starts and ends, and see any shortage. */
@Composable
fun CashDayScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.SALE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val open by remember { app.repo.money.openSession() }.collectAsStateWithLifecycle(null)
    val past by remember { app.repo.money.pastSessions() }.collectAsStateWithLifecycle(emptyList())
    val openedAt = open?.openedAt ?: 0L
    val flow by remember(openedAt) { app.repo.money.watchCashFlowSince(openedAt) }.collectAsStateWithLifecycle(0L)
    var counted by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.cash_day), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                val s = open
                if (s == null) Panel(stringResource(R.string.cash_day_open), Brand.GreenGrad, PanelColors.Green) {
                    Muted(stringResource(R.string.cash_day_open_help))
                    Input(counted, { counted = it }, label = stringResource(R.string.cash_day_counted), keyboard = KeyboardType.Decimal)
                    GradButton(stringResource(R.string.cash_day_open), Brand.GreenGrad, Modifier.fillMaxWidth()) {
                        scope.launch { app.repo.openDay(counted.toPaisa() ?: 0, who.staffId); counted = "" }
                    }
                } else Panel(stringResource(R.string.cash_day_close), Brand.OrangeGrad, PanelColors.Orange) {
                    Muted(stringResource(R.string.cash_day_since, fmt.format(Date(s.openedAt))))
                    SumRow(stringResource(R.string.cash_day_opening), s.openingPaisa.taka())
                    SumRow(stringResource(R.string.cash_day_flow), flow.taka())
                    val expected = s.openingPaisa + flow
                    SumRow(stringResource(R.string.cash_day_expected), expected.taka(), strong = true)
                    Input(counted, { counted = it }, label = stringResource(R.string.cash_day_counted), keyboard = KeyboardType.Decimal)
                    counted.toPaisa()?.let { c ->
                        val diff = c - expected
                        SumRow(stringResource(if (diff < 0) R.string.cash_day_short else R.string.cash_day_over), kotlin.math.abs(diff).taka(),
                            strong = true, color = if (diff < 0) Brand.Red else Brand.DarkGreen)
                    }
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    GradButton(stringResource(R.string.cash_day_close), Brand.OrangeGrad, Modifier.fillMaxWidth(), enabled = counted.toPaisa() != null) {
                        scope.launch { app.repo.closeDay(counted.toPaisa() ?: 0, note); counted = ""; note = "" }
                    }
                }
            }
            if (past.isNotEmpty()) item { T(stringResource(R.string.cash_day_past), 15.sp, FontWeight.Bold) }
            items(past, key = { it.id }) { s ->
                val diff = (s.countedPaisa ?: 0) - (s.expectedPaisa ?: 0)
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Muted(fmt.format(Date(s.openedAt)) + " → " + fmt.format(Date(s.closedAt ?: 0)))
                    SumRow(stringResource(R.string.cash_day_expected), (s.expectedPaisa ?: 0).taka())
                    SumRow(stringResource(R.string.cash_day_counted), (s.countedPaisa ?: 0).taka())
                    if (diff != 0L) SumRow(stringResource(if (diff < 0) R.string.cash_day_short else R.string.cash_day_over), kotlin.math.abs(diff).taka(),
                        strong = true, color = if (diff < 0) Brand.Red else Brand.DarkGreen)
                    if (s.note.isNotBlank()) Muted(s.note)
                }
            }
        }
    }
}
