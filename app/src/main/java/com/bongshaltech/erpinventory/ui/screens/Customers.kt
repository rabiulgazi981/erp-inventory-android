package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bongshaltech.erpinventory.data.db.CustomerEntity
import com.bongshaltech.erpinventory.data.db.CustomerTxn
import com.bongshaltech.erpinventory.data.db.CustomerType
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
import com.bongshaltech.erpinventory.ui.components.Initials
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tag
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** List filter: every customer, one type, or only those who owe money. */
private enum class CustFilter { ALL, RETAIL, WHOLESALE, DUE }

@Composable
fun CustomersScreen(dueOnly: Boolean, go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.can(StaffMenu.CUSTOMERS)) return NotAllowed(onBack)
    var q by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(if (dueOnly) CustFilter.DUE else CustFilter.ALL) }
    val type = when (filter) { CustFilter.RETAIL -> CustomerType.RETAIL.name; CustFilter.WHOLESALE -> CustomerType.WHOLESALE.name; else -> "" }
    val list by remember(q, filter) { app.repo.customers.search(q.trim(), type, filter == CustFilter.DUE) }
        .collectAsStateWithLifecycle(emptyList())
    val all by remember { app.repo.customers.count() }.collectAsStateWithLifecycle(0)
    val totalDue by remember { app.repo.customers.totalDue() }.collectAsStateWithLifecycle(0L)
    val dueCount by remember { app.repo.customers.dueCount() }.collectAsStateWithLifecycle(0)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.home_customers), onBack)
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Muted(stringResource(R.string.home_due))
                                T(totalDue.taka0(), 22.sp, FontWeight.Bold, if (totalDue > 0) Brand.Amber else Ui.c.text)
                            }
                            Muted(stringResource(R.string.home_due_customers, dueCount))
                        }
                    }
                }
                item { SearchBox(q, { q = it }, stringResource(R.string.cust_search)) }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.chip_all, all), filter == CustFilter.ALL, { filter = CustFilter.ALL })
                        Chip(stringResource(R.string.cust_retail), filter == CustFilter.RETAIL, { filter = CustFilter.RETAIL })
                        Chip(stringResource(R.string.cust_wholesale), filter == CustFilter.WHOLESALE, { filter = CustFilter.WHOLESALE })
                        Chip(stringResource(R.string.cust_due_only, dueCount), filter == CustFilter.DUE, { filter = CustFilter.DUE })
                    }
                }
                if (list.isEmpty()) item { Muted(stringResource(if (all == 0) R.string.cust_empty else R.string.cust_no_match), 14.sp) }
                items(list, key = { it.id }) { c -> CustomerRow(c) { go(Routes.customer(c.id)) } }
            }
            val shape = CircleShape
            Row(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).height(52.dp)
                    .shadow(10.dp, shape, ambientColor = Brand.Blue, spotColor = Brand.Blue).clip(shape)
                    .background(Brand.BlueGrad).clickable { go(Routes.customerEdit(0)) }.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.PersonAdd, null, tint = Color.White, modifier = Modifier.size(20.dp))
                T(stringResource(R.string.add_customer), 15.sp, FontWeight.SemiBold, Color.White, shadow = true)
            }
        }
    }
}

@Composable
private fun TypeTag(type: String) {
    val wholesale = type == CustomerType.WHOLESALE.name
    val tint = if (wholesale) Tints.Purple else Tints.Blue
    Tag(stringResource(if (wholesale) R.string.cust_wholesale else R.string.cust_retail), tint.fg, tint.brush)
}

@Composable
private fun CustomerRow(c: CustomerEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Initials(c.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(c.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(c.phone)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (c.duePaisa > 0) T(stringResource(R.string.cust_due_amount, c.duePaisa.taka0()), 13.sp, FontWeight.Bold, Brand.Red)
                else T(stringResource(R.string.cust_no_due), 12.sp, FontWeight.SemiBold, Brand.DarkGreen)
                TypeTag(c.type)
            }
        }
    }
}

