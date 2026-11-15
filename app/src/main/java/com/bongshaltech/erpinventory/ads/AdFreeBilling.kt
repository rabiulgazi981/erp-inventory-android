package com.bongshaltech.erpinventory.ads

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.bongshaltech.erpinventory.ErpApp
import com.bongshaltech.erpinventory.core.billing.PlanMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * "Remove ads" subscription through Google Play Billing.
 *
 * Play Console must have a subscription with product id [PRODUCT_ID] and four auto-renewing base plans:
 * monthly (P1M) 250, every 3 months (P3M) 700, every 6 months (P6M) 1300 and yearly (P1Y) 2400 taka. Prices are set there, not in the app; the app shows
 * whatever price Play returns, in the buyer's currency.
 *
 * The result is cached in settings so ads stay off while offline. Each time Play is
 * reachable the cache is refreshed, so a cancelled or expired subscription turns ads back on.
 */
class AdFreeBilling(private val app: ErpApp) : PurchasesUpdatedListener {
    data class Plan(val offerToken: String, val price: String, val period: String, val hasTrial: Boolean, val priceMicros: Long = 0, val months: Int = 0)

    sealed interface State {
        data object Loading : State
        data object Ready : State
        /** Play Store missing, signed out, or the product isn't set up in Play Console yet. */
        data object Unavailable : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state
    private var details: ProductDetails? = null

    private val client = BillingClient.newBuilder(app)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private fun connect(then: () -> Unit) {
        if (client.isReady) return then()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) then() else _state.value = State.Unavailable
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    /** Load prices and re-check what this Google account has bought. Safe to call often. */
    fun refresh() = connect { scope.launch { loadPlans(); restore() } }

    private suspend fun loadPlans() {
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(ProductType.SUBS).build()
        val r = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
        val d = r.productDetailsList?.firstOrNull()
        details = d
        _plans.value = d?.subscriptionOfferDetails.orEmpty().map { o ->
            val phases = o.pricingPhases.pricingPhaseList
            val paid = phases.last()
            Plan(o.offerToken, paid.formattedPrice, paid.billingPeriod, phases.any { it.priceAmountMicros == 0L },
                paid.priceAmountMicros, PlanMath.months(paid.billingPeriod))
        }.distinctBy { it.period }.sortedBy { if (it.months == 0) Int.MAX_VALUE else it.months }
        _state.value = if (_plans.value.isEmpty()) State.Unavailable else State.Ready
    }

    private suspend fun restore() {
        val r = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
        if (r.billingResult.responseCode != BillingResponseCode.OK) return // keep the cached value
        r.purchasesList.forEach { acknowledge(it) }
        val active = r.purchasesList.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        app.settings.setAdFree(active)
    }

    fun buy(activity: Activity, plan: Plan) {
        val d = details ?: return
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(plan.offerToken).build()
        connect { client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item)).build()) }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK || result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
            scope.launch { purchases?.forEach { acknowledge(it) }; restore() }
        }
    }

    /** Play refunds a subscription that isn't acknowledged within 3 days. */
    private suspend fun acknowledge(p: Purchase) {
        if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        }
    }

    companion object {
        const val PRODUCT_ID = "ad_free"
    }
}
