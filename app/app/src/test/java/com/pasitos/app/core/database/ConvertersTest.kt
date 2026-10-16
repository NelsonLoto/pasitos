package com.pasitos.app.core.database

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `ADR-26 las fechas del calendario se guardan sin hora ni zona`() {
        val date = LocalDate.of(2026, 10, 14)

        assertEquals("2026-10-14", converters.localDateToText(date))
        assertEquals(date, converters.textToLocalDate("2026-10-14"))
    }

    @Test
    fun `ADR-26 los momentos se guardan en UTC con milisegundos`() {
        val instant = Instant.parse("2026-10-14T22:15:30.250Z")

        assertEquals(instant, converters.epochMillisToInstant(converters.instantToEpochMillis(instant)))
    }

    @Test
    fun `los valores nulos quedan nulos`() {
        assertNull(converters.localDateToText(null))
        assertNull(converters.textToLocalDate(null))
        assertNull(converters.instantToEpochMillis(null))
        assertNull(converters.epochMillisToInstant(null))
    }
}
