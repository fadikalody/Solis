package com.solis.tracker.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvWriterTest {

    @Test
    fun `generateCsv correctly formats standard fields`() {
        val headers = listOf("ID", "Name", "Value")
        val rows = listOf(
            listOf("1", "Task 1", "100"),
            listOf("2", "Task 2", "200")
        )
        val csv = CsvWriter.generateCsv(headers, rows)
        
        // Includes BOM
        assertTrue(csv.startsWith("\uFEFF"))
        
        val lines = csv.split("\r\n").drop(1).filter { it.isNotEmpty() }
        assertEquals(2, lines.size)
        assertEquals("1,Task 1,100", lines[0])
        assertEquals("2,Task 2,200", lines[1])
    }

    @Test
    fun `generateCsv escapes quotes and commas correctly`() {
        val headers = listOf("Notes")
        val rows = listOf(
            listOf("Hello, World"),
            listOf("He said \"Hello\"")
        )
        val csv = CsvWriter.generateCsv(headers, rows)
        
        val lines = csv.split("\r\n").drop(1).filter { it.isNotEmpty() }
        assertEquals("\"Hello, World\"", lines[0])
        assertEquals("\"He said \"\"Hello\"\"\"", lines[1])
    }

    @Test
    fun `generateCsv prefixes formula injection attempts`() {
        val headers = listOf("Input")
        val rows = listOf(
            listOf("=1+2"),
            listOf("+A1"),
            listOf("-100"),
            listOf("@SUM(A1:A2)")
        )
        val csv = CsvWriter.generateCsv(headers, rows)
        
        val lines = csv.split("\r\n").drop(1).filter { it.isNotEmpty() }
        assertEquals("'=1+2", lines[0])
        assertEquals("'+A1", lines[1])
        assertEquals("'-100", lines[2])
        assertEquals("'@SUM(A1:A2)", lines[3])
    }
}
