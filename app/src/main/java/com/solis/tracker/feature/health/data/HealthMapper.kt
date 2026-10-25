package com.solis.tracker.feature.health.data

import com.solis.tracker.core.database.entity.HealthEntryEntity
import com.solis.tracker.core.database.entity.HealthMetricTypeEntity
import com.solis.tracker.feature.health.domain.model.HealthEntry
import com.solis.tracker.feature.health.domain.model.MetricType
import com.solis.tracker.feature.health.domain.model.ValueKind

fun HealthMetricTypeEntity.toDomain() = MetricType(
    id = id,
    key = key,
    name = name,
    unit = unit,
    valueKind = runCatching { ValueKind.valueOf(valueKind) }.getOrElse { ValueKind.NUMBER },
    isBuiltIn = isBuiltIn,
    goalValue = goalValue
)

fun MetricType.toEntity() = HealthMetricTypeEntity(
    id = id,
    key = key,
    name = name,
    unit = unit,
    valueKind = valueKind.name,
    isBuiltIn = isBuiltIn,
    goalValue = goalValue
)

fun HealthEntryEntity.toDomain() = HealthEntry(
    id = id,
    metricTypeId = metricTypeId,
    value = value,
    recordedAt = recordedAt,
    note = note
)

fun HealthEntry.toEntity() = HealthEntryEntity(
    id = id,
    metricTypeId = metricTypeId,
    value = value,
    recordedAt = recordedAt,
    note = note
)
