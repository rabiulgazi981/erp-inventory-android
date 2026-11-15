package com.bongshaltech.erpinventory.core

import com.bongshaltech.erpinventory.core.stock.Recipe
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class RecipeTest {
    @Test fun scalesInputs() {
        // 2 kg flour makes 10 cakes; 25 cakes need 5 kg
        assertEquals(5_000L, Recipe.need(2_000, 10_000, 25_000))
        // 1 unit makes 3; 1 more needs 333 milli
        assertEquals(333L, Recipe.need(1_000, 3_000, 1_000))
        assertEquals(667L, Recipe.need(1_000, 3_000, 2_000))
    }

    @Test fun costRounds() {
        assertEquals(1_250L, Recipe.cost(2_500, 500))
        assertEquals(1L, Recipe.cost(1, 500))
    }

    @Test fun finishedWhenAllDone() {
        assertTrue(Recipe.finished(listOf(Triple(5_000L, 4_000L, 1_000L))))
        assertFalse(Recipe.finished(listOf(Triple(5_000L, 4_000L, 0L), Triple(1_000L, 1_000L, 0L))))
    }
}
