package com.bongshaltech.erpinventory.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.core.books.AccountType
import com.bongshaltech.erpinventory.core.books.Books
import com.bongshaltech.erpinventory.core.books.JournalLine
import com.bongshaltech.erpinventory.core.books.Recurring
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
import com.bongshaltech.erpinventory.ui.components.Tag
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import java.time.LocalDate
import kotlinx.coroutines.launch

private val typeLabel = mapOf(
    AccountType.ASSET to R.string.acc_asset, AccountType.LIABILITY to R.string.acc_liability, AccountType.EQUITY to R.string.acc_equity,
    AccountType.INCOME to R.string.acc_income, AccountType.EXPENSE to R.string.acc_expense,
)

/** Chart of accounts with each account's balance and a trial balance check. */
@Composable
fun AccountsScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        app.repo.seedAccounts(
            listOf(
                ctx.getString(R.string.acc_s_cash) to AccountType.ASSET, ctx.getString(R.string.acc_s_bank) to AccountType.ASSET,
                ctx.getString(R.string.acc_s_fixed) to AccountType.ASSET, ctx.getString(R.string.acc_s_payable) to AccountType.LIABILITY,
                ctx.getString(R.string.acc_s_capital) to AccountType.EQUITY, ctx.getString(R.string.acc_s_income) to AccountType.INCOME,
                ctx.getString(R.string.acc_s_rent) to AccountType.EXPENSE, ctx.getString(R.string.acc_s_dep) to AccountType.EXPENSE,
            )
        )
    }
    val accounts by remember { app.repo.books.accountTotals() }.collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(0) }
    val dr = accounts.sumOf { it.debitPaisa }
    val cr = accounts.sumOf { it.creditPaisa }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.accounts), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                GradButton(stringResource(R.string.jv_title), Brand.BlueGrad, Modifier.fillMaxWidth()) { go(Routes.JOURNAL) }
            }
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    T(stringResource(R.string.acc_trial), 14.sp, FontWeight.Bold)
                    SumRow(stringResource(R.string.jv_debit), dr.taka())
                    SumRow(stringResource(R.string.jv_credit), cr.taka())
                    T(stringResource(if (dr == cr) R.string.acc_balanced else R.string.acc_not_balanced), 13.sp, FontWeight.SemiBold,
                        if (dr == cr) Brand.DarkGreen else Brand.Red)
                }
            }
            AccountType.entries.forEach { t ->
                val rows = accounts.filter { it.type == t.name }
                if (rows.isNotEmpty()) item(key = "h" + t.name) { Muted(stringResource(typeLabel.getValue(t)).uppercase(), 12.sp) }
                items(rows, key = { it.id }) { a ->
                    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TintIcon(Icons.AutoMirrored.Outlined.MenuBook, Tints.Indigo, 36.dp, 18.dp, 10.dp)
                            Column(Modifier.weight(1f)) {
                                T(a.name, 14.sp, FontWeight.SemiBold, if (a.active) Color.Unspecified else Ui.c.muted, maxLines = 1)
                                Muted(stringResource(R.string.jv_debit) + " " + a.debitPaisa.taka() + " · " + stringResource(R.string.jv_credit) + " " + a.creditPaisa.taka())
                            }
                            T(Books.balance(t, a.debitPaisa, a.creditPaisa).taka(), 14.sp, FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                Panel(stringResource(R.string.acc_new), Brand.PurpleGrad, PanelColors.Purple) {
                    Input(name, { name = it }, label = stringResource(R.string.acc_name))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountType.entries.forEachIndexed { i, t -> Chip(stringResource(typeLabel.getValue(t)), type == i, { type = i }) }
                    }
                    GradButton(stringResource(R.string.ord_save), Brand.PurpleGrad, Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
                        scope.launch { app.repo.addAccount(name, AccountType.entries[type]); name = "" }
                    }
                }
            }
        }
    }
}

private class LineDraft(accountId: Long? = null) {
    var accountId by mutableStateOf(accountId)
    var side by mutableStateOf(0) // 0 debit, 1 credit
    var amount by mutableStateOf("")
}

