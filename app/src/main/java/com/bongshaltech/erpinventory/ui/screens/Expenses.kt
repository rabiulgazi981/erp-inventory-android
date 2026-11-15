package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.ExpenseEntity
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class Period { TODAY, MONTH, YEAR }

private fun Period.from(): Long {
    val d = LocalDate.now()
    val start = when (this) { Period.TODAY -> d; Period.MONTH -> d.withDayOfMonth(1); Period.YEAR -> d.withDayOfYear(1) }
    return start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** Ready-made categories; shops can type their own too. */
private val presetCategories = listOf(
    R.string.exp_rent, R.string.exp_electricity, R.string.exp_salary, R.string.exp_transport,
    R.string.exp_tea, R.string.exp_internet, R.string.exp_repair, R.string.exp_other,
)

@Composable
fun ExpensesScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.EXPENSE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf(Period.MONTH) }
    var category by rememberSaveable { mutableStateOf("") }
    val from = period.from()
    val list by remember(period, category) { app.repo.expenses.since(from, category) }.collectAsStateWithLifecycle(emptyList())
    val total by remember(period) { app.repo.expenses.totalSince(from) }.collectAsStateWithLifecycle(0L)
    val cats by remember(period) { app.repo.expenses.byCategory(from) }.collectAsStateWithLifecycle(emptyList())

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.expenses), onBack)
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.exp_today), period == Period.TODAY, { period = Period.TODAY; category = "" })
                        Chip(stringResource(R.string.exp_month), period == Period.MONTH, { period = Period.MONTH; category = "" })
                        Chip(stringResource(R.string.exp_year), period == Period.YEAR, { period = Period.YEAR; category = "" })
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Muted(stringResource(R.string.exp_total))
                        T(total.taka0(), 24.sp, FontWeight.Bold, if (total > 0) Brand.Red else Ui.c.text)
                    }
                }
                if (cats.size > 1) item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.exp_all_categories), category.isEmpty(), { category = "" })
                        cats.forEach { c -> Chip("${c.category} · ${c.totalPaisa.taka0()}", category == c.category, { category = c.category }) }
                    }
                }
                if (list.isEmpty()) item { Muted(stringResource(R.string.exp_empty), 14.sp) }
                items(list, key = { it.id }) { e ->
                    ExpenseRow(e, canDelete = who.owner) { scope.launch { app.repo.expenses.delete(e.id) } }
                }
            }
            val shape = CircleShape
            Row(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).height(52.dp)
                    .shadow(10.dp, shape, ambientColor = Brand.Purple, spotColor = Brand.Purple).clip(shape)
                    .background(Brand.PurpleGrad).clickable { go(Routes.EXPENSE_NEW) }.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                T(stringResource(R.string.exp_add), 15.sp, FontWeight.SemiBold, Color.White, shadow = true)
            }
        }
    }
}

@Composable
private fun ExpenseRow(e: ExpenseEntity, canDelete: Boolean, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val method = methods.firstOrNull { it.first == e.method }?.second ?: R.string.pay_cash
    var confirm by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TintIcon(Icons.Outlined.ReceiptLong, Tints.Purple, 40.dp, 20.dp, 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(e.category, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(listOf(e.note, fmt.format(Date(e.createdAt)), stringResource(method)).filter { it.isNotBlank() }.joinToString(" · "))
            }
            T(e.amountPaisa.taka0(), 14.sp, FontWeight.Bold)
            if (canDelete) Icon(
                Icons.Outlined.DeleteOutline, stringResource(R.string.exp_delete),
                tint = if (confirm) Brand.Red else Ui.c.muted,
                modifier = Modifier.size(22.dp).clickable { if (confirm) onDelete() else confirm = true },
            )
        }
        if (confirm) T(stringResource(R.string.exp_delete_confirm), 12.sp, FontWeight.SemiBold, Brand.Red, Modifier.padding(top = 6.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseNewScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.EXPENSE)) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val used by remember { app.repo.expenses.usedCategories() }.collectAsStateWithLifecycle(emptyList())
    var category by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf(PayMethod.CASH) }
    var note by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val required = stringResource(R.string.required)
    val savedMsg = stringResource(R.string.exp_saved)
    val presets = presetCategories.map { stringResource(it) }
    val choices = (used + presets).distinct()

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.exp_add), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Panel(stringResource(R.string.exp_panel), Brand.PurpleGrad, PanelColors.Purple) {
                T(stringResource(R.string.exp_category), 13.sp, FontWeight.SemiBold, Ui.c.label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { c -> Chip(c, category == c, { category = c }, soft = true) }
                }
                Input(category, { category = it }, placeholder = stringResource(R.string.exp_custom_category),
                    error = required.takeIf { tried && category.isBlank() })
                val amountP = amount.toPaisa()
                Input(amount, { amount = it }, label = stringResource(R.string.exp_amount), keyboard = KeyboardType.Decimal,
                    error = required.takeIf { tried && (amountP == null || amountP <= 0) })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (m, label) -> Chip(stringResource(label), method == m, { method = m }) }
                }
                Input(note, { note = it }, label = stringResource(R.string.cust_note))
            }
        }
        BottomBar {
            GradButton(stringResource(R.string.exp_save), Brand.PurpleGrad, Modifier.weight(1f), height = 52.dp) {
                tried = true
                val p = amount.toPaisa()
                if (category.isBlank() || p == null || p <= 0) return@GradButton
                scope.launch {
                    val number = app.repo.addExpense(category, p, method, note, who.staffId)
                    Toast.makeText(ctx, "$savedMsg · $number", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
        }
    }
}
