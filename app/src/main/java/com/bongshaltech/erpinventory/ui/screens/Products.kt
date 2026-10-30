package com.bongshaltech.erpinventory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.Brush
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
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.ui.components.BottomBar
import com.bongshaltech.erpinventory.ui.components.BottomNav
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.Chip
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.HeaderIcon
import com.bongshaltech.erpinventory.ui.components.Input
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.Tab
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.qty
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui
import com.bongshaltech.erpinventory.ui.toMilli
import com.bongshaltech.erpinventory.ui.toPaisa
import kotlinx.coroutines.launch

@Composable
fun NotAllowed(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header("", onBack)
        Muted(stringResource(R.string.not_allowed), 15.sp, Modifier.padding(24.dp))
    }
}

/** Search box from the design: magnifier, no label, 44dp. */
@Composable
fun SearchBox(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Input(
        value, onChange, modifier, placeholder = placeholder, height = 44.dp,
        leading = { Icon(Icons.Outlined.Search, null, tint = Ui.c.muted, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
fun ProductsScreen(lowOnly: Boolean, go: (String) -> Unit, tab: (Tab) -> Unit) {
    val app = app()
    val who = rememberWho()
    var q by rememberSaveable { mutableStateOf("") }
    var low by rememberSaveable { mutableStateOf(lowOnly) }
    val list by remember(q, low) { if (low) app.repo.products.searchLow(q.trim()) else app.repo.products.search(q.trim()) }
        .collectAsStateWithLifecycle(emptyList())
    val all by remember { app.repo.products.count() }.collectAsStateWithLifecycle(0)
    val lowCount by remember { app.repo.products.lowStockCount() }.collectAsStateWithLifecycle(0)
    val canEdit = who.can(StaffMenu.PRODUCTS)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.products_title)) {
            HeaderIcon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan)) { go(Routes.SOON) }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { SearchBox(q, { q = it }, stringResource(R.string.products_search)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.chip_all, all), !low, { low = false })
                        Chip(stringResource(R.string.chip_low, lowCount), low, { low = true })
                    }
                }
                if (list.isEmpty()) item { Muted(stringResource(if (all == 0) R.string.products_empty else R.string.no_match), 14.sp) }
                items(list, key = { it.id }) { p -> ProductRow(p) { if (canEdit) go(Routes.productEdit(p.id)) } }
            }
            if (canEdit) {
                val shape = CircleShape
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(16.dp).height(52.dp)
                        .shadow(10.dp, shape, ambientColor = Brand.DarkGreen, spotColor = Brand.DarkGreen).clip(shape)
                        .background(Brand.GreenGrad).clickable { go(Routes.productEdit(0)) }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    T(stringResource(R.string.product_add), 15.sp, FontWeight.SemiBold, Color.White)
                }
            }
        }
        BottomNav(Tab.PRODUCTS, tab)
    }
}

@Composable
fun ProductRow(p: ProductEntity, onClick: () -> Unit) {
    val low = p.stockMilli <= p.lowStockMilli
    Card(Modifier.fillMaxWidth(), padding = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Ui.c.soft), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Inventory2, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(p.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Muted(listOf(p.barcode?.let { "SKU $it" }, p.category.ifBlank { null }).filterNotNull().joinToString(" · ").ifEmpty { p.unit })
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(p.pricePaisa.taka0(), 14.sp, FontWeight.Bold)
                T(
                    if (low) stringResource(R.string.stock_left, p.stockMilli.qty()) else stringResource(R.string.stock_in, p.stockMilli.qty()),
                    12.sp, FontWeight.SemiBold, if (low) Brand.Red else Brand.DarkGreen,
                )
            }
        }
    }
}

