package com.beanpilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beanpilot.data.local.entity.CoffeeBeanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeBeanDao {
    @Query("SELECT * FROM coffee_beans ORDER BY beanName ASC")
    fun observeAll(): Flow<List<CoffeeBeanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bean: CoffeeBeanEntity): Long

    @Query("SELECT * FROM coffee_beans WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CoffeeBeanEntity?

    @Query("SELECT * FROM coffee_beans WHERE lower(beanName) = lower(:beanName) LIMIT 1")
    suspend fun findByName(beanName: String): CoffeeBeanEntity?

    @Delete
    suspend fun delete(bean: CoffeeBeanEntity)
}
