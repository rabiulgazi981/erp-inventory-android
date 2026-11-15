package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.bongshaltech.erpinventory.core.hr.Payroll
import com.bongshaltech.erpinventory.data.db.AttendanceEntity
import com.bongshaltech.erpinventory.data.db.AttendanceStatus
import com.bongshaltech.erpinventory.data.db.EmployeeEntity
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
import com.bongshaltech.erpinventory.ui.components.Toggle
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private fun LocalDate.key() = year * 10000 + monthValue * 100 + dayOfMonth
private fun YearMonth.key() = year * 100 + monthValue

private val statusLabel = listOf(
    AttendanceStatus.PRESENT to R.string.hr_present, AttendanceStatus.ABSENT to R.string.hr_absent,
    AttendanceStatus.HALF to R.string.hr_half, AttendanceStatus.LEAVE to R.string.hr_leave,
)

@Composable
private fun Stepper(text: String, onPrev: () -> Unit, onNext: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clickable(onClick = onPrev), contentAlignment = Alignment.Center) { T("‹", 22.sp) }
        T(text, 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f), align = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.size(40.dp).let { if (onNext != null) it.clickable(onClick = onNext) else it }, contentAlignment = Alignment.Center) {
            T("›", 22.sp, color = if (onNext != null) Ui.c.text else Ui.c.muted)
        }
    }
}

/** More › Staff attendance & salary. Owner only. */
@Composable
fun HrScreen(onBack: () -> Unit) {
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.hr), onBack)
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Segmented(listOf(stringResource(R.string.hr_attendance), stringResource(R.string.hr_salary), stringResource(R.string.hr_people)), tab, { tab = it })
        }
        when (tab) {
            0 -> Attendance()
            1 -> Salaries(who.staffId)
            else -> People()
        }
    }
}

