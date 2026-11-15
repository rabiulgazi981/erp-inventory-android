package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.foundation.background
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

/** Title and one-line note for each optional module, in the order shown. */
internal val featureRows: List<Triple<Feature, Int, Int>> = listOf(
    Triple(Feature.PROMOS, R.string.promos, R.string.feat_promos_sub),
    Triple(Feature.ORDERS, R.string.orders, R.string.feat_orders_sub),
    Triple(Feature.INSTALLMENTS, R.string.installments, R.string.feat_installments_sub),
    Triple(Feature.CREDIT_LIMIT, R.string.feat_credit, R.string.feat_credit_sub),
    Triple(Feature.AREAS, R.string.feat_areas, R.string.feat_areas_sub),
    Triple(Feature.LOYALTY, R.string.feat_loyalty, R.string.feat_loyalty_sub),
    Triple(Feature.CASH_DAY, R.string.cash_day, R.string.feat_cash_day_sub),
    Triple(Feature.RECURRING, R.string.recurring, R.string.feat_recurring_sub),
    Triple(Feature.INVESTORS, R.string.investors, R.string.feat_investors_sub),
    Triple(Feature.ACCOUNTING, R.string.accounts, R.string.feat_accounting_sub),
    Triple(Feature.LOANS, R.string.loans, R.string.feat_loans_sub),
    Triple(Feature.HR, R.string.hr, R.string.feat_hr_sub),
    Triple(Feature.BATCHES, R.string.batches, R.string.feat_batches_sub),
    Triple(Feature.SERIALS, R.string.serials, R.string.feat_serials_sub),
    Triple(Feature.REPACK, R.string.repack, R.string.feat_repack_sub),
    Triple(Feature.WAREHOUSES, R.string.warehouses, R.string.feat_warehouses_sub),
    Triple(Feature.PACKS, R.string.feat_packs, R.string.feat_packs_sub),
    Triple(Feature.SCALE_BARCODE, R.string.feat_scale, R.string.feat_scale_sub),
    Triple(Feature.TABLES, R.string.tables, R.string.feat_tables_sub),
    Triple(Feature.MOBILE_BANKING, R.string.mfs_toggle, R.string.mfs_toggle_sub),
)

@Composable
fun FeaturesScreen(onBack: () -> Unit) {
    val app = app()
    val who = rememberWho()
    if (who.ready && !who.owner) return NotAllowed(onBack)
    val scope = rememberCoroutineScope()
    val s by app.settings.settings.collectAsStateWithLifecycle(null)
    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.features), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Muted(stringResource(R.string.features_note))
            Group(null) {
                featureRows.forEachIndexed { i, (f, title, sub) ->
                    ListRow(
                        stringResource(title), sub = stringResource(sub), last = i == featureRows.lastIndex,
                        trailing = { Toggle(s?.on(f) == true, { on -> scope.launch { app.settings.setFeature(f, on) } }) },
                    )
                    if (f == Feature.LOYALTY && s?.on(f) == true) s?.let { cur -> LoyaltyRates(cur.pointEveryPaisa, cur.pointValuePaisa) { e, v -> scope.launch { app.settings.setLoyalty(e, v) } } }
                    if (f == Feature.SCALE_BARCODE && s?.on(f) == true) ListRow(
                        stringResource(R.string.scale_by_price), sub = stringResource(R.string.scale_by_price_sub),
                        trailing = { Toggle(s?.scaleByPrice == true, { on -> scope.launch { app.settings.setScaleByPrice(on) } }) },
                    )
                }
            }
        }
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
