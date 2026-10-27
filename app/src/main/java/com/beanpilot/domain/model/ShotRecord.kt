package com.beanpilot.domain.model

data class ShotRecord(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val doseInGr: Double,
    val doseOutGr: Double,
    val brewTimeMs: Long,
    val ratio: Double,
    val profileUsed: String,
    val grinderSetting: Double,
    val rating: Int,
    val notes: String? = null,
)
