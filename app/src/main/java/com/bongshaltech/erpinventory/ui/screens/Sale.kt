package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.CartLine
import com.bongshaltech.erpinventory.data.NewSale
import com.bongshaltech.erpinventory.data.SaleResult
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.ui.components.ErpTopBar
import com.bongshaltech.erpinventory.ui.components.Field
import com.bongshaltech.erpinventory.ui.components.SectionRow
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.toPaisa
import kotlinx.coroutines.launch

private val methods = listOf(
    PayMethod.CASH to R.string.pay_cash, PayMethod.BKASH to R.string.pay_bkash,
    PayMethod.NAGAD to R.string.pay_nagad, PayMethod.CARD to R.string.pay_card,
)

@Composable
fun SaleScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    val cart = remember { mutableStateListOf<CartLine>() }
    var discount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var custName by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var sms by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<SaleResult.Saved?>(null) }

    // The per-sale SMS switch starts from the shop's default in Settings.
    LaunchedEffect(Unit) { sms = app.settings.current().smsDefaultOn }

    val subtotal = cart.sumOf { it.totalPaisa }
    val discountPaisa = (discount.toPaisa() ?: 0).coerceIn(0, subtotal)
    val total = subtotal - discountPaisa
    val paidPaisa = if (paid.isBlank()) total else paid.toPaisa() ?: 0
    val hasPhone = phone.count(Char::isDigit) >= 11

    fun add(pId: Long, delta: Long) {
        val i = cart.indexOfFirst { it.product.id == pId }
        if (i >= 0) {
            val next = cart[i].qtyMilli + delta
            if (next <= 0) cart.removeAt(i) else cart[i] = cart[i].copy(qtyMilli = next)
        }
    }

    val notEnough = stringResource(R.string.sale_not_enough, "%s")
    val dueNeeds = stringResource(R.string.sale_due_needs_customer)

    Scaffold(topBar = { ErpTopBar(stringResource(R.string.sale_title), onBack) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Field(q, { q = it }, stringResource(R.string.products_search)) }
            items(matches.take(if (q.isBlank()) 6 else 12), key = { "p${it.id}" }) { p ->
                ProductRow(p) {
                    val i = cart.indexOfFirst { it.product.id == p.id }
                    if (i >= 0) cart[i] = cart[i].copy(qtyMilli = cart[i].qtyMilli + 1000) else cart.add(CartLine(p, 1000))
                    q = ""
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            if (cart.isEmpty()) item { Text(stringResource(R.string.sale_cart_empty), style = MaterialTheme.typography.bodyMedium) }
            items(cart, key = { "c${it.product.id}" }) { line ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brand.Blue.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, style = MaterialTheme.typography.titleSmall)
                        Text("${line.qtyMilli.qty()} × ${line.product.pricePaisa.taka()}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { add(line.product.id, -1000) }) { Icon(Icons.Default.Remove, null) }
                    Text(line.qtyMilli.qty(), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { add(line.product.id, 1000) }) { Icon(Icons.Default.Add, null) }
                    Text(line.totalPaisa.taka(), style = MaterialTheme.typography.titleSmall)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionRow(stringResource(R.string.sale_subtotal), subtotal.taka())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field(discount, { discount = it }, stringResource(R.string.sale_discount), Modifier.weight(1f), number = true)
                        Field(paid, { paid = it }, stringResource(R.string.sale_paid), Modifier.weight(1f), number = true)
                    }
                    SectionRow(stringResource(R.string.sale_total), total.taka(), strong = true)
                    when {
                        paidPaisa < total -> SectionRow(stringResource(R.string.sale_due), (total - paidPaisa).taka(), color = Brand.Danger)
                        paidPaisa > total -> SectionRow(stringResource(R.string.sale_change), (paidPaisa - total).taka(), color = Brand.Green)
                    }
                    Field(phone, { phone = it }, stringResource(R.string.sale_customer), phone = true)
                    if (phone.isNotBlank()) Field(custName, { custName = it }, stringResource(R.string.sale_customer_name))
                    Text(stringResource(R.string.sale_method), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        methods.forEach { (m, label) ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(stringResource(label)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.sale_sms), style = MaterialTheme.typography.titleSmall)
                            if (sms && !hasPhone) Text(stringResource(R.string.sale_sms_needs_phone), style = MaterialTheme.typography.bodySmall, color = Brand.Orange)
                        }
                        Switch(checked = sms, onCheckedChange = { sms = it })
                    }
                    message?.let { Text(it, color = Brand.Danger) }
                    Button(
                        enabled = cart.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                val r = app.repo.saveSale(
                                    NewSale(
                                        lines = cart.toList(), discountPaisa = discountPaisa, paidPaisa = paidPaisa, method = method,
                                        customerPhone = phone, customerName = custName, smsRequested = sms && hasPhone, staffId = who.staffId,
                                    )
                                )
                                when (r) {
                                    is SaleResult.Saved -> { saved = r; message = null }
                                    is SaleResult.NotEnoughStock -> message = notEnough.replace("%s", r.productName)
                                    SaleResult.DueNeedsCustomer -> message = dueNeeds
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) { Text(stringResource(R.string.sale_save) + if (cart.isNotEmpty()) " · ${total.taka()}" else "") }
                }
            }
        }
    }

    saved?.let { s ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.sale_saved, s.number)) },
            text = { Text(s.totalPaisa.taka(), style = MaterialTheme.typography.headlineSmall) },
            confirmButton = {
                Button(onClick = {
                    cart.clear(); discount = ""; paid = ""; phone = ""; custName = ""; method = PayMethod.CASH; saved = null
                }) { Text(stringResource(R.string.sale_new)) }
            },
            dismissButton = { TextButton(onClick = { saved = null; onBack() }) { Text(stringResource(R.string.sale_done)) } },
        )
    }
}
