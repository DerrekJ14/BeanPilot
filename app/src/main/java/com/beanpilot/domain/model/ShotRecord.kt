package com.beanpilot.domain.model

data class ShotRecord(
    val id: Long = 0,
    val sourceId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val coffeeName: String? = null,
    val coffeeBeanId: Long? = null,
    val doseInGr: Double? = null,
    val doseOutGr: Double? = null,
    val brewTimeMs: Long,
    val ratio: Double? = null,
    val profileUsed: String,
    val grinderSetting: Double? = null,
    val rating: Int? = null,
    val balanceTaste: String? = null,
    val bodyRating: Int? = null,
    val incomplete: Boolean = false,
    val notes: String? = null,
)