@Composable
fun ProductEditScreen(id: Long, onDone: () -> Unit) {
    val app = app()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<ProductEntity?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("pcs") }
    var cost by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var lowAt by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val required = stringResource(R.string.required)
    val savedMsg = stringResource(R.string.product_saved)
    val soon = stringResource(R.string.coming_soon)

    LaunchedEffect(id) {
        if (id != 0L) app.repo.products.get(id)?.let { p ->
            loaded = p; name = p.name; barcode = p.barcode.orEmpty(); category = p.category; unit = p.unit
            cost = (p.costPaisa / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()
            price = (p.pricePaisa / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()
            stock = p.stockMilli.qty(); lowAt = p.lowStockMilli.qty()
        }
    }

    fun save(next: Boolean) {
        tried = true
        val pricePaisa = price.toPaisa()
        if (name.isBlank() || pricePaisa == null) return
        val base = loaded ?: ProductEntity(name = "", pricePaisa = 0)
        scope.launch {
            app.repo.saveProduct(
                base.copy(
                    name = name.trim(), barcode = barcode.trim().ifEmpty { null }, category = category.trim(),
                    unit = unit.trim().ifEmpty { "pcs" }, costPaisa = cost.toPaisa() ?: 0, pricePaisa = pricePaisa,
                    stockMilli = stock.toMilli() ?: 0, lowStockMilli = lowAt.toMilli() ?: 0,
                )
            )
            if (next) {
                Toast.makeText(ctx, savedMsg, Toast.LENGTH_SHORT).show()
                loaded = null; name = ""; barcode = ""; cost = ""; price = ""; stock = ""; lowAt = ""; tried = false
            } else onDone()
        }
    }

    val costP = cost.toPaisa()
    val priceP = price.toPaisa()

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(if (loaded == null) R.string.product_add else R.string.product_edit), onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)).background(Ui.c.soft)
                            .border(2.dp, Color(0xFF9CA3AF), RoundedCornerShape(14.dp))
                            .clickable { Toast.makeText(ctx, soon, Toast.LENGTH_SHORT).show() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Icon(Icons.Outlined.CameraAlt, null, tint = Ui.c.muted, modifier = Modifier.size(26.dp))
                        Muted(stringResource(R.string.photo), 11.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T(stringResource(R.string.photo_title), 14.sp, FontWeight.SemiBold)
                        Muted(stringResource(R.string.photo_note))
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Input(name, { name = it }, label = stringResource(R.string.product_name), error = required.takeIf { tried && name.isBlank() })
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Input(barcode, { barcode = it }, Modifier.weight(1f), label = stringResource(R.string.product_barcode))
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Brand.GreenGrad)
                                .clickable { Toast.makeText(ctx, soon, Toast.LENGTH_SHORT).show() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan), tint = Color.White, modifier = Modifier.size(20.dp)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(category, { category = it }, Modifier.weight(1f), label = stringResource(R.string.product_category))
                        Input(unit, { unit = it }, Modifier.weight(1f), label = stringResource(R.string.product_unit))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(cost, { cost = it }, Modifier.weight(1f), label = stringResource(R.string.product_cost), keyboard = KeyboardType.Decimal)
                        Input(
                            price, { price = it }, Modifier.weight(1f), label = stringResource(R.string.product_price), keyboard = KeyboardType.Decimal,
                            error = required.takeIf { tried && priceP == null },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(stock, { stock = it }, Modifier.weight(1f), label = stringResource(R.string.product_stock), keyboard = KeyboardType.Decimal)
                        Input(lowAt, { lowAt = it }, Modifier.weight(1f), label = stringResource(R.string.product_low_at), keyboard = KeyboardType.Decimal)
                    }
                    if (costP != null && priceP != null && costP > 0) {
                        val profit = priceP - costP
                        val pct = profit * 100.0 / costP
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFDCFCE7), Color(0xFFECFCCB))))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            T(stringResource(R.string.profit_per_item), 13.sp, color = Color(0xFF14532D), modifier = Modifier.weight(1f))
                            T("${profit.taka0()} (${"%.1f".format(pct)}%)", 15.sp, FontWeight.Bold, Color(0xFF14532D))
                        }
                    }
                }
            }
            if (loaded != null) T(
                stringResource(R.string.delete), 15.sp, FontWeight.SemiBold, Brand.Red,
                Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp))
                    .clickable { scope.launch { app.repo.products.archive(id); onDone() } }.padding(12.dp),
            )
        }
        BottomBar {
            WhiteButton(stringResource(R.string.save_add_next), height = 52.dp) { save(true) }
            GradButton(stringResource(R.string.product_save), Brand.GreenGrad, Modifier.weight(1f), height = 52.dp) { save(false) }
        }
    }
}
