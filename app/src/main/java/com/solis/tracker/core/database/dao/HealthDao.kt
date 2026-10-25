package com.solis.tracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.solis.tracker.core.database.entity.HealthEntryEntity
import com.solis.tracker.core.database.entity.HealthMetricTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_metric_type ORDER BY isBuiltIn DESC, name ASC")
    fun getAllMetricTypes(): Flow<List<HealthMetricTypeEntity>>

    @Query("SELECT * FROM health_metric_type WHERE id = :id")
    suspend fun getMetricTypeById(id: String): HealthMetricTypeEntity?

    @Query("SELECT * FROM health_entry ORDER BY recordedAt DESC")
    fun getAllEntries(): Flow<List<HealthEntryEntity>>
    
    @Query("SELECT * FROM health_entry WHERE metricTypeId = :metricTypeId ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentEntriesForMetric(metricTypeId: String, limit: Int = 1): Flow<List<HealthEntryEntity>>

    @Query("SELECT * FROM health_entry WHERE metricTypeId = :metricTypeId AND recordedAt >= :startDate AND recordedAt <= :endDate ORDER BY recordedAt ASC")
    fun getEntriesForMetricBetween(metricTypeId: String, startDate: Long, endDate: Long): Flow<List<HealthEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HealthEntryEntity)
    
    @Query("DELETE FROM health_entry WHERE id = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMetricTypes(metricTypes: List<HealthMetricTypeEntity>)

    @Query("SELECT COUNT(*) FROM health_metric_type")
    suspend fun getMetricTypeCount(): Int
}
