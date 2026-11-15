package com.bongshaltech.erpinventory.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bongshaltech.erpinventory.BuildConfig
import com.bongshaltech.erpinventory.ErpApp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * AdMob for the free version: a banner at the bottom of the main tabs, and a full-screen ad
 * after some sales (never more than once every [MIN_GAP_MS], and only between sales).
 * Nothing is shown when the ad-free subscription is active or when no ad unit is configured.
 */
object Ads {
    private const val SALES_PER_AD = 4
    private const val MIN_GAP_MS = 3 * 60_000L

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready
    private var interstitial: InterstitialAd? = null
    private var salesSinceAd = 0
    private var lastShownAt = 0L

    /** Ask for ad consent where the law needs it (EEA, UK), then start the ads SDK. */
    fun start(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { if (consent.canRequestAds()) init(activity) }
        }, { if (consent.canRequestAds()) init(activity) })
        if (consent.canRequestAds()) init(activity)
    }

    private fun init(activity: Activity) {
        if (_ready.value) return
        _ready.value = true
        val ctx = activity.applicationContext
        Thread { MobileAds.initialize(ctx) {} }.start()
        loadInterstitial(ctx)
    }

    private fun loadInterstitial(ctx: Context) {
        val unit = BuildConfig.INTERSTITIAL_AD_UNIT
        if (unit.isBlank() || interstitial != null) return
        InterstitialAd.load(ctx, unit, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
        })
    }

    /** Call when the shopkeeper closes the "sale saved" box. */
    fun afterSale(ctx: Context) {
        val activity = ctx.findActivity() ?: return
        val app = activity.application as ErpApp
        if (!_ready.value || app.adFreeCached) return
        salesSinceAd++
        val now = System.currentTimeMillis()
        if (salesSinceAd < SALES_PER_AD || now - lastShownAt < MIN_GAP_MS) return
        val ad = interstitial ?: return loadInterstitial(activity.applicationContext)
        interstitial = null
        salesSinceAd = 0
        lastShownAt = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadInterstitial(activity.applicationContext)
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) = loadInterstitial(activity.applicationContext)
        }
        ad.show(activity)
    }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

/** Bottom banner for the main tabs. Takes no space at all for ad-free users or before ads are ready. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ErpApp
    val show by remember { combine(Ads.ready, app.settings.settings) { r, s -> r && !s.adFree } }
        .collectAsStateWithLifecycle(false)
    val unit = BuildConfig.BANNER_AD_UNIT
    if (!show || unit.isBlank()) return
    val widthDp = LocalConfiguration.current.screenWidthDp
    val view = remember(widthDp) {
        AdView(ctx).apply {
            adUnitId = unit
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
            adListener = object : AdListener() {}
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(view) { onDispose { view.destroy() } }
    AndroidView(factory = { view }, modifier = modifier.fillMaxWidth())
}
