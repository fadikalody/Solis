package com.solis.tracker.feature.health.domain

import com.solis.tracker.feature.health.domain.model.HealthEntry
import com.solis.tracker.feature.health.domain.model.MetricType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface HealthRepository {
    fun getAllMetricTypes(): Flow<List<MetricType>>
    suspend fun getMetricTypeById(id: String): MetricType?
    suspend fun saveMetricType(metricType: MetricType)
    
    fun getAllEntries(): Flow<List<HealthEntry>>
    fun getRecentEntriesForMetric(metricTypeId: String, limit: Int): Flow<List<HealthEntry>>
    fun getEntriesForMetricBetween(metricTypeId: String, start: Instant, end: Instant): Flow<List<HealthEntry>>
    
    suspend fun saveEntry(entry: HealthEntry)
    suspend fun deleteEntry(entryId: String)

    suspend fun seedBuiltInMetricsIfNeeded()
}
