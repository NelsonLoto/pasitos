package com.pasitos.app.feature.profile

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/** RNF-OFF-04 en hora argentina (UTC−3), sin depender de la zona del teléfono (ADR-26). */
class LastSyncTextTest {

    /** 22/10/2026 a las 18:00 en Argentina. */
    private val now = Instant.parse("2026-10-22T21:00:00Z")

    @Test
    fun `RNF-OFF-04 antes de la primera sincronización lo dice`() {
        assertEquals("Todavía no se sincronizó", lastSyncText(null, pendingChanges = 0, now))
    }

    @Test
    fun `RNF-OFF-04 hoy, ayer y otros días en hora argentina`() {
        assertEquals("Última sincronización: hoy a las 14:32", lastSyncText(Instant.parse("2026-10-22T17:32:00Z"), 0, now))
        // 00:30 UTC del 22 ya es el 21 en Argentina
        assertEquals("Última sincronización: ayer a las 21:30", lastSyncText(Instant.parse("2026-10-22T00:30:00Z"), 0, now))
        assertEquals("Última sincronización: 12/10 a las 09:05", lastSyncText(Instant.parse("2026-10-12T12:05:00Z"), 0, now))
        assertEquals("Última sincronización: 30/12/2025 a las 10:00", lastSyncText(Instant.parse("2025-12-30T13:00:00Z"), 0, now))
    }

    @Test
    fun `RNF-OFF-04 suma los cambios que todavía no se enviaron`() {
        val synced = Instant.parse("2026-10-22T17:32:00Z")

        assertEquals("Última sincronización: hoy a las 14:32 · 1 cambio sin sincronizar", lastSyncText(synced, 1, now))
        assertEquals("Todavía no se sincronizó · 3 cambios sin sincronizar", lastSyncText(null, 3, now))
    }
}
