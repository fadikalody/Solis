package com.solis.tracker.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun instantConversion_roundTrip() {
        val now = Instant.ofEpochMilli(1700000000000L)
        val millis = converters.toEpochMillis(now)
        val converted = converters.fromEpochMillis(millis)
        assertEquals(now, converted)
    }

    @Test
    fun instantConversion_nullHandled() {
        assertNull(converters.toEpochMillis(null))
        assertNull(converters.fromEpochMillis(null))
    }

    @Test
    fun localDateConversion_roundTrip() {
        val date = LocalDate.of(2026, 3, 8)
        val epochDay = converters.toEpochDay(date)
        val converted = converters.fromEpochDay(epochDay)
        assertEquals(date, converted)
    }

    @Test
    fun localDateConversion_nullHandled() {
        assertNull(converters.toEpochDay(null))
        assertNull(converters.fromEpochDay(null))
    }
}
