package com.beanpilot.data.repository

import com.beanpilot.data.local.dao.ShotDao
import com.beanpilot.data.local.entity.ShotEntity
import com.beanpilot.data.remote.GaggiMateShot
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
                    sourceId = entity.sourceId,
                    timestamp = entity.timestamp,
                    coffeeName = entity.coffeeName,
                    doseInGr = entity.doseInGr,
                    doseOutGr = entity.doseOutGr,
                    brewTimeMs = entity.brewTimeMs,
                    ratio = entity.ratio,
                    profileUsed = entity.profileUsed,
                    grinderSetting = entity.grinderSetting,
                    rating = entity.rating,
                    balanceTaste = entity.balanceTaste,
                    incomplete = entity.incomplete,
                    notes = entity.notes,
                )
            }
        }

    suspend fun saveShot(shot: ShotRecord) {
        shotDao.insert(
            ShotEntity(
                id = shot.id,
                sourceId = shot.sourceId,
                timestamp = shot.timestamp,
                coffeeName = shot.coffeeName,
                doseInGr = shot.doseInGr,
                doseOutGr = shot.doseOutGr,
                brewTimeMs = shot.brewTimeMs,
                ratio = shot.ratio,
                profileUsed = shot.profileUsed,
                grinderSetting = shot.grinderSetting,
                rating = shot.rating,
                balanceTaste = shot.balanceTaste,
                incomplete = shot.incomplete,
                notes = shot.notes,
            )
        )
    }

    suspend fun importMachineShots(shots: List<GaggiMateShot>) {
        shots.forEach { shot ->
            shotDao.insertIfMissing(
                ShotEntity(
                    sourceId = shot.sourceId,
                    timestamp = shot.timestampSeconds * 1_000,
                    doseOutGr = shot.doseOutGr,
                    brewTimeMs = shot.durationMs,
                    profileUsed = shot.profileName,
                    rating = shot.rating,
                    incomplete = shot.incomplete,
                ),
            )
            shotDao.updateImportedShot(
                sourceId = shot.sourceId,
                timestamp = shot.timestampSeconds * 1_000,
                brewTimeMs = shot.durationMs,
                profileUsed = shot.profileName,
                incomplete = shot.incomplete,
            )
        }
    }

    suspend fun updateShotReview(shot: ShotRecord) {
        shotDao.update(
            ShotEntity(
                id = shot.id,
                sourceId = shot.sourceId,
                timestamp = shot.timestamp,
                coffeeName = shot.coffeeName,
                doseInGr = shot.doseInGr,
                doseOutGr = shot.doseOutGr,
                brewTimeMs = shot.brewTimeMs,
                ratio = shot.ratio,
                profileUsed = shot.profileUsed,
                grinderSetting = shot.grinderSetting,
                rating = shot.rating,
                incomplete = shot.incomplete,
                notes = shot.notes,
            ),
        )
    }
}
