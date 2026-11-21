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
import androidx.compose.runtime.mutableStateListOf
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
import com.bongshaltech.erpinventory.ui.scanBarcode
import com.bongshaltech.erpinventory.data.StaffMenu
import com.bongshaltech.erpinventory.data.db.ProductEntity
import com.bongshaltech.erpinventory.data.db.search
import com.bongshaltech.erpinventory.data.db.searchLow
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
    val ctx = LocalContext.current
    var q by rememberSaveable { mutableStateOf("") }
    var low by rememberSaveable { mutableStateOf(lowOnly) }
    val list by remember(q, low) { if (low) app.repo.products.searchLow(q.trim()) else app.repo.products.search(q.trim()) }
        .collectAsStateWithLifecycle(emptyList())
    val all by remember { app.repo.products.count() }.collectAsStateWithLifecycle(0)
    val lowCount by remember { app.repo.products.lowStockCount() }.collectAsStateWithLifecycle(0)
    val canEdit = who.can(StaffMenu.PRODUCTS)

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.products_title)) {
            HeaderIcon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan)) { scanBarcode(ctx) { q = it } }
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
        com.bongshaltech.erpinventory.ads.AdBanner()
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
                Muted(listOf(p.code.ifBlank { null }, p.barcode?.let { "SKU $it" }, p.category.ifBlank { null }).filterNotNull().joinToString(" · ").ifEmpty { p.unit })
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
    var generic by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("pcs") }
    var cost by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var lowAt by rememberSaveable { mutableStateOf("") }
    var packSize by rememberSaveable { mutableStateOf("") }
    var packName by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    // Single product, or a style with size / colour variants (each its own price, barcode and stock).
    var variantMode by rememberSaveable { mutableStateOf(false) }
    var sizes by rememberSaveable { mutableStateOf("") }
    var colors by rememberSaveable { mutableStateOf("") }
    val vrows = remember { mutableStateListOf<VRow>() }
    var variantMsg by remember { mutableStateOf<String?>(null) }
    val required = stringResource(R.string.required)
    val savedMsg = stringResource(R.string.product_saved)
    val soon = stringResource(R.string.coming_soon)
    val st = app.settings.settings.collectAsStateWithLifecycle(null).value
    val pharmacy = st?.shopType == com.bongshaltech.erpinventory.data.ShopType.PHARMACY
    val packsOn = st?.on(com.bongshaltech.erpinventory.data.Feature.PACKS) == true

    LaunchedEffect(id) {
        if (id != 0L) app.repo.products.get(id)?.let { p ->
            if (p.groupId != 0L) {
                variantMode = true
                vrows.clear()
                app.repo.products.variantsNow(p.groupId).filter { it.active }.forEach { v -> vrows.add(VRow.of(v)) }
            }
            loaded = p; name = com.bongshaltech.erpinventory.core.stock.Variants.baseOf(p.name, p.variant); barcode = p.barcode.orEmpty(); category = p.category; generic = p.generic; unit = p.unit
            cost = (p.costPaisa / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()
            price = (p.pricePaisa / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()
            stock = p.stockMilli.qty(); lowAt = p.lowStockMilli.qty()
            packSize = if (p.packMilli > 0) p.packMilli.qty() else ""; packName = p.packName
        }
    }

    fun save(next: Boolean) {
        tried = true
        if (variantMode) {
            val rows = vrows.map { it.toRow() }
            if (name.isBlank() || rows.isEmpty() || vrows.any { it.price.toPaisa() == null || it.variant.isBlank() }) { variantMsg = ctx.getString(R.string.var_need_rows); return }
            val base = (loaded ?: ProductEntity(name = "", pricePaisa = 0)).copy(
                name = name.trim(), category = com.bongshaltech.erpinventory.core.stock.Categories.join(com.bongshaltech.erpinventory.core.stock.Categories.split(category)),
                generic = generic.trim(), unit = unit.trim().ifEmpty { "pcs" }, packMilli = (packSize.toMilli() ?: 0).coerceAtLeast(0), packName = packName.trim(),
            )
            scope.launch {
                if (app.repo.saveVariants(base, rows) == null) { variantMsg = ctx.getString(R.string.var_need_rows); return@launch }
                if (next) {
                    Toast.makeText(ctx, savedMsg, Toast.LENGTH_SHORT).show()
                    loaded = null; name = ""; vrows.clear(); sizes = ""; colors = ""; tried = false; variantMsg = null
                } else onDone()
            }
            return
        }
        val pricePaisa = price.toPaisa()
        if (name.isBlank() || pricePaisa == null) return
        val base = loaded ?: ProductEntity(name = "", pricePaisa = 0)
        scope.launch {
            app.repo.saveProduct(
                base.copy(
                    name = name.trim(), barcode = barcode.trim().ifEmpty { null }, category = com.bongshaltech.erpinventory.core.stock.Categories.join(com.bongshaltech.erpinventory.core.stock.Categories.split(category)), generic = generic.trim(),
                    unit = unit.trim().ifEmpty { "pcs" }, costPaisa = cost.toPaisa() ?: 0, pricePaisa = pricePaisa,
                    stockMilli = stock.toMilli() ?: 0, lowStockMilli = lowAt.toMilli() ?: 0,
                    packMilli = (packSize.toMilli() ?: 0).coerceAtLeast(0), packName = packName.trim(),
                )
            )
            if (next) {
                Toast.makeText(ctx, savedMsg, Toast.LENGTH_SHORT).show()
                loaded = null; name = ""; generic = ""; barcode = ""; cost = ""; price = ""; stock = ""; lowAt = ""; packSize = ""; packName = ""; tried = false
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
            if (loaded?.groupId == 0L || loaded == null) Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(stringResource(R.string.var_type), 14.sp, FontWeight.SemiBold)
                    Muted(stringResource(if (variantMode) R.string.var_type_variant_sub else R.string.var_type_single_sub))
                }
                com.bongshaltech.erpinventory.ui.components.Segmented(
                    listOf(stringResource(R.string.var_single), stringResource(R.string.var_variant)), if (variantMode) 1 else 0,
                    { i ->
                        variantMode = i == 1
                        // A saved single product turned into variants keeps its stock on the first variant.
                        if (variantMode && vrows.isEmpty()) vrows.add(VRow(loaded?.id ?: 0L, "", price, cost, barcode, stock, lowAt))
                    },
                )
            }
            loaded?.code?.takeIf { it.isNotBlank() }?.let { Muted(stringResource(R.string.product_code_is, it)) }
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Input(name, { name = it }, label = stringResource(if (variantMode) R.string.var_style_name else R.string.product_name), error = required.takeIf { tried && name.isBlank() })
                    if (pharmacy || generic.isNotBlank()) Input(generic, { generic = it }, label = stringResource(R.string.product_generic))
                    if (!variantMode) Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Input(barcode, { barcode = it }, Modifier.weight(1f), label = stringResource(R.string.product_barcode))
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Brand.GreenGrad)
                                .clickable { scanBarcode(ctx) { barcode = it } },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan), tint = Color.White, modifier = Modifier.size(20.dp)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(category, { category = it }, Modifier.weight(1f), label = stringResource(R.string.product_category))
                        Input(unit, { unit = it }, Modifier.weight(1f), label = stringResource(R.string.product_unit))
                    }
                    Muted(stringResource(R.string.category_many_hint))
                    if (packsOn) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(packName, { packName = it }, Modifier.weight(1f), label = stringResource(R.string.pack_name), placeholder = stringResource(R.string.pack_default))
                        Input(packSize, { packSize = it }, Modifier.weight(1f), label = stringResource(R.string.pack_size, unit.ifBlank { "pcs" }), keyboard = KeyboardType.Decimal)
                    }
                    if (!variantMode) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(cost, { cost = it }, Modifier.weight(1f), label = stringResource(R.string.product_cost), keyboard = KeyboardType.Decimal)
                        Input(
                            price, { price = it }, Modifier.weight(1f), label = stringResource(R.string.product_price), keyboard = KeyboardType.Decimal,
                            error = required.takeIf { tried && priceP == null },
                        )
                    }
                    if (!variantMode) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Input(stock, { stock = it }, Modifier.weight(1f), label = stringResource(R.string.product_stock), keyboard = KeyboardType.Decimal)
                        Input(lowAt, { lowAt = it }, Modifier.weight(1f), label = stringResource(R.string.product_low_at), keyboard = KeyboardType.Decimal)
                    }
                    if (!variantMode && costP != null && priceP != null && costP > 0) {
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
            if (variantMode) VariantEditor(sizes, { sizes = it }, colors, { colors = it }, vrows, variantMsg)
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

/** One variant row while it is being typed. */
internal data class VRow(
    val id: Long, val variant: String, val price: String, val cost: String, val barcode: String, val stock: String, val low: String,
) {
    fun toRow() = com.bongshaltech.erpinventory.data.ShopRepo.VariantRow(
        id, variant.trim(), barcode.trim(), cost.toPaisa() ?: 0, price.toPaisa() ?: 0, stock.toMilli() ?: 0, low.toMilli() ?: 0,
    )

    companion object {
        private fun money(p: Long) = if (p == 0L) "" else (p / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()
        fun of(p: ProductEntity) = VRow(p.id, p.variant, money(p.pricePaisa), money(p.costPaisa), p.barcode.orEmpty(), p.stockMilli.qty(), p.lowStockMilli.qty())
    }
}

/** Sizes × colours make the rows; each row has its own price, cost, barcode, opening stock and reorder level. */
@Composable
private fun VariantEditor(
    sizes: String, onSizes: (String) -> Unit, colors: String, onColors: (String) -> Unit,
    rows: androidx.compose.runtime.snapshots.SnapshotStateList<VRow>, message: String?,
) {
    val ctx = LocalContext.current
    com.bongshaltech.erpinventory.ui.components.Panel(stringResource(R.string.var_panel), Brand.PurpleGrad, com.bongshaltech.erpinventory.ui.components.PanelColors.Purple) {
        Muted(stringResource(R.string.var_help))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Input(sizes, onSizes, Modifier.weight(1f), label = stringResource(R.string.var_sizes), placeholder = "S, M, L")
            Input(colors, onColors, Modifier.weight(1f), label = stringResource(R.string.var_colors), placeholder = "Red, Blue")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradButton(stringResource(R.string.var_make_rows), Brand.PurpleGrad, Modifier.weight(1f), height = 42.dp) {
                val combos = com.bongshaltech.erpinventory.core.stock.Variants.combos(
                    listOf(com.bongshaltech.erpinventory.core.stock.Variants.options(sizes), com.bongshaltech.erpinventory.core.stock.Variants.options(colors)),
                )
                val first = rows.firstOrNull()
                // An empty first row (from a single product) takes the first combination and keeps its stock.
                if (first != null && first.variant.isBlank() && combos.isNotEmpty()) rows[0] = first.copy(variant = combos.first())
                combos.filter { c -> rows.none { it.variant.equals(c, ignoreCase = true) } }.forEach { c ->
                    rows.add(VRow(0, c, first?.price.orEmpty(), first?.cost.orEmpty(), "", "", first?.low.orEmpty()))
                }
            }
            WhiteButton(stringResource(R.string.var_same_all), height = 42.dp) {
                val f = rows.firstOrNull() ?: return@WhiteButton
                for (i in rows.indices) rows[i] = rows[i].copy(price = f.price, cost = f.cost, low = f.low)
            }
        }
        rows.forEachIndexed { i, r ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Ui.c.border, RoundedCornerShape(12.dp)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(r.variant, { rows[i] = r.copy(variant = it) }, Modifier.weight(1f), placeholder = stringResource(R.string.var_name), height = 40.dp)
                    T("✕", 15.sp, color = Ui.c.muted, modifier = Modifier.clickable { rows.removeAt(i) }.padding(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(r.price, { rows[i] = r.copy(price = it) }, Modifier.weight(1f), label = stringResource(R.string.product_price), keyboard = KeyboardType.Decimal, height = 40.dp)
                    Input(r.cost, { rows[i] = r.copy(cost = it) }, Modifier.weight(1f), label = stringResource(R.string.product_cost), keyboard = KeyboardType.Decimal, height = 40.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Input(r.stock, { rows[i] = r.copy(stock = it) }, Modifier.weight(1f), label = stringResource(R.string.product_stock), keyboard = KeyboardType.Decimal, height = 40.dp)
                    Input(r.low, { rows[i] = r.copy(low = it) }, Modifier.weight(1f), label = stringResource(R.string.product_low_at), keyboard = KeyboardType.Decimal, height = 40.dp)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Input(r.barcode, { rows[i] = r.copy(barcode = it) }, Modifier.weight(1f), label = stringResource(R.string.product_barcode), height = 40.dp)
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Brand.GreenGrad).clickable { scanBarcode(ctx) { code -> rows[i] = rows[i].copy(barcode = code) } },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan), tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        if (rows.isEmpty()) Muted(stringResource(R.string.var_no_rows))
        T("+ " + stringResource(R.string.var_add_row), 14.sp, FontWeight.SemiBold, Brand.Link,
            Modifier.clickable { rows.add(VRow(0, "", rows.firstOrNull()?.price.orEmpty(), rows.firstOrNull()?.cost.orEmpty(), "", "", "")) }.padding(vertical = 4.dp))
        message?.let { T(it, 13.sp, FontWeight.SemiBold, Brand.Red) }
    }
}
