package com.beanpilot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beanpilot.data.local.dao.CoffeeBeanDao
import com.beanpilot.data.local.dao.ShotDao
import com.beanpilot.data.local.entity.CoffeeBeanEntity
import com.beanpilot.data.local.entity.ShotEntity

@Database(
    entities = [CoffeeBeanEntity::class, ShotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeBeanDao(): CoffeeBeanDao
    abstract fun shotDao(): ShotDao
}
