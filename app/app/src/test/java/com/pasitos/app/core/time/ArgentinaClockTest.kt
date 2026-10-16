package com.pasitos.app.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ArgentinaClockTest {

    @Test
    fun `ADR-26 a las 22 de Argentina sigue siendo hoy aunque en UTC ya sea mañana`() {
        val clock = ArgentinaClock(Clock.fixed(Instant.parse("2026-10-15T01:00:00Z"), ZoneOffset.UTC))

        assertEquals(LocalDate.of(2026, 10, 14), clock.today())
    }

    @Test
    fun `ADR-26 la fecha no depende de la zona configurada en el teléfono`() {
        // En Tokio ya es 15/10 a las 08:30; en Argentina, 14/10 a las 20:30
        val clock = ArgentinaClock(Clock.fixed(Instant.parse("2026-10-14T23:30:00Z"), ZoneId.of("Asia/Tokyo")))

        assertEquals(LocalDate.of(2026, 10, 14), clock.today())
    }

    @Test
    fun `ADR-26 a la medianoche de Argentina cambia el día`() {
        val clock = ArgentinaClock(Clock.fixed(Instant.parse("2026-10-15T03:00:00Z"), ZoneOffset.UTC))

        assertEquals(LocalDate.of(2026, 10, 15), clock.today())
    }
}
