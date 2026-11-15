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
    Triple(Feature.CASH_DAY, R.string.cash_day, R.string.feat_cash_day_sub),
    Triple(Feature.LOANS, R.string.loans, R.string.feat_loans_sub),
    Triple(Feature.HR, R.string.hr, R.string.feat_hr_sub),
    Triple(Feature.BATCHES, R.string.batches, R.string.feat_batches_sub),
    Triple(Feature.SERIALS, R.string.serials, R.string.feat_serials_sub),
    Triple(Feature.REPACK, R.string.repack, R.string.feat_repack_sub),
    Triple(Feature.WAREHOUSES, R.string.warehouses, R.string.feat_warehouses_sub),
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
                }
            }
        }
    }
}
