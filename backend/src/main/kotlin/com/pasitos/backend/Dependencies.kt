package com.pasitos.backend

import com.pasitos.backend.auth.AccountRepository
import com.pasitos.backend.auth.AuthService
import com.pasitos.backend.auth.JwtConfig
import com.pasitos.backend.auth.JwtService
import com.pasitos.backend.auth.SessionRepository
import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.child.ChildService
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.measurement.MeasurementRepository
import com.pasitos.backend.measurement.MeasurementService
import io.ktor.server.config.ApplicationConfig

/**
 * Crea los servicios y repositorios a mano, sin framework de inyección (11 · Backend §1). Cada
 * dominio nuevo agrega aquí su repositorio y su servicio.
 */
class Dependencies(config: ApplicationConfig, clock: ArgentinaClock = ArgentinaClock()) {

    val jwtService = JwtService(JwtConfig.from(config))

    private val childRepository = ChildRepository()

    val authService = AuthService(AccountRepository(), SessionRepository(), jwtService, clock)
    val childService = ChildService(childRepository, GuardianshipRepository(), clock)
    val measurementService = MeasurementService(MeasurementRepository(), childRepository, clock)
}
