package com.beanpilot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.beanpilot.data.local.dao.CoffeeBeanDao
import com.beanpilot.data.local.dao.ShotDao
import com.beanpilot.data.local.entity.CoffeeBeanEntity
import com.beanpilot.data.local.entity.ShotEntity

@Database(
    entities = [CoffeeBeanEntity::class, ShotEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeBeanDao(): CoffeeBeanDao
    abstract fun shotDao(): ShotDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shots ADD COLUMN sourceId TEXT")
                db.execSQL(
                    "ALTER TABLE shots ADD COLUMN incomplete INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX index_shots_sourceId ON shots(sourceId)",
                )
            }

        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shots ADD COLUMN coffeeName TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shots ADD COLUMN balanceTaste TEXT")
            }
        }
    }
}
