package com.bongshaltech.erpinventory.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.BusinessProfile
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.print.BtPrinter
import com.bongshaltech.erpinventory.print.EscPos
import com.bongshaltech.erpinventory.print.PairedPrinter
import com.bongshaltech.erpinventory.print.PrintResult
import com.bongshaltech.erpinventory.print.ReceiptData
import com.bongshaltech.erpinventory.print.ReceiptLabels
import com.bongshaltech.erpinventory.print.ReceiptLine
import com.bongshaltech.erpinventory.print.ReceiptRenderer
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun labels(ctx: Context) = ReceiptLabels(
    memo = ctx.getString(R.string.rc_memo), date = ctx.getString(R.string.rc_date), customer = ctx.getString(R.string.rc_customer),
    subtotal = ctx.getString(R.string.rc_subtotal), discount = ctx.getString(R.string.rc_discount), total = ctx.getString(R.string.rc_total),
    paid = ctx.getString(R.string.rc_paid), due = ctx.getString(R.string.rc_due), method = "",
)

private fun methodName(ctx: Context, m: PayMethod) = ctx.getString(methods.firstOrNull { it.first == m }?.second ?: R.string.pay_cash)

/** Builds the memo for one saved sale. */
private suspend fun loadReceipt(ctx: Context, saleId: Long, profile: BusinessProfile): ReceiptData? {
    val repo = (ctx.applicationContext as com.bongshaltech.erpinventory.ErpApp).repo
    val s = repo.sales.get(saleId) ?: return null
    val items = repo.sales.items(saleId)
    val cust = s.customerId?.let { repo.customers.get(it) }
    val due = s.totalPaisa - s.paidPaisa
    return ReceiptData(
        profile = profile,
        number = s.number,
        dateText = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(s.createdAt)),
        customer = cust?.let { c -> if (c.name == c.phone) c.phone else "${c.name} (${c.phone})" },
        lines = items.map { ReceiptLine(it.name, it.qtyMilli.qty(), it.unitPricePaisa.taka(), it.lineTotalPaisa.taka()) },
        subtotal = s.subtotalPaisa.taka(),
        discount = s.discountPaisa.takeIf { it > 0 }?.taka(),
        total = s.totalPaisa.taka(),
        paid = s.paidPaisa.taka(),
        due = due.takeIf { it > 0 }?.taka(),
        method = methodName(ctx, s.method),
    )
}

/** A short sample memo for the printer test. */
private fun sampleReceipt(ctx: Context, profile: BusinessProfile) = ReceiptData(
    profile = profile, number = "TEST", dateText = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
    customer = null, lines = listOf(ReceiptLine(ctx.getString(R.string.printer_test_item), "1", 100_00L.taka(), 100_00L.taka())),
    subtotal = 100_00L.taka(), discount = null, total = 100_00L.taka(), paid = 100_00L.taka(), due = null,
    method = methodName(ctx, PayMethod.CASH),
)

/** Shows Bangla-safe printer messages as a toast. */
private fun toastResult(ctx: Context, r: PrintResult) {
    val msg = when (r) {
        PrintResult.Done -> ctx.getString(R.string.printer_done)
        PrintResult.BluetoothOff -> ctx.getString(R.string.printer_bt_off)
        PrintResult.NoBluetooth -> ctx.getString(R.string.printer_no_bt)
        is PrintResult.Failed -> ctx.getString(R.string.printer_failed)
    }
    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
}

private fun shareImage(ctx: Context, bmp: Bitmap, name: String) {
    val dir = File(ctx.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "$name.png")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, null))
}

/**
 * Printer state shared by the receipt and printer screens: asks for the Bluetooth permission
 * when needed, lets the shopkeeper pick a paired printer, then prints.
 */
private class PrinterFlow(val ctx: Context) {
    var picking by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var paired by mutableStateOf(emptyList<PairedPrinter>())
    fun refresh() { paired = BtPrinter.paired(ctx) }
}

@Composable
private fun rememberPrinterFlow(): Pair<PrinterFlow, (after: () -> Unit) -> Unit> {
    val ctx = LocalContext.current
    val flow = remember { PrinterFlow(ctx) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { flow.refresh(); pending?.invoke() } else Toast.makeText(ctx, ctx.getString(R.string.printer_perm), Toast.LENGTH_LONG).show()
        pending = null
    }
    val withPermission: (() -> Unit) -> Unit = { after ->
        val p = BtPrinter.permission
        if (p == null || BtPrinter.hasPermission(ctx)) { flow.refresh(); after() } else { pending = after; ask.launch(p) }
    }
    return flow to withPermission
}

