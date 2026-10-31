package com.beanpilot.domain.recommendation

import com.beanpilot.domain.model.ShotRecord
import com.beanpilot.domain.model.CoffeeBean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrewCoachTest {
    @Test
    fun emptyHistoryAsksForReviewedShotData() {
        val recommendation = BrewCoach.recommend(emptyList())

        assertEquals("Choose a coffee to focus your profile", recommendation.title)
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
    fun numericTasteBalanceAtSourEndRecommendsSlowingExtraction() {
        val recommendation = BrewCoach.recommend(
            listOf(
                ShotRecord(
                    id = 1,
                    brewTimeMs = 30_000,
                    profileUsed = "Adaptive",
                    balanceTaste = "9",
                ),
            ),
        )

        assertEquals("Slow the extraction slightly", recommendation.title)
    }

    @Test
    fun numericTasteBalanceAtBitterEndRecommendsSpeedingExtraction() {
        val recommendation = BrewCoach.recommend(
            listOf(
                ShotRecord(
                    id = 1,
                    brewTimeMs = 30_000,
                    profileUsed = "Adaptive",
                    balanceTaste = "2",
                ),
            ),
        )

        assertEquals("Speed the extraction slightly", recommendation.title)
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

        assertEquals("Choose a coffee to focus your profile", recommendation.title)
    }

    @Test
    fun selectedCoffeeOnlyUsesShotsLinkedToThatCoffee() {
        val recommendation = BrewCoach.recommend(
            shots = listOf(
                ShotRecord(
                    id = 1,
                    timestamp = 2,
                    coffeeBeanId = 8,
                    brewTimeMs = 18_000,
                    profileUsed = "Adaptive",
                    balanceTaste = "sour",
                ),
                ShotRecord(
                    id = 2,
                    timestamp = 1,
                    coffeeBeanId = 7,
                    brewTimeMs = 34_000,
                    profileUsed = "Adaptive",
                    balanceTaste = "balanced",
                ),
            ),
            selectedCoffee = CoffeeBean(id = 7, roaster = "Roaster", beanName = "House"),
        )

        assertEquals("Tune House one variable at a time", recommendation.title)
    }

    @Test
    fun selectedCoffeeWithoutHistoryRequestsBeanSpecificBaseline() {
        val recommendation = BrewCoach.recommend(
            shots = emptyList(),
            selectedCoffee = CoffeeBean(id = 7, roaster = "Roaster", beanName = "House"),
        )

        assertEquals("Start a profile for House", recommendation.title)
        assertTrue(recommendation.detail.contains("House"))
    }
}
