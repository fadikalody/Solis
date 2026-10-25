package com.solis.tracker.feature.health.domain.model

import java.time.Instant

enum class ValueKind {
    NUMBER, DURATION, SCALE
}

data class MetricType(
    val id: String,
    val key: String,
    val name: String,
    val unit: String,
    val valueKind: ValueKind,
    val isBuiltIn: Boolean,
    val goalValue: Double?
)

data class HealthEntry(
    val id: String,
    val metricTypeId: String,
    val value: Double,
    val recordedAt: Instant,
    val note: String
)