@Composable
private fun Attendance() {
    val app = app()
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var day by remember { mutableStateOf(today) }
    val people by remember { app.repo.hr.employees() }.collectAsStateWithLifecycle(emptyList())
    val marks by remember(day) { app.repo.hr.day(day.key()) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()) }
    var dept by remember { mutableStateOf("") }
    val depts = people.filter { it.active }.map { it.department }.filter { it.isNotBlank() }.distinct().sorted()
    val active = people.filter { it.active && (dept.isEmpty() || it.department == dept) }
    val locked by remember(day) { app.repo.hr.watchLocked(day.year * 100 + day.monthValue) }.collectAsStateWithLifecycle(0)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Stepper(day.format(fmt), { day = day.minusDays(1) }, if (day < today) ({ day = day.plusDays(1) }) else null) }
        if (depts.isNotEmpty()) item { DeptChips(depts, dept) { dept = it } }
        if (locked > 0) item { T(stringResource(R.string.hr_locked_note), 13.sp, FontWeight.SemiBold, Brand.Amber) }
        if (active.isEmpty()) item { Muted(stringResource(R.string.hr_no_people), 14.sp) }
        else if (locked == 0) item {
            T(stringResource(R.string.hr_all_present), 13.sp, FontWeight.SemiBold, Brand.Link, modifier = Modifier.clickable {
                scope.launch { active.filter { e -> marks.none { it.employeeId == e.id } }.forEach { app.repo.markAttendance(it.id, day.key(), AttendanceStatus.PRESENT) } }
            }.padding(vertical = 4.dp))
        }
        items(active, key = { it.id }) { e ->
            val mark = marks.firstOrNull { it.employeeId == e.id }?.status
            Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                T(e.name + if (e.shift.isNotBlank()) " · " + e.shift else "", 14.sp, FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statusLabel.forEach { (st, label) ->
                        Chip(stringResource(label), mark == st, {
                            if (locked == 0) scope.launch { app.repo.markAttendance(e.id, day.key(), if (mark == st) null else st) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun Salaries(staffId: Long?) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val now = remember { YearMonth.now() }
    var month by remember { mutableStateOf(now) }
    val people by remember { app.repo.hr.employees() }.collectAsStateWithLifecycle(emptyList())
    val marks by remember(month) { app.repo.hr.month(month.atDay(1).key(), month.atEndOfMonth().key()) }.collectAsStateWithLifecycle(emptyList())
    val paid by remember(month) { app.repo.hr.paid(month.key()) }.collectAsStateWithLifecycle(emptyList())
    var paying by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var message by remember { mutableStateOf<String?>(null) }
    val fmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val category = stringResource(R.string.hr_salary)
    val list = people.filter { it.active || paid.any { p -> p.employeeId == it.id } }
    val advances by remember { app.repo.hr.advances() }.collectAsStateWithLifecycle(emptyList())
    val locked by remember(month) { app.repo.hr.watchLocked(month.key()) }.collectAsStateWithLifecycle(0)
    var advanceMode by remember { mutableStateOf(false) }
    var cut by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Stepper(month.format(fmt), { month = month.minusMonths(1); paying = null }, if (month < now) ({ month = month.plusMonths(1); paying = null }) else null) }
        if (list.isEmpty()) item { Muted(stringResource(R.string.hr_no_people), 14.sp) }
        item {
            val totalLeft = list.sumOf { e ->
                val m = marks.firstOrNull { it.employeeId == e.id }
                (Payroll.earned(e.salaryPaisa, month.lengthOfMonth(), m?.absent ?: 0, m?.half ?: 0) - (paid.firstOrNull { it.employeeId == e.id }?.paidPaisa ?: 0)).coerceAtLeast(0)
            }
            if (list.isNotEmpty()) SumRow(stringResource(R.string.hr_total_left), totalLeft.taka(), strong = true)
        }
        if (list.isNotEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(stringResource(R.string.hr_lock), 14.sp, FontWeight.SemiBold)
                    Muted(stringResource(R.string.hr_lock_sub))
                }
                Toggle(locked > 0, { on -> scope.launch { app.repo.setMonthLocked(month.key(), on) } })
            }
        }
        items(list, key = { it.id }) { e ->
            val m = marks.firstOrNull { it.employeeId == e.id }
            val earned = Payroll.earned(e.salaryPaisa, month.lengthOfMonth(), m?.absent ?: 0, m?.half ?: 0)
            val given = paid.firstOrNull { it.employeeId == e.id }?.paidPaisa ?: 0
            val left = earned - given
            Card(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { paying = if (paying == e.id) null else e.id; amount = ""; cut = ""; advanceMode = false; message = null }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(e.name, 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    T(left.coerceAtLeast(0).taka(), 15.sp, FontWeight.Bold, if (left > 0) Brand.Red else Brand.DarkGreen)
                }
                Muted(stringResource(R.string.hr_month_line, m?.present ?: 0, m?.absent ?: 0, m?.half ?: 0, m?.leave ?: 0))
                SumRow(stringResource(R.string.hr_monthly), e.salaryPaisa.taka())
                SumRow(stringResource(R.string.hr_earned), earned.taka())
                SumRow(stringResource(R.string.hr_paid), given.taka())
                val owesAdvance = (advances.firstOrNull { it.employeeId == e.id }?.paidPaisa ?: 0).coerceAtLeast(0)
                if (owesAdvance > 0) SumRow(stringResource(R.string.hr_advance_left), owesAdvance.taka(), color = Brand.Amber)
                if (paying == e.id) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.hr_salary), !advanceMode, { advanceMode = false })
                        Chip(stringResource(R.string.hr_advance), advanceMode, { advanceMode = true })
                    }
                    if (!advanceMode && owesAdvance > 0) Input(cut, { cut = it }, label = stringResource(R.string.hr_cut_advance), keyboard = KeyboardType.Decimal,
                        placeholder = minOf(owesAdvance, left.coerceAtLeast(0)).taka())
                    Input(amount, { amount = it }, label = stringResource(R.string.cust_amount), keyboard = KeyboardType.Decimal,
                        placeholder = left.coerceAtLeast(0).taka())
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        methods.forEach { (pm, label) -> Chip(stringResource(label), method == pm, { method = pm }) }
                    }
                    GradButton(stringResource(R.string.hr_pay), Brand.BlueGrad, Modifier.fillMaxWidth()) {
                        val cutP = if (advanceMode) 0L else (if (cut.isBlank()) minOf(owesAdvance, left.coerceAtLeast(0)) else cut.toPaisa() ?: 0).coerceIn(0, owesAdvance)
                        val a = if (amount.isBlank()) (if (advanceMode) 0L else (left - cutP).coerceAtLeast(0)) else amount.toPaisa() ?: 0
                        scope.launch {
                            val n = app.repo.paySalary(e.id, month.key(), a, method, category, e.name + " · " + month.format(fmt), staffId,
                                advance = advanceMode, cutAdvancePaisa = cutP)
                            cut = ""; advanceMode = false
                            message = if (n == null) ctx.getString(R.string.cash_bank_bad) else { paying = null; ctx.getString(R.string.hr_paid_saved, n) }
                        }
                    }
                }
            }
        }
        message?.let { item { T(it, 14.sp, FontWeight.SemiBold) } }
        item { Muted(stringResource(R.string.hr_salary_help), 12.sp) }
    }
}

