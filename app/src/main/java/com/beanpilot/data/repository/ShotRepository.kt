package com.beanpilot.data.repository

import com.beanpilot.data.local.dao.ShotDao
import com.beanpilot.data.local.entity.ShotEntity
import com.beanpilot.domain.model.ShotRecord
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ShotRepository @Inject constructor(
    private val shotDao: ShotDao,
) {
    fun recentShots(): Flow<List<ShotRecord>> =
        shotDao.observeRecentShots().map { entities ->
            entities.map { entity ->
                ShotRecord(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    doseInGr = entity.doseInGr,
                    doseOutGr = entity.doseOutGr,
                    brewTimeMs = entity.brewTimeMs,
                    ratio = entity.ratio,
                    profileUsed = entity.profileUsed,
                    grinderSetting = entity.grinderSetting,
                    rating = entity.rating,
                    notes = entity.notes,
                )
            }
        }

    suspend fun saveShot(shot: ShotRecord) {
        shotDao.insert(
            ShotEntity(
                id = shot.id,
                timestamp = shot.timestamp,
                doseInGr = shot.doseInGr,
                doseOutGr = shot.doseOutGr,
                brewTimeMs = shot.brewTimeMs,
                ratio = shot.ratio,
                profileUsed = shot.profileUsed,
                grinderSetting = shot.grinderSetting,
                rating = shot.rating,
                notes = shot.notes,
            )
        )
    }
}
