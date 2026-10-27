package com.beanpilot.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shots",
    indices = [Index(value = ["sourceId"], unique = true)],
)
data class ShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val doseInGr: Double? = null,
    val doseOutGr: Double? = null,
    val brewTimeMs: Long,
    val ratio: Double? = null,
    val profileUsed: String,
    val grinderSetting: Double? = null,
    val rating: Int? = null,
    val incomplete: Boolean = false,
    val notes: String? = null,
)
