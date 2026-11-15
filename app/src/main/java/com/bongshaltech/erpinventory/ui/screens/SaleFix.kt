package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.DeleteResult
import com.bongshaltech.erpinventory.data.db.InstallmentRow
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Asks why, then deletes the sale (owner) or sends the request to the owner (staff).
 * [onDeleted] runs after a real delete, so the screen can close.
 */
@Composable
fun DeleteSaleDialog(saleId: Long, onClose: () -> Unit, onDeleted: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val required = stringResource(R.string.required)
    val hasReturns = stringResource(R.string.del_has_returns)
    val asked = stringResource(R.string.del_asked)
    val done = stringResource(R.string.del_done)
    val giveBack = stringResource(R.string.del_give_back, "%s")
    AlertDialog(
        onDismissRequest = onClose,
        title = { T(stringResource(if (who.owner) R.string.del_title else R.string.del_ask_title), 18.sp, FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Muted(stringResource(if (who.owner) R.string.del_note else R.string.del_ask_note), 13.sp)
                Input(reason, { reason = it }, label = stringResource(R.string.del_reason), error = required.takeIf { tried && reason.isBlank() })
            }
        },
        confirmButton = {
            GradButton(stringResource(if (who.owner) R.string.del_confirm else R.string.del_ask), Brand.OrangeGrad, height = 44.dp) {
                tried = true
                if (reason.isBlank()) return@GradButton
                scope.launch {
                    when (val r = app.repo.deleteSale(saleId, reason, who.staffId)) {
                        is DeleteResult.Deleted -> {
                            val extra = if (r.manualRefundPaisa > 0) " " + giveBack.format(r.manualRefundPaisa.taka0()) else ""
                            Toast.makeText(ctx, "$done · ${r.number}$extra", Toast.LENGTH_LONG).show()
                            onClose(); onDeleted()
                        }
                        DeleteResult.Requested -> { Toast.makeText(ctx, asked, Toast.LENGTH_LONG).show(); onClose() }
                        DeleteResult.HasReturns -> { Toast.makeText(ctx, hasReturns, Toast.LENGTH_LONG).show(); onClose() }
                        DeleteResult.Gone -> { onClose(); onDeleted() }
                    }
                }
            }
        },
        dismissButton = { WhiteButton(stringResource(R.string.bk_cancel), height = 44.dp, onClick = onClose) },
        containerColor = Ui.c.card,
    )
}

/** More › Deleted sales: staff requests waiting for the owner, and sales already deleted. Owner only. */
@Composable
fun DeletedSalesScreen(go: (String) -> Unit, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val requests by remember { app.repo.saleFix.requests() }.collectAsStateWithLifecycle(emptyList())
    val deleted by remember { app.repo.saleFix.deleted() }.collectAsStateWithLifecycle(emptyList())
    var tab by remember { mutableStateOf(0) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    val done = stringResource(R.string.del_done)
    val hasReturns = stringResource(R.string.del_has_returns)
    val giveBack = stringResource(R.string.del_give_back, "%s")
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.del_sales), onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Segmented(listOf(stringResource(R.string.del_requests_n, requests.size), stringResource(R.string.del_deleted)), tab) { tab = it }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == 0) {
                if (requests.isEmpty()) item { Muted(stringResource(R.string.del_requests_empty)) }
                items(requests, key = { it.id }) { r ->
                    Card(Modifier.fillMaxWidth(), onClick = { go(Routes.receipt(r.saleId)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T(r.number, 14.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                            T(r.totalPaisa.taka0(), 14.sp, FontWeight.Bold)
                        }
                        Muted(listOfNotNull(r.staffName, fmt.format(r.createdAt)).joinToString(" · "))
                        T(r.reason, 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            WhiteButton(stringResource(R.string.del_reject), Modifier.weight(1f), height = 40.dp) { scope.launch { app.repo.rejectDelete(r.saleId) } }
                            GradButton(stringResource(R.string.del_approve), Brand.OrangeGrad, Modifier.weight(1f), height = 40.dp) {
                                scope.launch {
                                    when (val res = app.repo.deleteSale(r.saleId, r.reason, null)) {
                                        is DeleteResult.Deleted -> Toast.makeText(
                                            ctx, "$done · ${res.number}" + if (res.manualRefundPaisa > 0) " " + giveBack.format(res.manualRefundPaisa.taka0()) else "",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        DeleteResult.HasReturns -> Toast.makeText(ctx, hasReturns, Toast.LENGTH_LONG).show()
                                        DeleteResult.Gone -> app.repo.rejectDelete(r.saleId)
                                        DeleteResult.Requested -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (deleted.isEmpty()) item { Muted(stringResource(R.string.del_deleted_empty)) }
                items(deleted, key = { it.id }) { d ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T(d.number, 14.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                            T(d.totalPaisa.taka0(), 14.sp, FontWeight.Bold, Brand.Red)
                        }
                        Muted(stringResource(R.string.del_when, fmt.format(d.saleAt), fmt.format(d.deletedAt)))
                        d.requestedByName?.let { Muted(stringResource(R.string.del_asked_by, it)) }
                        T(d.reason, 13.sp)
                        Muted(d.itemsText)
                    }
                }
            }
        }
    }
}

/** Instalment plan on the customer screen: each payment, when it is due and what is left. */
@Composable
fun InstallmentList(rows: List<InstallmentRow>) {
    if (rows.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    T(stringResource(R.string.installments), 15.sp, FontWeight.Bold)
    Card(Modifier.fillMaxWidth()) {
        rows.forEach { r ->
            val left = (r.amountPaisa - r.paidPaisa).coerceAtLeast(0)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T("${r.saleNumber} · ${r.seq}", 13.sp, FontWeight.SemiBold)
                    Muted(LocalDate.ofEpochDay(r.dueDay).format(fmt))
                }
                when {
                    left == 0L -> T(stringResource(R.string.inst_paid), 13.sp, FontWeight.SemiBold, Brand.DarkGreen)
                    r.dueDay < today -> T(stringResource(R.string.inst_late, left.taka0()), 13.sp, FontWeight.SemiBold, Brand.Red)
                    else -> T(left.taka0(), 13.sp, FontWeight.SemiBold)
                }
            }
        }
    }
}
