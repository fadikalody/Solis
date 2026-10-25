package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "health_entry",
    indices = [
        Index("recordedAt"),
        Index("metricTypeId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = HealthMetricTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["metricTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HealthEntryEntity(
    @PrimaryKey val id: String,
    val metricTypeId: String,
    val value: Double,
    val recordedAt: Instant = Instant.now(),
    val note: String = ""
)
