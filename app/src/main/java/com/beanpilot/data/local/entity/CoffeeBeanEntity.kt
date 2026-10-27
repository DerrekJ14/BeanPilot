package com.beanpilot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coffee_beans")
data class CoffeeBeanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roaster: String,
    val beanName: String,
    val origin: String? = null,
    val process: String? = null,
    val roastDate: String? = null,
    val notes: String? = null,
)
