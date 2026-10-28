package com.beanpilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beanpilot.data.local.entity.ShotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {
    @Query("SELECT * FROM shots ORDER BY timestamp DESC LIMIT 20")
    fun observeRecentShots(): Flow<List<ShotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shot: ShotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shots: List<ShotEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(shot: ShotEntity)

    @Query(
        """
        UPDATE shots
        SET timestamp = :timestamp,
            brewTimeMs = :brewTimeMs,
            profileUsed = :profileUsed,
            incomplete = :incomplete
        WHERE sourceId = :sourceId
        """,
    )
    suspend fun updateImportedShot(
        sourceId: String,
        timestamp: Long,
        brewTimeMs: Long,
        profileUsed: String,
        incomplete: Boolean,
    )

    @Update
    suspend fun update(shot: ShotEntity)

}