@Composable
private fun People() {
    val app = app()
    val scope = rememberCoroutineScope()
    val people by remember { app.repo.hr.employees() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<EmployeeEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf("") }
    var leave by remember { mutableStateOf("") }
    val year = remember { LocalDate.now().year }
    val taken by remember { app.repo.hr.leaveTaken(year * 10000 + 101, year * 10000 + 1231) }.collectAsStateWithLifecycle(emptyList())
    fun load(e: EmployeeEntity?) {
        editing = e; name = e?.name.orEmpty(); phone = e?.phone.orEmpty(); role = e?.role.orEmpty()
        department = e?.department.orEmpty(); shift = e?.shift.orEmpty(); leave = e?.leaveDays?.takeIf { it > 0 }?.toString().orEmpty()
        salary = e?.salaryPaisa?.takeIf { it > 0 }?.let { (it / 100.0).toBigDecimal().stripTrailingZeros().toPlainString() }.orEmpty()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(people, key = { it.id }) { e ->
            Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { load(e) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        T(e.name, 14.sp, FontWeight.SemiBold)
                        Muted(listOf(e.department, e.role, e.shift, e.phone, e.salaryPaisa.taka()).filter { it.isNotBlank() }.joinToString(" · "))
                        val used = taken.firstOrNull { it.employeeId == e.id }?.paidPaisa ?: 0
                        if (e.leaveDays > 0 || used > 0) T(stringResource(R.string.hr_leave_year, used.toInt(), e.leaveDays), 12.sp,
                            color = if (e.leaveDays > 0 && used > e.leaveDays) Brand.Red else Ui.c.muted)
                    }
                    Toggle(e.active, { on -> scope.launch { app.repo.hr.saveEmployee(e.copy(active = on)) } })
                }
            }
        }
        item {
            Panel(stringResource(if (editing == null) R.string.hr_add_person else R.string.hr_edit_person), Brand.GreenGrad, PanelColors.Green) {
                Input(name, { name = it }, label = stringResource(R.string.promo_name))
                Input(phone, { phone = it }, label = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone)
                Input(role, { role = it }, label = stringResource(R.string.hr_role))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(department, { department = it }, Modifier.weight(1f), label = stringResource(R.string.hr_department))
                    Input(shift, { shift = it }, Modifier.weight(1f), label = stringResource(R.string.hr_shift), placeholder = "9-5")
                }
                Input(leave, { leave = it.filter(Char::isDigit).take(3) }, label = stringResource(R.string.hr_leave_days), keyboard = KeyboardType.Number)
                Input(salary, { salary = it }, label = stringResource(R.string.hr_monthly), keyboard = KeyboardType.Decimal)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradButton(stringResource(R.string.ord_save), Brand.GreenGrad, Modifier.weight(1f), enabled = name.isNotBlank()) {
                        val base = editing ?: EmployeeEntity(name = "")
                        scope.launch {
                            app.repo.hr.saveEmployee(base.copy(name = name.trim(), phone = phone.filter(Char::isDigit), role = role.trim(), salaryPaisa = salary.toPaisa() ?: 0,
                                department = department.trim(), shift = shift.trim(), leaveDays = leave.toIntOrNull() ?: 0))
                            load(null)
                        }
                    }
                    if (editing != null) com.bongshaltech.erpinventory.ui.components.WhiteButton(stringResource(R.string.hr_cancel), Modifier.weight(1f)) { load(null) }
                }
            }
        }
    }
}

@Composable
private fun DeptChips(depts: List<String>, picked: String, pick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(stringResource(R.string.hr_all_depts), picked.isEmpty(), { pick("") })
        depts.forEach { d -> Chip(d, picked == d, { pick(d) }) }
    }
}
