package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Blender
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.TintIcon
import com.bongshaltech.erpinventory.ui.components.softShadow
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Tint
import com.bongshaltech.erpinventory.ui.theme.Tints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.bongshaltech.erpinventory.ui.toPaisa
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.data.Feature
import com.bongshaltech.erpinventory.ui.components.Group
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.ListRow
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.Toggle
import com.bongshaltech.erpinventory.ui.theme.Ui
import kotlinx.coroutines.launch

/** One optional module: its switch, title, one-line note, icon and colour. */
internal data class FeatureRow(val f: Feature, val title: Int, val sub: Int, val icon: ImageVector, val tint: Tint)

/** The Features page, grouped into sections so a shopkeeper finds a switch by what it is for. */
internal val featureSections: List<Pair<Int, List<FeatureRow>>> = listOf(
    R.string.feat_sec_sales to listOf(
        FeatureRow(Feature.PROMOS, R.string.promos, R.string.feat_promos_sub, Icons.Outlined.LocalOffer, Tints.Pink),
        FeatureRow(Feature.ORDERS, R.string.orders, R.string.feat_orders_sub, Icons.Outlined.Description, Tints.Blue),
        FeatureRow(Feature.INSTALLMENTS, R.string.installments, R.string.feat_installments_sub, Icons.Outlined.CalendarMonth, Tints.Indigo),
        FeatureRow(Feature.CREDIT_LIMIT, R.string.feat_credit, R.string.feat_credit_sub, Icons.Outlined.CreditScore, Tints.Amber),
        FeatureRow(Feature.AREAS, R.string.feat_areas, R.string.feat_areas_sub, Icons.Outlined.Place, Tints.Teal),
        FeatureRow(Feature.LOYALTY, R.string.feat_loyalty, R.string.feat_loyalty_sub, Icons.Outlined.Stars, Tints.Orange),
        FeatureRow(Feature.COURIER, R.string.cr_title, R.string.feat_courier_sub, Icons.Outlined.LocalShipping, Tints.Blue),
    ),
    R.string.feat_sec_money to listOf(
        FeatureRow(Feature.CASH_DAY, R.string.cash_day, R.string.feat_cash_day_sub, Icons.Outlined.PointOfSale, Tints.Green),
        FeatureRow(Feature.MOBILE_BANKING, R.string.mfs_toggle, R.string.mfs_toggle_sub, Icons.Outlined.PhoneAndroid, Tints.Pink),
        FeatureRow(Feature.LOANS, R.string.loans, R.string.feat_loans_sub, Icons.Outlined.RequestQuote, Tints.Amber),
        FeatureRow(Feature.RECURRING, R.string.recurring, R.string.feat_recurring_sub, Icons.Outlined.EventRepeat, Tints.Purple),
        FeatureRow(Feature.INVESTORS, R.string.investors, R.string.feat_investors_sub, Icons.Outlined.Handshake, Tints.Teal),
        FeatureRow(Feature.ACCOUNTING, R.string.accounts, R.string.feat_accounting_sub, Icons.Outlined.AccountBalance, Tints.Indigo),
    ),
    R.string.feat_sec_stock to listOf(
        FeatureRow(Feature.BATCHES, R.string.batches, R.string.feat_batches_sub, Icons.Outlined.EventAvailable, Tints.Orange),
        FeatureRow(Feature.SERIALS, R.string.serials, R.string.feat_serials_sub, Icons.Outlined.QrCode2, Tints.Indigo),
        FeatureRow(Feature.REPACK, R.string.repack, R.string.feat_repack_sub, Icons.Outlined.Inventory2, Tints.Green),
        FeatureRow(Feature.PACKS, R.string.feat_packs, R.string.feat_packs_sub, Icons.Outlined.ViewModule, Tints.Purple),
        FeatureRow(Feature.WAREHOUSES, R.string.warehouses, R.string.feat_warehouses_sub, Icons.Outlined.Warehouse, Tints.Blue),
        FeatureRow(Feature.SCALE_BARCODE, R.string.feat_scale, R.string.feat_scale_sub, Icons.Outlined.Scale, Tints.Teal),
    ),
    R.string.feat_sec_make to listOf(
        FeatureRow(Feature.MANUFACTURING, R.string.manufacturing, R.string.feat_mfg_sub, Icons.Outlined.Blender, Tints.Orange),
        FeatureRow(Feature.FACTORY, R.string.factory, R.string.feat_factory_sub, Icons.Outlined.Factory, Tints.Grey),
        FeatureRow(Feature.REPAIR, R.string.repair, R.string.feat_repair_sub, Icons.Outlined.Build, Tints.Amber),
    ),
    R.string.feat_sec_type to listOf(
        FeatureRow(Feature.TABLES, R.string.tables, R.string.feat_tables_sub, Icons.Outlined.TableRestaurant, Tints.Orange),
        FeatureRow(Feature.KITCHEN, R.string.kitchen, R.string.feat_kitchen_sub, Icons.Outlined.SoupKitchen, Tints.Pink),
        FeatureRow(Feature.PRESCRIPTION, R.string.rx_title, R.string.feat_rx_sub, Icons.Outlined.MedicalServices, Tints.Teal),
    ),
    R.string.feat_sec_people to listOf(
        FeatureRow(Feature.HR, R.string.hr, R.string.feat_hr_sub, Icons.Outlined.Badge, Tints.Indigo),
    ),
)

