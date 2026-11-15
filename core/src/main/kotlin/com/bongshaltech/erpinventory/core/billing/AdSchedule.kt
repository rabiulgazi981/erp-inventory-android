package com.bongshaltech.erpinventory.core.billing

/**
 * How many ads a free user sees, by days since the app was first opened (owner's rule, 2026-11-15):
 * the first 7 days have no ads at all; then one or two full-screen ads a day and no banner;
 * from day 31 the normal amount (bottom banner and up to [FULL_MAX] full-screen ads a day),
 * still within AdMob's frequency guidance (gap between ads, only between sales).
 */
object AdSchedule {
    const val FREE_DAYS = 7
    const val LIGHT_UNTIL_DAY = 30
    const val FULL_LIGHT = 2
    const val FULL_MAX = 5

    data class Rule(val banner: Boolean, val fullPerDay: Int)

    fun rule(daysSinceInstall: Long): Rule = when {
        daysSinceInstall < FREE_DAYS -> Rule(banner = false, fullPerDay = 0)
        daysSinceInstall < LIGHT_UNTIL_DAY -> Rule(banner = false, fullPerDay = FULL_LIGHT)
        else -> Rule(banner = true, fullPerDay = FULL_MAX)
    }

    /** Whole days between [installedAt] and [now] (millis). An unknown install time counts as today. */
    fun days(installedAt: Long, now: Long): Long = if (installedAt <= 0 || now < installedAt) 0 else (now - installedAt) / 86_400_000L
}
