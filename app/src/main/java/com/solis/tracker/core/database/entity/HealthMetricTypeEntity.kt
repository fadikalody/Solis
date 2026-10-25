package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_metric_type")
data class HealthMetricTypeEntity(
    @PrimaryKey val id: String,
    val key: String,
    val name: String,
    val unit: String,
    val valueKind: String,
    val isBuiltIn: Boolean = false,
    val goalValue: Double? = null
)