@Composable
fun FeaturesScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val s by app.settings.settings.collectAsStateWithLifecycle(null)
    val all = featureSections.sumOf { it.second.size }
    val onCount = featureSections.sumOf { sec -> sec.second.count { s?.on(it.f) == true } }
    var query by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.features), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Summary: how many are on, on the accent colour.
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier.fillMaxWidth().softShadow(shape).clip(shape).background(Brand.BlueGrad).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Tune, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f)) {
                    T(stringResource(R.string.feat_on_count, onCount, all), 17.sp, FontWeight.Bold, Color.White)
                    T(stringResource(R.string.features_note), 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
            com.bongshaltech.erpinventory.ui.components.Input(query, { query = it }, placeholder = stringResource(R.string.feat_search),
                leading = { Icon(Icons.Outlined.Search, null, tint = Ui.c.muted, modifier = Modifier.size(20.dp)) })
            val q = query.trim().lowercase()
            featureSections.forEach { (title, all) ->
                val rows = if (q.isEmpty()) all else all.filter { r ->
                    ctx.getString(r.title).lowercase().contains(q) || ctx.getString(r.sub).lowercase().contains(q)
                }
                if (rows.isNotEmpty()) Group(stringResource(title)) {
                    rows.forEachIndexed { i, r ->
                        val on = s?.on(r.f) == true
                        FeatureItem(r, on, i == rows.lastIndex) { v -> scope.launch { app.settings.setFeature(r.f, v) } }
                        if (r.f == Feature.LOYALTY && on) s?.let { cur -> LoyaltyRates(cur.pointEveryPaisa, cur.pointValuePaisa) { e, v -> scope.launch { app.settings.setLoyalty(e, v) } } }
                        if (r.f == Feature.SCALE_BARCODE && on) ListRow(
                            stringResource(R.string.scale_by_price), sub = stringResource(R.string.scale_by_price_sub),
                            trailing = { Toggle(s?.scaleByPrice == true, { v -> scope.launch { app.settings.setScaleByPrice(v) } }) },
                        )
                    }
                }
            }
        }
    }
}

/** One feature: coloured icon, title, a short note and the switch. The whole row toggles. */
@Composable
private fun FeatureItem(r: FeatureRow, on: Boolean, last: Boolean, onChange: (Boolean) -> Unit) {
    val divider = Ui.c.divider
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!on) }
            .drawBehind { if (!last) drawRect(divider, topLeft = Offset(68.dp.toPx(), size.height - 1.dp.toPx()), size = size.copy(width = size.width - 68.dp.toPx(), height = 1.dp.toPx())) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TintIcon(r.icon, r.tint, 40.dp, 21.dp, 12.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            T(stringResource(r.title), 15.sp, FontWeight.SemiBold)
            Muted(stringResource(r.sub), 12.sp)
        }
        Toggle(on, onChange)
    }
}

/** Loyalty rates: one point per this much taka paid, and what a point is worth when spent. */
@Composable
private fun LoyaltyRates(everyPaisa: Long, valuePaisa: Long, save: (Long, Long) -> Unit) {
    var every by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf((everyPaisa / 100).toString()) }
    var value by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf((valuePaisa / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()) }
    androidx.compose.foundation.layout.Row(
        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        com.bongshaltech.erpinventory.ui.components.Input(
            every, {
                every = it.filter(Char::isDigit).take(6)
                every.toLongOrNull()?.takeIf { e -> e > 0 }?.let { e -> save(e * 100, valuePaisa) }
            }, Modifier.weight(1f), label = stringResource(R.string.loyalty_every), keyboard = androidx.compose.ui.text.input.KeyboardType.Number, height = 40.dp,
        )
        com.bongshaltech.erpinventory.ui.components.Input(
            value, {
                value = it
                it.toPaisa()?.let { v -> save(everyPaisa, v) }
            }, Modifier.weight(1f), label = stringResource(R.string.loyalty_value), keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal, height = 40.dp,
        )
    }
}
