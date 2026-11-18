package com.pasitos.app.core.time

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Fecha del calendario argentino, independiente de la zona configurada en el teléfono. */
object ArgentinaClock {
    val zone: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    fun today(clock: Clock = Clock.systemUTC()): LocalDate = LocalDate.now(clock.withZone(zone))
}
