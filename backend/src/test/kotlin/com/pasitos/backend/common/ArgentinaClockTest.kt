package com.pasitos.backend.common

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ArgentinaClockTest {

    @Test
    fun `ADR-26 hoy es el día argentino aunque en UTC ya sea el día siguiente`() {
        // 14/10 a las 23:30 en Argentina = 15/10 a las 02:30 en UTC.
        val clock = ArgentinaClock(Clock.fixed(Instant.parse("2026-10-15T02:30:00Z"), ZoneOffset.UTC))

        assertEquals(LocalDate.parse("2026-10-14"), clock.today())
    }

    @Test
    fun `ADR-26 a la medianoche argentina ya cambia el día`() {
        val clock = ArgentinaClock(Clock.fixed(Instant.parse("2026-10-15T03:00:00Z"), ZoneOffset.UTC))

        assertEquals(LocalDate.parse("2026-10-15"), clock.today())
    }
}