/** Manual journal entries. Debits must equal credits before it saves. */
@Composable
fun JournalScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val accounts by remember { app.repo.books.activeAccounts() }.collectAsStateWithLifecycle(emptyList())
    val entries by remember { app.repo.books.entries() }.collectAsStateWithLifecycle(emptyList())
    val lines = remember { mutableStateListOf(LineDraft(), LineDraft().also { it.side = 1 }) }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val parsed = lines.map { l ->
        val p = l.amount.toPaisa() ?: 0
        JournalLine(l.accountId ?: 0, if (l.side == 0) p else 0, if (l.side == 1) p else 0)
    }
    val dr = parsed.sumOf { it.debitPaisa }
    val cr = parsed.sumOf { it.creditPaisa }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.jv_title), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(stringResource(R.string.jv_new), Brand.BlueGrad, PanelColors.Blue) {
                    if (accounts.isEmpty()) Muted(stringResource(R.string.jv_no_accounts))
                    lines.forEachIndexed { i, l ->
                        Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                accounts.forEach { a -> Chip(a.name, l.accountId == a.id, { l.accountId = a.id }) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Segmented(listOf(stringResource(R.string.jv_debit), stringResource(R.string.jv_credit)), l.side, { l.side = it })
                                Input(l.amount, { l.amount = it }, Modifier.weight(1f), placeholder = "0", keyboard = KeyboardType.Decimal)
                                if (lines.size > 2) Chip("✕", false, { lines.removeAt(i) }, soft = true)
                            }
                        }
                    }
                    Chip(stringResource(R.string.jv_add_line), false, { lines.add(LineDraft()) }, soft = true)
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    SumRow(stringResource(R.string.jv_debit), dr.taka())
                    SumRow(stringResource(R.string.jv_credit), cr.taka())
                    if (dr != cr) SumRow(stringResource(R.string.jv_diff), (dr - cr).taka(), strong = true, color = Brand.Red)
                    GradButton(stringResource(R.string.ord_save), Brand.BlueGrad, Modifier.fillMaxWidth()) {
                        if (lines.any { (it.amount.toPaisa() ?: 0) > 0 && it.accountId == null }) { message = ctx.getString(R.string.jv_pick_account); return@GradButton }
                        scope.launch {
                            val n = app.repo.postJournal(LocalDate.now().toEpochDay(), note, parsed, who.staffId)
                            if (n == null) message = ctx.getString(R.string.jv_bad)
                            else {
                                message = null; note = ""
                                lines.clear(); lines.add(LineDraft()); lines.add(LineDraft().also { it.side = 1 })
                            }
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
            items(entries, key = { it.id }) { e ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(e.number, 14.sp, FontWeight.SemiBold)
                            Muted(LocalDate.ofEpochDay(e.day).toString() + if (e.note.isNotBlank()) " · " + e.note else "")
                        }
                        T(e.totalPaisa.taka(), 14.sp, FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Monthly expenses (rent, bills). Each one shows as due on its day and posts as a normal expense with one tap. */
@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val list by remember { app.repo.books.recurring() }.collectAsStateWithLifecycle(emptyList())
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.recurring), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { r ->
                val due = r.active && Recurring.isDue(r.dayOfMonth, r.lastMonth, today)
                val posted = r.lastMonth >= today.year * 100 + today.monthValue
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TintIcon(Icons.Outlined.EventRepeat, if (due) Tints.Amber else Tints.Purple, 40.dp, 20.dp, 10.dp)
                        Column(Modifier.weight(1f)) {
                            T(r.category, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(stringResource(R.string.rec_every, r.dayOfMonth) + " · " + r.amountPaisa.taka())
                        }
                        when {
                            !r.active -> Tag(stringResource(R.string.rec_paused), Ui.c.muted, Tints.Grey.brush)
                            posted -> Tag(stringResource(R.string.rec_posted), Color(0xFF166534), Tints.Green.brush)
                            due -> Tag(stringResource(R.string.rec_due), Color(0xFF92400E), Tints.Amber.brush)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (r.active && !posted) Chip(stringResource(R.string.rec_post_now), false, {
                            scope.launch { if (app.repo.postRecurring(r.id, who.staffId) == null) message = ctx.getString(R.string.rec_already) }
                        })
                        Chip(stringResource(if (r.active) R.string.rec_pause else R.string.rec_resume), false, {
                            scope.launch { app.repo.books.setRecurringActive(r.id, !r.active) }
                        }, soft = true)
                        Chip(stringResource(R.string.rec_delete), false, { scope.launch { app.repo.books.deleteRecurring(r.id) } }, soft = true)
                    }
                }
            }
            item {
                Panel(stringResource(R.string.rec_new), Brand.PurpleGrad, PanelColors.Purple) {
                    Input(category, { category = it }, label = stringResource(R.string.rec_category))
                    Input(amount, { amount = it }, label = stringResource(R.string.loan_amount), keyboard = KeyboardType.Decimal)
                    Input(day, { day = it.filter(Char::isDigit).take(2) }, label = stringResource(R.string.rec_day), keyboard = KeyboardType.Number)
                    MethodChips(method) { method = it }
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    GradButton(stringResource(R.string.ord_save), Brand.PurpleGrad, Modifier.fillMaxWidth()) {
                        scope.launch {
                            val id = app.repo.addRecurring(category, amount.toPaisa() ?: 0, method, day.toIntOrNull() ?: 1, note)
                            message = if (id == null) ctx.getString(R.string.cash_bank_bad) else { category = ""; amount = ""; note = ""; null }
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
        }
    }
}

/** People who put money into the shop: what each put in, what was paid back, and their share. */
@Composable
fun InvestorsScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val list by remember { app.repo.books.investors() }.collectAsStateWithLifecycle(emptyList())
    var pick by remember { mutableStateOf<Long?>(null) }
    var dir by remember { mutableStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var note by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.investors), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Muted(stringResource(R.string.inv_total)); T(list.sumOf { it.inPaisa - it.outPaisa }.taka(), 18.sp, FontWeight.Bold, Brand.DarkGreen)
                }
            }
            items(list, key = { it.id }) { i ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { pick = i.id }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TintIcon(Icons.Outlined.Groups, if (pick == i.id) Tints.Blue else Tints.Teal, 40.dp, 20.dp, 10.dp)
                        Column(Modifier.weight(1f)) {
                            T(i.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(stringResource(R.string.inv_in) + " " + i.inPaisa.taka() + " · " + stringResource(R.string.inv_out) + " " + i.outPaisa.taka() +
                                if (i.shareBp > 0) " · " + (i.shareBp / 100.0).toString().removeSuffix(".0") + "%" else "")
                        }
                        T((i.inPaisa - i.outPaisa).taka(), 14.sp, FontWeight.Bold)
                    }
                }
            }
            if (list.isNotEmpty()) item {
                Panel(stringResource(R.string.inv_money), Brand.GreenGrad, PanelColors.Green) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { i -> Chip(i.name, pick == i.id, { pick = i.id }) }
                    }
                    Segmented(listOf(stringResource(R.string.inv_in), stringResource(R.string.inv_out)), dir, { dir = it })
                    Input(amount, { amount = it }, label = stringResource(R.string.loan_amount), keyboard = KeyboardType.Decimal)
                    MethodChips(method) { method = it }
                    Input(note, { note = it }, label = stringResource(R.string.cust_note))
                    GradButton(stringResource(R.string.ord_save), Brand.GreenGrad, Modifier.fillMaxWidth(), enabled = pick != null) {
                        val id = pick ?: return@GradButton
                        scope.launch {
                            val n = app.repo.investorMoney(id, dir == 0, method, amount.toPaisa() ?: 0, note, who.staffId)
                            message = if (n == null) ctx.getString(R.string.cash_bank_bad) else { amount = ""; note = ""; null }
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold, Brand.Red) }
                }
            }
            item {
                Panel(stringResource(R.string.inv_new), Brand.OrangeGrad, PanelColors.Orange) {
                    Input(name, { name = it }, label = stringResource(R.string.loan_party))
                    Input(phone, { phone = it }, label = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone)
                    Input(share, { share = it }, label = stringResource(R.string.inv_share), keyboard = KeyboardType.Decimal)
                    GradButton(stringResource(R.string.ord_save), Brand.OrangeGrad, Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
                        scope.launch {
                            val bp = ((share.toDoubleOrNull() ?: 0.0) * 100).toInt()
                            pick = app.repo.addInvestor(name, phone, bp, "")
                            name = ""; phone = ""; share = ""
                        }
                    }
                }
            }
        }
    }
}
