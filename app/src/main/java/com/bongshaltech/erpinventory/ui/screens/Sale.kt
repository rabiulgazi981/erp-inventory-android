package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.CartLine
import com.bongshaltech.erpinventory.data.HeldSale
import com.bongshaltech.erpinventory.data.NewSale
import com.bongshaltech.erpinventory.data.SaleResult
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.SumRow
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tag
import com.bongshaltech.erpinventory.ui.components.Toggle
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import com.bongshaltech.erpinventory.core.sms.SmsCounter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val methods = listOf(
    PayMethod.CASH to R.string.pay_cash, PayMethod.BKASH to R.string.pay_bkash,
    PayMethod.NAGAD to R.string.pay_nagad, PayMethod.CARD to R.string.pay_card,
)

@Composable
private fun SquareAction(icon: ImageVector, grad: Brush, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(grad).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
fun SaleScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (!who.ready) { Box(Modifier.fillMaxSize().background(Ui.c.bg)); return }
    if (!who.can(StaffMenu.SALE)) { NotAllowed(onBack); return }
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val code = settings?.shopCode ?: "ERP"
    val nextNo by remember(code) { app.repo.nextSaleNumber(code) }.collectAsStateWithLifecycle("")

    var q by remember { mutableStateOf("") }
    val matches by remember(q) { app.repo.products.search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    val cart = remember { mutableStateListOf<CartLine>() }
    var discount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var custName by remember { mutableStateOf("") }
    var showName by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var sms by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<SaleResult.Saved?>(null) }
    var held by remember { mutableStateOf(HeldSale.lines.isNotEmpty()) }

    // The per-sale SMS switch starts from the shop's default in More.
    LaunchedEffect(Unit) { sms = app.settings.current().smsDefaultOn }

    val digits = phone.filter(Char::isDigit)
    val customer by produceState<com.bongshaltech.erpinventory.data.db.CustomerEntity?>(null, digits) {
        value = if (digits.length >= 11) app.repo.customers.byPhone(digits) else null
    }
    val subtotal = cart.sumOf { it.totalPaisa }
    val discountPaisa = if (who.can(StaffMenu.DISCOUNT)) (discount.toPaisa() ?: 0).coerceIn(0, subtotal) else 0
    val total = subtotal - discountPaisa
    val paidPaisa = if (paid.isBlank()) total else paid.toPaisa() ?: 0
    val hasPhone = digits.length >= 11

    fun addProduct(p: com.bongshaltech.erpinventory.data.db.ProductEntity) {
        val i = cart.indexOfFirst { it.product.id == p.id }
        if (i >= 0) cart[i] = cart[i].copy(qtyMilli = cart[i].qtyMilli + 1000) else cart.add(CartLine(p, 1000))
        q = ""
    }
    fun change(pId: Long, delta: Long) {
        val i = cart.indexOfFirst { it.product.id == pId }
        if (i < 0) return
        val next = cart[i].qtyMilli + delta
        if (next <= 0) cart.removeAt(i) else cart[i] = cart[i].copy(qtyMilli = next)
    }
    fun reset() {
        cart.clear(); discount = ""; paid = ""; phone = ""; custName = ""; showName = false; method = PayMethod.CASH; message = null
    }

    val notEnough = stringResource(R.string.sale_not_enough, "%s")
    val dueNeeds = stringResource(R.string.sale_due_needs_customer)
    val heldMsg = stringResource(R.string.sale_held)
    val today = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.sale_title), onBack) {
            if (nextNo.isNotEmpty()) Tag(nextNo, Color(0xFF1E40AF), Tints.Blue.brush)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Panel(stringResource(R.string.panel_customer), Brand.BlueGrad, PanelColors.Blue) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Input(phone, { phone = it }, Modifier.weight(1f), placeholder = stringResource(R.string.sale_customer), keyboard = KeyboardType.Phone, height = 42.dp)
                    SquareAction(Icons.Outlined.Add, Brand.BlueGrad, stringResource(R.string.add_customer)) { showName = !showName }
                }
                if (showName || (hasPhone && customer == null && custName.isNotEmpty())) {
                    Input(custName, { custName = it }, placeholder = stringResource(R.string.sale_customer_name), height = 42.dp)
                }
                Row {
                    Muted(
                        customer?.let { "${it.name} · " + stringResource(R.string.prev_due, it.duePaisa.taka0()) } ?: stringResource(R.string.walk_in),
                        modifier = Modifier.weight(1f),
                    )
                    Muted(stringResource(R.string.date_is, today))
                }
            }
            Panel(stringResource(R.string.panel_product), Brand.GreenGrad, PanelColors.Green) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Input(q, { q = it }, Modifier.weight(1f), placeholder = stringResource(R.string.search_product), height = 42.dp)
                    SquareAction(Icons.Outlined.QrCodeScanner, Brand.GreenGrad, stringResource(R.string.scan)) {
                        Toast.makeText(ctx, ctx.getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
                    }
                }
                if (q.isNotBlank()) {
                    if (matches.isEmpty()) Muted(stringResource(R.string.no_match), 13.sp)
                    matches.take(8).forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ui.c.soft).clickable { addProduct(p) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                T(p.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                                Muted(stringResource(R.string.stock_in, p.stockMilli.qty()))
                            }
                            T(p.pricePaisa.taka0(), 14.sp, FontWeight.Bold)
                        }
                    }
                }
                if (cart.isEmpty() && q.isBlank()) Muted(stringResource(R.string.sale_cart_empty), 13.sp)
                val line = Ui.c.divider
                cart.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().drawBehind { drawRect(line, size = size.copy(height = 1.dp.toPx())) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            T(c.product.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            Muted("${c.product.pricePaisa.taka0()} × ${c.qtyMilli.qty()}")
                        }
                        Row(
                            Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Ui.c.inputBorder, RoundedCornerShape(10.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(32.dp).clickable { change(c.product.id, -1000) }, contentAlignment = Alignment.Center) { T("−", 18.sp) }
                            T(c.qtyMilli.qty(), 14.sp, FontWeight.SemiBold, modifier = Modifier.width(24.dp), align = TextAlign.Center)
                            Box(Modifier.size(32.dp).clickable { change(c.product.id, 1000) }, contentAlignment = Alignment.Center) { T("+", 18.sp) }
                        }
                        T(c.totalPaisa.taka0(), 14.sp, FontWeight.Bold, modifier = Modifier.width(72.dp), align = TextAlign.End)
                    }
                }
            }
            Panel(stringResource(R.string.panel_amount), Brand.OrangeGrad, PanelColors.Orange) {
                SumRow(stringResource(R.string.sale_subtotal), subtotal.taka0())
                if (who.can(StaffMenu.DISCOUNT)) Row(verticalAlignment = Alignment.CenterVertically) {
                    T(stringResource(R.string.sale_discount), 14.sp, color = Ui.c.muted, modifier = Modifier.weight(1f))
                    Input(discount, { discount = it }, Modifier.width(90.dp), keyboard = KeyboardType.Decimal, height = 34.dp, alignEnd = true, placeholder = "0")
                }
                Column(
                    Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = Brand.Blue, spotColor = Brand.Blue)
                        .clip(RoundedCornerShape(12.dp)).background(Brand.TotalGrad).padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    T(stringResource(R.string.sale_total), 12.sp, color = Color(0xFFE6EEFF))
                    T(total.taka(), 26.sp, FontWeight.Bold, Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { (m, label) ->
                        val on = m == method
                        val shape = RoundedCornerShape(8.dp)
                        Box(
                            Modifier.weight(1f).height(34.dp).clip(shape)
                                .background(if (on) Color(0xFFFFF7ED) else Ui.c.card)
                                .border(1.dp, if (on) Color(0xFFC2410C) else Ui.c.inputBorder, shape)
                                .clickable { method = m },
                            contentAlignment = Alignment.Center,
                        ) { T(stringResource(label), 13.sp, if (on) FontWeight.SemiBold else FontWeight.Normal, if (on) Color(0xFF9A3412) else Ui.c.label) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(stringResource(R.string.sale_paid), 14.sp, color = Ui.c.muted, modifier = Modifier.weight(1f))
                    Input(paid, { paid = it }, Modifier.width(110.dp), keyboard = KeyboardType.Decimal, height = 34.dp, alignEnd = true, placeholder = total.taka0().removePrefix("৳ "))
                }
                when {
                    paidPaisa < total -> SumRow(stringResource(R.string.sale_due), (total - paidPaisa).taka0(), strong = true, color = Brand.Amber)
                    paidPaisa > total -> SumRow(stringResource(R.string.sale_change), (paidPaisa - total).taka0(), strong = true, color = Brand.DarkGreen)
                }
                val shape = RoundedCornerShape(12.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape)
                        .background(if (sms) Color(0xFFFFF7ED) else Ui.c.soft)
                        .border(1.dp, if (sms) Color(0xFFFED7AA) else Ui.c.border, shape)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    com.bongshaltech.erpinventory.ui.components.TintIcon(Icons.Outlined.ChatBubbleOutline, Tints.Orange, 34.dp, 18.dp, 10.dp)
                    Column(Modifier.weight(1f)) {
                        T(stringResource(R.string.sale_sms), 14.sp, FontWeight.SemiBold, if (sms) Color(0xFF111827) else Color.Unspecified)
                        val parts = SmsCounter.count(smsPreview(settings?.profile?.name.orEmpty(), nextNo, total)).parts
                        Muted(
                            when {
                                !sms -> stringResource(R.string.sms_off_note)
                                !hasPhone -> stringResource(R.string.sale_sms_needs_phone)
                                else -> stringResource(R.string.sms_on_note, phone, parts)
                            }
                        )
                    }
                    Toggle(sms, { sms = it }, 52.dp, 30.dp)
                }
                message?.let { T(it, 13.sp, FontWeight.SemiBold, Brand.Red) }
            }
        }
        BottomBar {
            if (cart.isEmpty() && held) {
                WhiteButton(stringResource(R.string.sale_resume)) { cart.addAll(HeldSale.lines); HeldSale.lines = emptyList(); held = false }
            } else {
                WhiteButton(stringResource(R.string.sale_hold)) {
                    if (cart.isNotEmpty()) {
                        HeldSale.lines = cart.toList(); held = true; reset()
                        Toast.makeText(ctx, heldMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            GradButton(
                stringResource(R.string.sale_save) + if (cart.isNotEmpty()) " · ${total.taka0()}" else "",
                Brand.BlueGrad, Modifier.weight(1f), enabled = cart.isNotEmpty(),
            ) {
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
            }
        }
    }

    saved?.let { s ->
        AlertDialog(
            onDismissRequest = {},
            title = { T(stringResource(R.string.sale_saved, s.number), 18.sp, FontWeight.Bold) },
            text = { T(s.totalPaisa.taka(), 26.sp, FontWeight.Bold, Brand.Blue) },
            confirmButton = { GradButton(stringResource(R.string.sale_new), Brand.BlueGrad, height = 44.dp) { reset(); saved = null } },
            dismissButton = { WhiteButton(stringResource(R.string.sale_done), height = 44.dp) { saved = null; onBack() } },
            containerColor = Ui.c.card,
        )
    }
}

/** What the receipt SMS will say, used to count SMS parts before sending. */
fun smsPreview(shop: String, number: String, totalPaisa: Long) =
    "$shop: Memo $number, total Tk ${totalPaisa / 100}. Thank you!"