@Composable
private fun PrinterPicker(flow: PrinterFlow, onPick: (PairedPrinter) -> Unit) {
    if (!flow.picking) return
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = { flow.picking = false },
        title = { T(stringResource(R.string.printer_choose), 18.sp, FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (flow.paired.isEmpty()) Muted(stringResource(R.string.printer_pair_first), 14.sp)
                flow.paired.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ui.c.soft)
                            .clickable { flow.picking = false; onPick(p) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            T(p.name, 15.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted(p.address, 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            WhiteButton(stringResource(R.string.printer_open_bt), height = 42.dp) {
                flow.picking = false
                runCatching { ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            }
        },
        containerColor = Ui.c.card,
    )
}

@Composable
private fun PrinterPanel(flow: PrinterFlow, withPermission: (() -> Unit) -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val s = settings ?: return
    Panel(stringResource(R.string.printer), Brand.BlueGrad, PanelColors.Blue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T(s.printerName.ifBlank { stringResource(R.string.printer_none) }, 15.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(stringResource(R.string.printer_note), 12.sp)
            }
            T(
                stringResource(R.string.printer_choose), 13.sp, FontWeight.SemiBold, Brand.Link,
                Modifier.clickable { withPermission { flow.picking = true } }.padding(8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(stringResource(R.string.printer_paper), 14.sp, modifier = Modifier.weight(1f))
            Segmented(listOf("58 mm", "80 mm"), if (s.paperMm >= 80) 1 else 0) { i ->
                scope.launch { app.settings.setPaper(if (i == 1) 80 else 58) }
            }
        }
    }
    PrinterPicker(flow) { p -> scope.launch { app.settings.setPrinter(p.address, p.name) } }
}

/** Prints [bmp] on the chosen printer, asking for one first if none is set. */
private fun printNow(
    ctx: Context, scope: kotlinx.coroutines.CoroutineScope, flow: PrinterFlow,
    withPermission: (() -> Unit) -> Unit, address: String, paperMm: Int, bmp: Bitmap?,
) {
    if (bmp == null || flow.busy) return
    withPermission {
        if (address.isBlank()) flow.picking = true
        else {
            flow.busy = true
            scope.launch {
                val bytes = withContext(Dispatchers.Default) { EscPos.image(bmp, cut = paperMm >= 80) }
                toastResult(ctx, BtPrinter.print(ctx, address, bytes))
                flow.busy = false
            }
        }
    }
}

@Composable
fun ReceiptScreen(saleId: Long, onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val (flow, withPermission) = rememberPrinterFlow()
    var number by remember { mutableStateOf("") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    val paper = settings?.paperMm ?: 58
    val profile = settings?.profile

    LaunchedEffect(paper, profile) {
        val p = profile ?: return@LaunchedEffect
        val d = loadReceipt(ctx, saleId, p) ?: return@LaunchedEffect
        number = d.number
        bmp = withContext(Dispatchers.Default) { ReceiptRenderer.render(d, labels(ctx), ReceiptRenderer.widthFor(paper)) }
    }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.receipt_title) + if (number.isNotEmpty()) " · $number" else "", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrinterPanel(flow, withPermission)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                bmp?.let {
                    Image(
                        it.asImageBitmap(), null,
                        Modifier.widthIn(max = 340.dp).fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Ui.c.border, RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }
        BottomBar {
            WhiteButton(stringResource(R.string.receipt_share)) { bmp?.let { shareImage(ctx, it, number.ifBlank { "receipt" }) } }
            GradButton(
                stringResource(if (flow.busy) R.string.printer_printing else R.string.receipt_print),
                Brand.BlueGrad, Modifier.weight(1f), enabled = bmp != null && !flow.busy,
            ) {
                printNow(ctx, scope, flow, withPermission, settings?.printerAddress.orEmpty(), paper, bmp)
            }
        }
    }
}

/** More › Receipt printer: choose the printer and paper, and print a test memo. */
@Composable
fun PrinterScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val (flow, withPermission) = rememberPrinterFlow()
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.printer), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrinterPanel(flow, withPermission)
            Muted(stringResource(R.string.printer_help), 13.sp)
        }
        BottomBar {
            GradButton(
                stringResource(if (flow.busy) R.string.printer_printing else R.string.printer_test),
                Brand.BlueGrad, Modifier.weight(1f), enabled = settings != null && !flow.busy,
            ) {
                val s = settings ?: return@GradButton
                val bmp = ReceiptRenderer.render(sampleReceipt(ctx, s.profile), labels(ctx), ReceiptRenderer.widthFor(s.paperMm))
                printNow(ctx, scope, flow, withPermission, s.printerAddress, s.paperMm, bmp)
            }
        }
    }
}
