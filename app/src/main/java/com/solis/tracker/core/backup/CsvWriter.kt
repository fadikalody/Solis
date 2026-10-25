package com.solis.tracker.core.backup

object CsvWriter {
    private const val UTF8_BOM = "\uFEFF"
    private val INJECTION_PREFIXES = listOf("=", "+", "-", "@")

    fun generateCsv(headers: List<String>, rows: List<List<String>>): String {
        return buildString {
            append(UTF8_BOM)
            append(formatRow(headers))
            append("\r\n")
            rows.forEach { row ->
                append(formatRow(row))
                append("\r\n")
            }
        }
    }

    private fun formatRow(row: List<String>): String {
        return row.joinToString(",") { cell ->
            escapeCell(cell)
        }
    }

    private fun escapeCell(cell: String): String {
        var escaped = cell
        
        // Prevent formula injection
        if (INJECTION_PREFIXES.any { escaped.startsWith(it) }) {
            escaped = "'$escaped"
        }

        // RFC-4180 quoting for commas, quotes, and newlines
        if (escaped.contains("\"") || escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
            escaped = "\"${escaped.replace("\"", "\"\"")}\""
        }
        
        return escaped
    }
}
