package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocalOffer
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
import com.bongshaltech.erpinventory.data.db.PayMethod
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.PromoEntity
import com.bongshaltech.erpinventory.data.db.PromoKind
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Panel
import com.bongshaltech.erpinventory.ui.components.PanelColors
import com.bongshaltech.erpinventory.ui.components.Segmented
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.components.Toggle
import com.bongshaltech.erpinventory.ui.taka
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tints
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toPaisa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val DAY = 24L * 60 * 60 * 1000

private fun PromoEntity.valueText(): String = when {
    kind == PromoKind.VOUCHER -> balancePaisa.taka()
    percentBp > 0 -> (percentBp / 100.0).toBigDecimal().stripTrailingZeros().toPlainString() + "%"
    else -> fixedPaisa.taka()
}

/** More › Offers & coupons: automatic offers, coupon codes and gift vouchers. Owner only. */
@Composable
fun PromosScreen(onBack: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    val kind = PromoKind.entries[tab]
    val list by remember(kind) { app.repo.promos.byKind(kind) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var percent by remember { mutableStateOf(0) }
    var value by remember { mutableStateOf("") }
    var scope0 by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf("") }
    var productQ by remember { mutableStateOf("") }
    var product by remember { mutableStateOf<ProductEntity?>(null) }
    var minBill by remember { mutableStateOf("") }
    var maxUses by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var soldFor by remember { mutableStateOf<PayMethod?>(PayMethod.CASH) }
    var message by remember { mutableStateOf<String?>(null) }
    val products by remember(productQ) { app.repo.products.search(productQ.trim()) }.collectAsStateWithLifecycle(emptyList())

    fun clear() { name = ""; code = ""; value = ""; category = ""; productQ = ""; product = null; minBill = ""; maxUses = ""; days = "" }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.promos), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Segmented(
                    listOf(stringResource(R.string.promo_offers), stringResource(R.string.promo_coupons), stringResource(R.string.promo_vouchers)),
                    tab, { tab = it; message = null; clear() },
                )
            }
            item {
                Muted(stringResource(when (kind) {
                    PromoKind.OFFER -> R.string.promo_offers_help
                    PromoKind.COUPON -> R.string.promo_coupons_help
                    PromoKind.VOUCHER -> R.string.promo_vouchers_help
                }), 12.sp)
            }
            if (list.isEmpty()) item { Muted(stringResource(R.string.cust_history_empty), 14.sp) }
            items(list, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TintIcon(
                            when (p.kind) { PromoKind.OFFER -> Icons.Outlined.LocalOffer; PromoKind.COUPON -> Icons.Outlined.ConfirmationNumber; else -> Icons.Outlined.CardGiftcard },
                            if (p.active) Tints.Green else Tints.Grey, 40.dp, 20.dp, 10.dp,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            T(p.name + if (p.code.isNotEmpty() && p.code != p.name) " · ${p.code}" else "", 14.sp, FontWeight.SemiBold, maxLines = 1)
                            val bits = buildList {
                                add(p.valueText())
                                if (p.kind == PromoKind.OFFER) add(if (p.productId != null) ctx.getString(R.string.promo_one_product) else p.category.ifBlank { ctx.getString(R.string.promo_all) })
                                if (p.minPaisa > 0) add(ctx.getString(R.string.promo_min_short, p.minPaisa.taka()))
                                if (p.kind != PromoKind.OFFER) add(ctx.getString(R.string.promo_used, p.uses) + if (p.maxUses > 0) "/${p.maxUses}" else "")
                                p.endAt?.let { add(ctx.getString(R.string.promo_until, fmt.format(Date(it)))) }
                            }
                            Muted(bits.joinToString(" · "))
                        }
                        Toggle(p.active, { on -> scope.launch { app.repo.promos.setActive(p.id, on) } })
                    }
                }
            }
            item {
                Panel(stringResource(R.string.promo_new), Brand.GreenGrad, PanelColors.Green) {
                    Input(name, { name = it }, label = stringResource(R.string.promo_name))
                    if (kind != PromoKind.OFFER) Input(code, { code = it.uppercase() }, label = stringResource(R.string.promo_code))
                    if (kind != PromoKind.VOUCHER) Segmented(listOf(stringResource(R.string.promo_percent), stringResource(R.string.promo_taka)), percent, { percent = it })
                    Input(value, { value = it }, label = stringResource(
                        if (kind == PromoKind.VOUCHER) R.string.promo_voucher_value else if (percent == 0) R.string.promo_percent_value
                        else if (kind == PromoKind.OFFER) R.string.promo_taka_per_unit else R.string.promo_taka_value
                    ), keyboard = KeyboardType.Decimal)
                    if (kind == PromoKind.OFFER) {
                        Segmented(listOf(stringResource(R.string.promo_all), stringResource(R.string.promo_category), stringResource(R.string.promo_one_product)), scope0, { scope0 = it })
                        if (scope0 == 1) Input(category, { category = it }, label = stringResource(R.string.promo_category))
                        if (scope0 == 2) {
                            val picked = product
                            if (picked != null) Row(verticalAlignment = Alignment.CenterVertically) {
                                T(picked.name, 14.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                T("✕", 14.sp, color = Ui.c.muted, modifier = Modifier.clickable { product = null }.padding(8.dp))
                            } else {
                                Input(productQ, { productQ = it }, placeholder = stringResource(R.string.search_product))
                                if (productQ.isNotBlank()) products.take(5).forEach { pr ->
                                    T(pr.name, 14.sp, modifier = Modifier.fillMaxWidth().clickable { product = pr; productQ = "" }.padding(vertical = 6.dp))
                                }
                            }
                        }
                    }
                    if (kind == PromoKind.COUPON) {
                        Input(minBill, { minBill = it }, label = stringResource(R.string.promo_min_bill), keyboard = KeyboardType.Decimal)
                        Input(maxUses, { maxUses = it }, label = stringResource(R.string.promo_max_uses), keyboard = KeyboardType.Number)
                    }
                    Input(days, { days = it }, label = stringResource(R.string.promo_days), keyboard = KeyboardType.Number)
                    if (kind == PromoKind.VOUCHER) {
                        Muted(stringResource(R.string.promo_sold_for))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(stringResource(R.string.promo_free_gift), soldFor == null, { soldFor = null })
                            methods.take(3).forEach { (m, label) -> Chip(stringResource(label), soldFor == m, { soldFor = m }) }
                        }
                    }
                    GradButton(stringResource(R.string.ord_save), Brand.GreenGrad, Modifier.fillMaxWidth()) {
                        val now = System.currentTimeMillis()
                        val v = value.toPaisa() ?: 0
                        val p = PromoEntity(
                            kind = kind, name = name, code = code,
                            percentBp = if (kind != PromoKind.VOUCHER && percent == 0) v.toInt().coerceIn(0, 10000) else 0,
                            fixedPaisa = if (kind != PromoKind.VOUCHER && percent == 1) v else 0,
                            productId = if (kind == PromoKind.OFFER && scope0 == 2) product?.id else null,
                            category = if (kind == PromoKind.OFFER && scope0 == 1) category else "",
                            minPaisa = if (kind == PromoKind.COUPON) minBill.toPaisa() ?: 0 else 0,
                            balancePaisa = if (kind == PromoKind.VOUCHER) v else 0,
                            startAt = now,
                            endAt = days.toIntOrNull()?.takeIf { it > 0 }?.let { now + it * DAY },
                            maxUses = if (kind == PromoKind.COUPON) maxUses.toIntOrNull() ?: 0 else if (kind == PromoKind.VOUCHER) 0 else 0,
                        )
                        val bad = (kind == PromoKind.OFFER && name.isBlank()) || (kind == PromoKind.OFFER && scope0 == 2 && product == null) ||
                            (kind == PromoKind.OFFER && scope0 == 1 && category.isBlank())
                        if (bad) { message = ctx.getString(R.string.promo_bad); return@GradButton }
                        scope.launch {
                            val ok = app.repo.savePromo(p, if (kind == PromoKind.VOUCHER) soldFor else null, who.staffId)
                            message = if (ok) { clear(); ctx.getString(R.string.ord_saved) } else ctx.getString(R.string.promo_bad)
                        }
                    }
                    message?.let { T(it, 14.sp, FontWeight.SemiBold) }
                }
            }
        }
    }
}
