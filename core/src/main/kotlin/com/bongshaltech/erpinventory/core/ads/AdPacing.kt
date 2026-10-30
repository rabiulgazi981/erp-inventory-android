package com.bongshaltech.erpinventory.core.ads

/**
 * Decides whether an ad may show right now. Values come from Firebase Remote Config so they can change without an app update.
 *
 * Rules agreed with the owner:
 * - No ads in the first [quietDays] days after install.
 * - Then [startPerDay] per day, growing slowly with engagement up to [maxPerDay].
 * - Interstitials only at natural breaks, at least [minInterstitialGapMinutes] apart, never on app open.
 * - Never offline, never for Remove-ads subscribers, never on sale / scan / print / form screens.
 */
data class AdConfig(
    val quietDays: Int = 7,
    val startPerDay: Int = 1,
    val maxPerDay: Int = 6,
    /** Each this many saved records in the last 7 days earns one more interstitial per day. */
    val recordsPerExtraAd: Int = 40,
    val minInterstitialGapMinutes: Int = 12,
    val bannerEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
)

enum class Placement { HOME_BANNER, INTERSTITIAL_AFTER_TASK }

data class AdState(
    val installedAtMillis: Long,
    val nowMillis: Long,
    val online: Boolean,
    val adFree: Boolean,
    /** True on sale, scan, print and form screens. */
    val onSensitiveScreen: Boolean,
    val interstitialsShownToday: Int,
    val lastInterstitialAtMillis: Long?,
    /** Sales, purchases, expenses and other records saved in the last 7 days. */
    val recordsLast7Days: Int,
    /** True only for the first screen after a cold start. */
    val justOpened: Boolean,
)

object AdPacing {
    private const val DAY = 24L * 60 * 60 * 1000
    private const val MINUTE = 60L * 1000

    fun dailyInterstitialCap(config: AdConfig, state: AdState): Int {
        val extra = if (config.recordsPerExtraAd <= 0) 0 else state.recordsLast7Days / config.recordsPerExtraAd
        return (config.startPerDay + extra).coerceAtMost(config.maxPerDay)
    }

    fun inQuietPeriod(config: AdConfig, state: AdState) =
        state.nowMillis - state.installedAtMillis < config.quietDays * DAY

    fun canShow(placement: Placement, config: AdConfig, state: AdState): Boolean {
        if (state.adFree || !state.online || state.onSensitiveScreen) return false
        if (inQuietPeriod(config, state)) return false
        return when (placement) {
            Placement.HOME_BANNER -> config.bannerEnabled
            Placement.INTERSTITIAL_AFTER_TASK -> {
                if (!config.interstitialEnabled || state.justOpened) return false
                if (state.interstitialsShownToday >= dailyInterstitialCap(config, state)) return false
                val last = state.lastInterstitialAtMillis
                last == null || state.nowMillis - last >= config.minInterstitialGapMinutes * MINUTE
            }
        }
    }
}
