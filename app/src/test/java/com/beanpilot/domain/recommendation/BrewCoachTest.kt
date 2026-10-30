package com.beanpilot.domain.recommendation

import com.beanpilot.domain.model.ShotRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrewCoachTest {
    @Test
    fun emptyHistoryAsksForReviewedShotData() {
        val recommendation = BrewCoach.recommend(emptyList())

        assertEquals("Build your extraction baseline", recommendation.title)
        assertEquals("LEARNING", recommendation.confidence)
    }

    @Test
    fun sourShortShotRecommendsSlowingExtraction() {
        val recommendation = BrewCoach.recommend(
            listOf(
                ShotRecord(
                    id = 1,
                    brewTimeMs = 18_000,
                    profileUsed = "Adaptive",
                    balanceTaste = "sour",
                ),
            ),
        )

        assertEquals("Slow the extraction slightly", recommendation.title)
        assertTrue(recommendation.detail.contains("finer grind"))
    }

    @Test
    fun bestRatedSameCoffeeShotProvidesPersonalizedRatioAdvice() {
        val recommendation = BrewCoach.recommend(
            listOf(
                ShotRecord(
                    id = 1,
                    timestamp = 1,
                    coffeeBeanId = 7,
                    doseInGr = 18.0,
                    doseOutGr = 36.0,
                    ratio = 2.0,
                    brewTimeMs = 30_000,
                    profileUsed = "Adaptive",
                    rating = 5,
                ),
                ShotRecord(
                    id = 2,
                    timestamp = 2,
                    coffeeBeanId = 7,
                    doseInGr = 18.0,
                    doseOutGr = 45.0,
                    ratio = 2.5,
                    brewTimeMs = 31_000,
                    profileUsed = "Adaptive",
                ),
            ),
        )

        assertEquals("Return to your best yield ratio", recommendation.title)
        assertEquals("PERSONAL", recommendation.confidence)
        assertTrue(recommendation.detail.contains("2.0:1"))
    }

    @Test
    fun incompleteShotsAreExcludedFromRecommendations() {
        val recommendation = BrewCoach.recommend(
            listOf(
                ShotRecord(
                    id = 1,
                    timestamp = 1,
                    brewTimeMs = 10_000,
                    profileUsed = "Adaptive",
                    balanceTaste = "sour",
                    incomplete = true,
                ),
            ),
        )

        assertEquals("Build your extraction baseline", recommendation.title)
    }
}
