package com.pasitos.backend.common

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reloj de la aplicación (ADR-26). "Hoy" se calcula siempre en hora argentina, sin depender de la
 * zona del servidor: un servidor en UTC ya está en el día siguiente entre las 21:00 y la
 * medianoche de Argentina. Recibe un [Clock] para poder fijar la hora en las pruebas.
 */
class ArgentinaClock(private val clock: Clock = Clock.systemUTC()) {

    /** Momento actual, para guardar como `timestamptz`. */
    fun now(): Instant = clock.instant()

    /** Fecha de hoy en Argentina, para validar y comparar fechas del calendario. */
    fun today(): LocalDate = LocalDate.now(clock.withZone(ZONE))

    companion object {
        val ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    }
}
