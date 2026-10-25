package com.solis.tracker.feature.health.data

import com.solis.tracker.core.common.AppDispatchers
import com.solis.tracker.core.common.Dispatcher
import com.solis.tracker.core.database.dao.HealthDao
import com.solis.tracker.core.database.entity.HealthMetricTypeEntity
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.feature.health.domain.HealthRepository
import com.solis.tracker.feature.health.domain.model.HealthEntry
import com.solis.tracker.feature.health.domain.model.MetricType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val healthDao: HealthDao,
    private val settingsRepository: SettingsRepository,
    @Dispatcher(AppDispatchers.Io) private val ioDispatcher: CoroutineDispatcher
) : HealthRepository {
    
    override fun getAllMetricTypes(): Flow<List<MetricType>> {
        return healthDao.getAllMetricTypes().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMetricTypeById(id: String): MetricType? = withContext(ioDispatcher) {
        healthDao.getMetricTypeById(id)?.toDomain()
    }

    override suspend fun saveMetricType(metricType: MetricType) = withContext(ioDispatcher) {
        healthDao.insertMetricTypes(listOf(metricType.toEntity()))
    }

    override fun getAllEntries(): Flow<List<HealthEntry>> {
        return healthDao.getAllEntries().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentEntriesForMetric(metricTypeId: String, limit: Int): Flow<List<HealthEntry>> {
        return healthDao.getRecentEntriesForMetric(metricTypeId, limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getEntriesForMetricBetween(metricTypeId: String, start: Instant, end: Instant): Flow<List<HealthEntry>> {
        return healthDao.getEntriesForMetricBetween(metricTypeId, start.toEpochMilli(), end.toEpochMilli())
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveEntry(entry: HealthEntry) = withContext(ioDispatcher) {
        healthDao.insertEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(entryId: String) = withContext(ioDispatcher) {
        healthDao.deleteEntry(entryId)
    }

    override suspend fun seedBuiltInMetricsIfNeeded() = withContext(ioDispatcher) {
        val settings = settingsRepository.userSettings.first()
        if (!settings.isHealthSeeded || healthDao.getMetricTypeCount() == 0) {
            val builtIns = listOf(
                HealthMetricTypeEntity("weight_id", "weight", "Weight", "kg", "NUMBER", true, null),
                HealthMetricTypeEntity("water_id", "water", "Water", "ml", "NUMBER", true, 2000.0),
                HealthMetricTypeEntity("sleep_id", "sleep", "Sleep", "min", "DURATION", true, 480.0), // 8 hours
                HealthMetricTypeEntity("mood_id", "mood", "Mood", "1-5", "SCALE", true, null),
                HealthMetricTypeEntity("exercise_id", "exercise", "Exercise", "min", "DURATION", true, 30.0)
            )
            healthDao.insertMetricTypes(builtIns)
            settingsRepository.setHealthSeeded(true)
        }
    }
}
