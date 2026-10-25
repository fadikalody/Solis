package com.solis.tracker.feature.health.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solis.tracker.core.database.AppDatabase
import com.solis.tracker.core.database.dao.HealthDao
import com.solis.tracker.core.database.entity.HealthEntryEntity
import com.solis.tracker.core.database.entity.HealthMetricTypeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class HealthDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var healthDao: HealthDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        healthDao = database.healthDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetMetricTypes() = runBlocking {
        val metric = HealthMetricTypeEntity(
            id = "weight_id",
            key = "weight",
            name = "Weight",
            unit = "kg",
            valueKind = "NUMBER",
            isBuiltIn = true,
            goalValue = null
        )
        healthDao.insertMetricTypes(listOf(metric))

        val types = healthDao.getAllMetricTypes().first()
        assertEquals(1, types.size)
        assertEquals("Weight", types[0].name)
        
        val count = healthDao.getMetricTypeCount()
        assertEquals(1, count)
    }

    @Test
    fun insertAndGetEntriesForMetricBetween() = runBlocking {
        val metric = HealthMetricTypeEntity("weight_id", "weight", "Weight", "kg", "NUMBER", true, null)
        healthDao.insertMetricTypes(listOf(metric))

        val now = Instant.now()
        val yesterday = now.minus(1, ChronoUnit.DAYS)
        val lastWeek = now.minus(7, ChronoUnit.DAYS)

        healthDao.insertEntry(HealthEntryEntity("e1", "weight_id", 70.0, now, ""))
        healthDao.insertEntry(HealthEntryEntity("e2", "weight_id", 71.0, yesterday, ""))
        healthDao.insertEntry(HealthEntryEntity("e3", "weight_id", 72.0, lastWeek, ""))

        val start = now.minus(2, ChronoUnit.DAYS).toEpochMilli()
        val end = now.toEpochMilli()
        
        // Should only get 'yesterday' and 'now'
        val entries = healthDao.getEntriesForMetricBetween("weight_id", start, end).first()
        
        assertEquals(2, entries.size)
        // Ordered by recordedAt ASC
        assertEquals("e2", entries[0].id)
        assertEquals("e1", entries[1].id)
    }
    
    @Test
    fun getRecentEntriesForMetric_respectsLimit() = runBlocking {
        val metric = HealthMetricTypeEntity("weight_id", "weight", "Weight", "kg", "NUMBER", true, null)
        healthDao.insertMetricTypes(listOf(metric))

        val now = Instant.now()
        
        healthDao.insertEntry(HealthEntryEntity("e1", "weight_id", 70.0, now, ""))
        healthDao.insertEntry(HealthEntryEntity("e2", "weight_id", 71.0, now.minusSeconds(10), ""))
        healthDao.insertEntry(HealthEntryEntity("e3", "weight_id", 72.0, now.minusSeconds(20), ""))

        // Limit 2
        val recent = healthDao.getRecentEntriesForMetric("weight_id", 2).first()
        
        assertEquals(2, recent.size)
        // Ordered by recordedAt DESC, so e1 then e2
        assertEquals("e1", recent[0].id)
        assertEquals("e2", recent[1].id)
    }
}
