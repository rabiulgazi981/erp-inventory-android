package com.bongshaltech.erpinventory.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.R
import com.bongshaltech.erpinventory.ads.AdFreeBilling
import com.bongshaltech.erpinventory.ads.findActivity
import com.bongshaltech.erpinventory.core.billing.PlanMath
import com.bongshaltech.erpinventory.ui.components.Card
import com.bongshaltech.erpinventory.ui.components.GradButton
import com.bongshaltech.erpinventory.ui.components.Header
import com.bongshaltech.erpinventory.ui.components.Muted
import com.bongshaltech.erpinventory.ui.components.T
import com.bongshaltech.erpinventory.ui.components.WhiteButton
import com.bongshaltech.erpinventory.ui.theme.Brand
import com.bongshaltech.erpinventory.ui.theme.Ui

/** More › Remove ads: the ad-free subscription, priced and sold by Google Play. */
@Composable
fun AdFreeScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = app()
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val plans by app.adFree.plans.collectAsStateWithLifecycle()
    val state by app.adFree.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.adFree.refresh() }
    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(Modifier.fillMaxSize().background(Ui.c.bg)) {
        Header(stringResource(R.string.remove_ads), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings?.adFree == true) {
                Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                    T(stringResource(R.string.adfree_on), 18.sp, FontWeight.Bold, Brand.Green)
                    Muted(stringResource(R.string.adfree_on_note), 13.sp)
                }
                WhiteButton(stringResource(R.string.adfree_manage), Modifier.fillMaxWidth()) {
                    open("https://play.google.com/store/account/subscriptions?sku=${AdFreeBilling.PRODUCT_ID}&package=${ctx.packageName}")
                }
                return@Column
            }
            Muted(stringResource(R.string.adfree_help), 14.sp)
            when {
                plans.isNotEmpty() -> plans.forEach { p ->
                    val monthly = plans.firstOrNull { it.months == 1 }?.priceMicros ?: 0
                    val per = when (p.months) {
                        1 -> stringResource(R.string.adfree_per_month)
                        12 -> stringResource(R.string.adfree_per_year)
                        0 -> ""
                        else -> stringResource(R.string.adfree_per_n_months, p.months)
                    }
                    val save = PlanMath.savingPercent(p.priceMicros, p.months, monthly)
                    Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                        T("${p.price} $per", 20.sp, FontWeight.Bold)
                        if (save > 0) T(stringResource(R.string.adfree_save, save), 13.sp, FontWeight.SemiBold, Brand.DarkGreen)
                        if (p.hasTrial) Muted(stringResource(R.string.adfree_trial), 13.sp)
                        GradButton(stringResource(R.string.go_ad_free), Brand.GreenGrad, Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            ctx.findActivity()?.let { app.adFree.buy(it, p) }
                        }
                    }
                }
                state == AdFreeBilling.State.Loading -> Muted(stringResource(R.string.sms_working), 14.sp)
                else -> Muted(stringResource(R.string.adfree_unavailable), 14.sp)
            }
            WhiteButton(stringResource(R.string.adfree_restore), Modifier.fillMaxWidth()) { app.adFree.refresh() }
            Muted(stringResource(R.string.adfree_cancel_note), 12.sp)
        }
    }
}
