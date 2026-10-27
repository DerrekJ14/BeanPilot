package com.beanpilot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shots")
data class ShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
