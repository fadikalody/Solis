package com.solis.tracker.feature.health.backup

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.core.backup.CsvFile
import com.solis.tracker.core.backup.CsvWriter
import com.solis.tracker.core.backup.ImportReport
import com.solis.tracker.core.backup.ImportStrategy
import com.solis.tracker.feature.health.domain.HealthRepository
import com.solis.tracker.feature.health.domain.model.HealthEntry
import com.solis.tracker.feature.health.domain.model.MetricType
import com.solis.tracker.feature.health.domain.model.ValueKind
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject

class HealthBackupContributor @Inject constructor(
    private val repository: HealthRepository
) : BackupContributor {
    override val key: String = "health"
    override val schemaVersion: Int = 1

    override suspend fun export(): JsonElement {
        val metrics = repository.getAllMetricTypes().first()
        val entries = repository.getAllEntries().first()
        
        return buildJsonObject {
            put("metrics", buildJsonArray {
                metrics.forEach { m ->
                    add(buildJsonObject {
                        put("id", m.id)
                        put("key", m.key)
                        put("name", m.name)
                        put("unit", m.unit)
                        put("valueKind", m.valueKind.name)
                        put("isBuiltIn", m.isBuiltIn)
                        m.goalValue?.let { put("goalValue", it) }
                    })
                }
            })
            put("entries", buildJsonArray {
                entries.forEach { e ->
                    add(buildJsonObject {
                        put("id", e.id)
                        put("metricTypeId", e.metricTypeId)
                        put("value", e.value)
                        put("recordedAt", e.recordedAt.toEpochMilli())
                        put("note", e.note)
                    })
                }
            })
        }
    }

    override suspend fun import(data: JsonElement, strategy: ImportStrategy): ImportReport {
        var added = 0
        var updated = 0
        
        val obj = data.jsonObject
        
        // Metrics
        val metricsArray = obj["metrics"]?.jsonArray
        if (metricsArray != null) {
            val existingMetrics = repository.getAllMetricTypes().first().associateBy { it.id }
            for (elem in metricsArray) {
                val item = elem.jsonObject
                val id = item["id"]?.jsonPrimitive?.content ?: continue
                if (existingMetrics[id] == null || strategy == ImportStrategy.REPLACE) {
                    val m = MetricType(
                        id = id,
                        key = item["key"]?.jsonPrimitive?.content ?: id,
                        name = item["name"]?.jsonPrimitive?.content ?: "Unknown",
                        unit = item["unit"]?.jsonPrimitive?.content ?: "",
                        valueKind = runCatching { ValueKind.valueOf(item["valueKind"]?.jsonPrimitive?.content ?: "NUMBER") }.getOrDefault(ValueKind.NUMBER),
                        isBuiltIn = item["isBuiltIn"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                        goalValue = item["goalValue"]?.jsonPrimitive?.content?.toDoubleOrNull()
                    )
                    repository.saveMetricType(m)
                    if (existingMetrics[id] == null) added++ else updated++
                }
            }
        }
        
        // Entries
        val entriesArray = obj["entries"]?.jsonArray
        if (entriesArray != null) {
            // Note: health entries don't have updatedAt currently. We use ID matching for merge/replace.
            val existingEntries = repository.getAllEntries().first().associateBy { it.id }
            
            for (elem in entriesArray) {
                val item = elem.jsonObject
                val id = item["id"]?.jsonPrimitive?.content ?: continue
                
                val existing = existingEntries[id]
                
                if (existing == null || strategy == ImportStrategy.REPLACE) {
                    val entry = HealthEntry(
                        id = id,
                        metricTypeId = item["metricTypeId"]?.jsonPrimitive?.content ?: continue,
                        value = item["value"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        recordedAt = Instant.ofEpochMilli(item["recordedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: Instant.now().toEpochMilli()),
                        note = item["note"]?.jsonPrimitive?.content ?: ""
                    )
                    repository.saveEntry(entry)
                    if (existing == null) added++ else updated++
                }
            }
        }
        
        return ImportReport(added = added, updated = updated)
    }

    override suspend fun exportCsv(): List<CsvFile> {
        val entries = repository.getAllEntries().first()
        val metricsMap = repository.getAllMetricTypes().first().associateBy { it.id }
        
        val rows = entries.map { e ->
            listOf(
                e.id,
                metricsMap[e.metricTypeId]?.name ?: e.metricTypeId,
                e.value.toString(),
                metricsMap[e.metricTypeId]?.unit ?: "",
                e.note,
                e.recordedAt.toString()
            )
        }
        val content = CsvWriter.generateCsv(
            headers = listOf("ID", "Metric", "Value", "Unit", "Note", "Date"),
            rows = rows
        )
        return listOf(CsvFile("health.csv", content))
    }
}
