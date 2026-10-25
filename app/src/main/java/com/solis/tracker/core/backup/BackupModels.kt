package com.solis.tracker.core.backup

import kotlinx.serialization.json.JsonElement

enum class ImportStrategy {
    MERGE, REPLACE
}

data class ImportReport(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val error: String? = null
) {
    operator fun plus(other: ImportReport) = ImportReport(
        added = added + other.added,
        updated = updated + other.updated,
        skipped = skipped + other.skipped,
        failed = failed + other.failed,
        error = error ?: other.error
    )
}

data class CsvFile(
    val filename: String,
    val content: String
)

interface BackupContributor {
    val key: String
    val schemaVersion: Int
    suspend fun export(): JsonElement
    suspend fun import(data: JsonElement, strategy: ImportStrategy): ImportReport
    suspend fun exportCsv(): List<CsvFile>
}
