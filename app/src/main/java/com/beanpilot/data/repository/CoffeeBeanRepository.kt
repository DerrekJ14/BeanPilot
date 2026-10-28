package com.beanpilot.data.repository

import com.beanpilot.data.local.dao.CoffeeBeanDao
import com.beanpilot.data.local.entity.CoffeeBeanEntity
import com.beanpilot.domain.model.CoffeeBean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CoffeeBeanRepository @Inject constructor(
    private val coffeeBeanDao: CoffeeBeanDao,
) {
    fun coffeeBeans(): Flow<List<CoffeeBean>> =
        coffeeBeanDao.observeAll().map { beans -> beans.map { it.toDomain() } }

    suspend fun save(bean: CoffeeBean): Long =
        coffeeBeanDao.insert(
            CoffeeBeanEntity(
                id = bean.id,
                roaster = bean.roaster.trim(),
                beanName = bean.beanName.trim(),
                origin = bean.origin.cleanOrNull(),
                process = bean.process.cleanOrNull(),
                roastDate = bean.roastDate.cleanOrNull(),
                notes = bean.notes.cleanOrNull(),
            ),
        )

    suspend fun findByName(beanName: String): CoffeeBean? =
        coffeeBeanDao.findByName(beanName.trim())?.toDomain()

    suspend fun delete(coffeeBeanId: Long) {
        val bean = coffeeBeanDao.findById(coffeeBeanId) ?: return
        coffeeBeanDao.delete(bean)
    }

    private fun CoffeeBeanEntity.toDomain(): CoffeeBean =
        CoffeeBean(
            id = id,
            roaster = roaster,
            beanName = beanName,
            origin = origin,
            process = process,
            roastDate = roastDate,
            notes = notes,
        )

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
