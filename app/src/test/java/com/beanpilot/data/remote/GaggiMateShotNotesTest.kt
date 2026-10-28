package com.beanpilot.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class GaggiMateShotNotesTest {
    @Test
    fun `review merge preserves taste data that BeanPilot does not edit`() {
        val existing = GaggiMateShotNotes(
            id = "31",
            rating = 4,
            beanType = "Old coffee",
            doseIn = "18.0",
            doseOut = "36.4",
            ratio = "2.02",
            grindSetting = "2.5",
            balanceTaste = "balanced",
            notes = "Keep this note",
        )

        val merged = existing.mergeReview(
            sourceId = "31",
            rating = 5,
            coffeeName = "New coffee",
            doseInGr = 19.0,
            doseOutGr = 38.0,
            ratio = 2.0,
            grinderSetting = null,
            balanceTaste = null,
            notes = "Updated note",
        )

        assertEquals(
            GaggiMateShotNotes(
                id = "31",
                rating = 5,
                beanType = "New coffee",
                doseIn = "19",
                doseOut = "38",
                ratio = "2",
                grindSetting = "2.5",
                balanceTaste = "balanced",
                notes = "Updated note",
            ),
            merged,
        )
    }

    @Test
    fun `unrated review maps to GaggiMate zero rating`() {
        val merged = GaggiMateShotNotes(rating = 5).mergeReview(
            sourceId = "8",
            rating = null,
            coffeeName = null,
            doseInGr = null,
            doseOutGr = null,
            ratio = null,
            grinderSetting = null,
            balanceTaste = null,
            notes = null,
        )

        assertEquals(0, merged.rating)
    }

    @Test
    fun `explicit empty coffee name and notes clear previously saved review fields`() {
        val merged = GaggiMateShotNotes(
            beanType = "Temporary coffee",
            balanceTaste = "balanced",
            notes = "Temporary test note",
        ).mergeReview(
            sourceId = "8",
            rating = null,
            coffeeName = "",
            doseInGr = null,
            doseOutGr = null,
            ratio = null,
            grinderSetting = null,
            balanceTaste = "",
            notes = "",
        )

        assertEquals("", merged.beanType)
        assertEquals("", merged.balanceTaste)
        assertEquals("", merged.notes)
    }
}
