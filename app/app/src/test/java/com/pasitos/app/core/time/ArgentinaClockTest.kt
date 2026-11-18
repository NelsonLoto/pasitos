package com.pasitos.app.core.time

import com.pasitos.app.core.database.Converters
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class ArgentinaClockTest {
    @Test fun `ADR-26 hoy no cambia al usar un telefono en otra zona`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val clock = Clock.fixed(Instant.parse("2026-11-18T02:59:59Z"), ZoneId.of("Pacific/Auckland"))
            assertEquals(LocalDate.of(2026, 11, 17), ArgentinaClock.today(clock))
            assertEquals(LocalDate.of(2026, 11, 18), ArgentinaClock.today(Clock.offset(clock, java.time.Duration.ofSeconds(1))))
        } finally { TimeZone.setDefault(original) }
    }

    @Test fun `ADR-26 fechas y momentos tienen representaciones distintas y reversibles`() {
        val converters = Converters()
        val date = LocalDate.of(2026, 11, 18)
        assertEquals("2026-11-18", converters.dateToText(date))
        assertEquals(date, converters.textToDate(converters.dateToText(date)))
        val instant = Instant.parse("2026-11-18T01:00:00.123Z")
        assertEquals(instant, converters.millisToInstant(converters.instantToMillis(instant)))
        assertNull(converters.textToDate(null))
        assertNull(converters.millisToInstant(null))
    }
}
