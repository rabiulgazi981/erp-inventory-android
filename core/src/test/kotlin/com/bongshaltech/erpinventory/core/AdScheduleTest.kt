package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.billing.AdSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdScheduleTest {
    @Test fun firstWeekHasNoAds() {
        for (d in 0L..6L) assertEquals(AdSchedule.Rule(false, 0), AdSchedule.rule(d))
    }

    @Test fun thenLightThenNormal() {
        assertEquals(AdSchedule.Rule(false, 2), AdSchedule.rule(7))
        assertEquals(AdSchedule.Rule(false, 2), AdSchedule.rule(29))
        assertTrue(AdSchedule.rule(30).banner)
        assertEquals(5, AdSchedule.rule(90).fullPerDay)
    }

    @Test fun days() {
        assertEquals(0, AdSchedule.days(0, 1_000))
        assertEquals(7, AdSchedule.days(1_000, 1_000 + 7 * 86_400_000L + 5))
        assertFalse(AdSchedule.rule(AdSchedule.days(5_000, 1_000)).banner)
    }
}
