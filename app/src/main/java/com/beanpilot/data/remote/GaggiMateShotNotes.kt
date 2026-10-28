package com.beanpilot.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class GaggiMateShotNotes(
    val id: String? = null,
    val rating: Int? = null,
    val beanType: String? = null,
    val doseIn: String? = null,
    val doseOut: String? = null,
    val ratio: String? = null,
    val grindSetting: String? = null,
    val balanceTaste: String? = null,
    val notes: String? = null,
)

fun GaggiMateShotNotes.mergeReview(
    sourceId: String,
    rating: Int?,
    coffeeName: String?,
    doseInGr: Double?,
    doseOutGr: Double?,
    ratio: Double?,
    grinderSetting: Double?,
    balanceTaste: String?,
    notes: String?,
): GaggiMateShotNotes = copy(
    id = sourceId,
    rating = rating ?: 0,
    beanType = coffeeName.nonBlankOr(beanType),
    doseIn = doseInGr?.formatForGaggiMate() ?: doseIn,
    doseOut = doseOutGr?.formatForGaggiMate() ?: doseOut,
    ratio = ratio?.formatForGaggiMate() ?: this.ratio,
    grindSetting = grinderSetting?.formatForGaggiMate() ?: grindSetting,
    balanceTaste = balanceTaste.nonBlankOr(this.balanceTaste),
    notes = notes.nonBlankOr(this.notes),
)

private fun String?.nonBlankOr(fallback: String?): String? =
    this?.takeIf(String::isNotBlank) ?: fallback

private fun Double.formatForGaggiMate(): String =
    java.math.BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
