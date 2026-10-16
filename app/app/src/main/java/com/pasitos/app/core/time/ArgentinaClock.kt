package com.pasitos.app.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reloj de la app (ADR-26). "Hoy" se calcula siempre en hora argentina, sin depender de la zona
 * configurada en el teléfono: un tutor de viaje en otro huso sigue viendo las fechas como en
 * Argentina. Recibe un [Clock] para poder fijar la hora en las pruebas.
 */
@Singleton
class ArgentinaClock @Inject constructor(private val clock: Clock) {

    /** Momento actual, para guardar como `Instant`. */
    fun now(): Instant = clock.instant()

    /** Fecha de hoy en Argentina, para validar y comparar fechas del calendario. */
    fun today(): LocalDate = LocalDate.now(clock.withZone(ZONE))

    companion object {
        val ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    }
}