@Composable
fun CustomerDetailScreen(id: Long, go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    val scope = rememberCoroutineScope()
    val c by remember(id) { app.repo.customers.watch(id) }.collectAsStateWithLifecycle(null)
    val history by remember(id) { app.repo.customers.history(id) }.collectAsStateWithLifecycle(emptyList())
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf(PayMethod.CASH) }
    var note by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.cust_payment_saved)
    val tooMuch = stringResource(R.string.cust_amount_too_much)
    val required = stringResource(R.string.required)
    if (who.ready && !who.can(StaffMenu.CUSTOMERS)) return NotAllowed(onBack)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(c?.name.orEmpty(), onBack) {
            HeaderIcon(Icons.Outlined.Edit, stringResource(R.string.cust_edit)) { go(Routes.customerEdit(id)) }
        }
        val cust = c ?: return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Initials(cust.name, 52.dp, 18.sp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T(cust.name, 16.sp, FontWeight.Bold, maxLines = 1)
                        Muted(listOf(cust.phone, cust.address).filter { it.isNotBlank() }.joinToString(" · "))
                        TypeTag(cust.type)
                    }
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Brand.GreenGrad)
                            .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))) } },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Call, stringResource(R.string.cust_call), tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Muted(stringResource(R.string.cust_current_due), 14.sp, Modifier.weight(1f))
                    T(cust.duePaisa.taka0(), 22.sp, FontWeight.Bold, if (cust.duePaisa > 0) Brand.Red else Brand.DarkGreen)
                }
            }
            if (cust.duePaisa > 0) Panel(stringResource(R.string.home_collect_due), Brand.GreenGrad, PanelColors.Green) {
                val amountP = amount.toPaisa()
                Input(
                    amount, { amount = it }, label = stringResource(R.string.cust_amount), keyboard = KeyboardType.Decimal,
                    placeholder = cust.duePaisa.taka0(),
                    error = when {
                        tried && (amountP == null || amountP <= 0) -> required
                        amountP != null && amountP > cust.duePaisa -> tooMuch
                        else -> null
                    },
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (m, label) -> Chip(stringResource(label), method == m, { method = m }) }
                }
                Input(note, { note = it }, label = stringResource(R.string.cust_note))
                GradButton(stringResource(R.string.cust_take_payment), Brand.GreenGrad, Modifier.fillMaxWidth()) {
                    tried = true
                    val p = amount.toPaisa()
                    if (p != null && p > 0 && p <= cust.duePaisa) scope.launch {
                        val number = app.repo.collectDue(cust.id, p, method, note, who.staffId)
                        if (number != null) {
                            Toast.makeText(ctx, "$savedMsg · $number", Toast.LENGTH_SHORT).show()
                            amount = ""; note = ""; method = PayMethod.CASH; tried = false
                        }
                    }
                }
            }
            T(stringResource(R.string.cust_history), 15.sp, FontWeight.Bold)
            if (history.isEmpty()) Muted(stringResource(R.string.cust_history_empty))
            history.forEach { HistoryRow(it) }
        }
    }
}

@Composable
private fun HistoryRow(t: CustomerTxn) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(stringResource(if (t.isPayment) R.string.cust_txn_payment else R.string.cust_txn_sale) + " · " + t.number, 13.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(fmt.format(Date(t.createdAt)))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T((if (t.isPayment) "− " else "") + t.amountPaisa.taka0(), 14.sp, FontWeight.Bold, if (t.isPayment) Brand.DarkGreen else Ui.c.text)
                if (!t.isPayment && t.duePaisa > 0) T(stringResource(R.string.cust_due_amount, t.duePaisa.taka0()), 12.sp, FontWeight.SemiBold, Brand.Red)
            }
        }
    }
}

@Composable
fun CustomerEditScreen(id: Long, onDone: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<CustomerEntity?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(CustomerType.RETAIL) }
    var tried by remember { mutableStateOf(false) }
    val required = stringResource(R.string.required)
    val taken = stringResource(R.string.cust_phone_taken)

    LaunchedEffect(id) {
        if (id != 0L) app.repo.customers.get(id)?.let { c ->
            loaded = c; name = c.name; phone = c.phone; address = c.address
            type = runCatching { CustomerType.valueOf(c.type) }.getOrDefault(CustomerType.RETAIL)
        }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(if (id == 0L) R.string.add_customer else R.string.cust_edit), onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Panel(stringResource(R.string.panel_customer), Brand.BlueGrad, PanelColors.Blue) {
                Input(name, { name = it }, label = stringResource(R.string.sale_customer_name), error = required.takeIf { tried && name.isBlank() })
                Input(
                    phone, { phone = it.filter(Char::isDigit).take(14) }, label = stringResource(R.string.onb_phone), keyboard = KeyboardType.Phone,
                    error = required.takeIf { tried && phone.length < 6 },
                )
                Input(address, { address = it }, label = stringResource(R.string.cust_address))
                T(stringResource(R.string.cust_type), 13.sp, FontWeight.SemiBold, Ui.c.label)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.cust_retail), type == CustomerType.RETAIL, { type = CustomerType.RETAIL }, soft = true)
                    Chip(stringResource(R.string.cust_wholesale), type == CustomerType.WHOLESALE, { type = CustomerType.WHOLESALE }, soft = true)
                }
            }
        }
        BottomBar {
            GradButton(stringResource(R.string.cust_save), Brand.BlueGrad, Modifier.weight(1f), height = 52.dp) {
                tried = true
                if (name.isBlank() || phone.length < 6) return@GradButton
                val base = loaded ?: CustomerEntity(name = "", phone = "")
                scope.launch {
                    if (app.repo.saveCustomer(base.copy(name = name, phone = phone, address = address, type = type.name))) onDone()
                    else Toast.makeText(ctx, taken, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
