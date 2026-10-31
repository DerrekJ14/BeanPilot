package com.beanpilot.domain.recommendation

import com.beanpilot.domain.model.ShotRecord
import com.beanpilot.domain.model.CoffeeBean
import java.util.Locale

data class BrewRecommendation(
    val title: String,
    val detail: String,
    val basis: String,
    val confidence: String,
)

object BrewCoach {
    fun recommend(
        shots: List<ShotRecord>,
        selectedCoffee: CoffeeBean? = null,
    ): BrewRecommendation {
        val completeShots = shots
            .filterNot { it.incomplete }
            .filter { selectedCoffee == null || belongsToCoffee(it, selectedCoffee) }
        val latest = completeShots.maxByOrNull { it.timestamp }
            ?: return BrewRecommendation(
                title = selectedCoffee?.let { "Start a profile for ${it.beanName}" }
                    ?: "Choose a coffee to focus your profile",
                detail = if (selectedCoffee == null) {
                    "Select a bean to compare its shot history and get extraction suggestions focused on that coffee."
                } else {
                    "No completed shots are linked to ${selectedCoffee.beanName} yet. Record dose, yield, taste, " +
                        "and a rating after your next extraction to build a bean-specific baseline."
                },
                basis = if (selectedCoffee == null) "No reviewed shot history yet"
                else "No completed shots linked to this coffee",
                confidence = "LEARNING",
            )

        val bestForCoffee = completeShots
            .asSequence()
            .filter { it.id != latest.id && it.rating != null && it.rating >= 4 }
            .filter { sameCoffee(it, latest) }
            .maxByOrNull { it.rating ?: 0 }

        if (bestForCoffee != null) {
            val currentRatio = latest.ratio ?: ratio(latest)
            val bestRatio = bestForCoffee.ratio ?: ratio(bestForCoffee)
            if (currentRatio != null && bestRatio != null && kotlin.math.abs(currentRatio - bestRatio) >= 0.2) {
                return BrewRecommendation(
                    title = "Return to your best yield ratio",
                    detail = "Your ${bestForCoffee.rating}/5 shot used " +
                        "${bestRatio.oneDecimal()}:1. Keep dose steady and target a similar yield.",
                    basis = "Compared with your highest-rated shot using this coffee",
                    confidence = "PERSONAL",
                )
            }

            val currentDose = latest.doseInGr
            val bestDose = bestForCoffee.doseInGr
            if (currentDose != null && bestDose != null && kotlin.math.abs(currentDose - bestDose) >= 1.0) {
                return BrewRecommendation(
                    title = "Repeat your best dose",
                    detail = "Your ${bestForCoffee.rating}/5 shot used ${bestDose.oneDecimal()} g in. " +
                        "Try that dose while keeping other variables stable.",
                    basis = "Compared with your highest-rated shot using this coffee",
                    confidence = "PERSONAL",
                )
            }

            val timeDelta = (latest.brewTimeMs - bestForCoffee.brewTimeMs) / 1_000.0
            if (kotlin.math.abs(timeDelta) >= 6.0) {
                val direction = if (timeDelta < 0) "slower" else "faster"
                val grindAdvice = if (timeDelta < 0) "slightly finer" else "slightly coarser"
                return BrewRecommendation(
                    title = "Tune shot time toward your best",
                    detail = "This shot ran ${kotlin.math.abs(timeDelta).oneDecimal()} s $direction than your " +
                        "${bestForCoffee.rating}/5 result. Try a $grindAdvice grind and change only one variable.",
                    basis = "Compared with your highest-rated shot using this coffee",
                    confidence = "PERSONAL",
                )
            }

            return BrewRecommendation(
                title = "Stay close to your proven recipe",
                detail = "This shot is near your ${bestForCoffee.rating}/5 result. Keep the dose, grind, and " +
                    "profile steady; use taste notes to decide whether to adjust yield.",
                basis = "Compared with your highest-rated shot using this coffee",
                confidence = "PERSONAL",
            )
        }

        val taste = latest.balanceTaste?.trim()?.lowercase(Locale.ROOT)
        val tasteScore = taste?.toIntOrNull()?.takeIf { it in 1..10 }
        if (taste == "sour" || tasteScore?.let { it >= 8 } == true || latest.brewTimeMs < 20_000) {
            return BrewRecommendation(
                title = "Slow the extraction slightly",
                detail = "The shot was short or marked sour. Try a slightly finer grind, keep dose and profile " +
                    "steady, then compare the taste.",
                basis = "Shot time and taste notes",
                confidence = "STARTING POINT",
            )
        }
        if (taste == "bitter" || tasteScore?.let { it <= 3 } == true || latest.brewTimeMs > 40_000) {
            return BrewRecommendation(
                title = "Speed the extraction slightly",
                detail = "The shot was long or marked bitter. Try a slightly coarser grind, keep dose and profile " +
                    "steady, then compare the taste.",
                basis = "Shot time and taste notes",
                confidence = "STARTING POINT",
            )
        }

        return BrewRecommendation(
            title = selectedCoffee?.let { "Tune ${it.beanName} one variable at a time" }
                ?: "Select a coffee for focused advice",
            detail = if (selectedCoffee == null) {
                "Choose a bean to compare shots against your best results for that coffee."
            } else {
                "Record dose, yield, grinder setting, taste balance, and a rating for this coffee. " +
                    "BeanPilot can then compare it with your best results and suggest profile adjustments."
            },
            basis = if (selectedCoffee == null) "No selected coffee"
            else "Not enough comparable rated shots for this coffee",
            confidence = "LEARNING",
        )
    }

    private fun belongsToCoffee(shot: ShotRecord, coffee: CoffeeBean): Boolean =
        shot.coffeeBeanId == coffee.id ||
            (shot.coffeeBeanId == null &&
                shot.coffeeName?.trim()?.equals(coffee.beanName.trim(), ignoreCase = true) == true)

    private fun sameCoffee(first: ShotRecord, second: ShotRecord): Boolean =
        if (first.coffeeBeanId != null && second.coffeeBeanId != null) {
            first.coffeeBeanId == second.coffeeBeanId
        } else {
            val firstName = first.coffeeName?.trim()?.takeIf(String::isNotEmpty)
            val secondName = second.coffeeName?.trim()?.takeIf(String::isNotEmpty)
            firstName != null && secondName != null && firstName.equals(secondName, ignoreCase = true)
        }

    private fun ratio(shot: ShotRecord): Double? =
        if (shot.doseInGr != null && shot.doseInGr > 0 && shot.doseOutGr != null) {
            shot.doseOutGr / shot.doseInGr
        } else {
            null
        }

    private fun Double.oneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)
}
