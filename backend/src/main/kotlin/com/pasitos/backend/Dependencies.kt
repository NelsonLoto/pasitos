package com.pasitos.backend

import com.pasitos.backend.account.AccountService
import com.pasitos.backend.auth.AccountRepository
import com.pasitos.backend.auth.AuthService
import com.pasitos.backend.auth.ConsentRepository
import com.pasitos.backend.auth.EmailTokenRepository
import com.pasitos.backend.auth.JwtConfig
import com.pasitos.backend.auth.JwtService
import com.pasitos.backend.auth.SessionRepository
import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.child.ChildService
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.mail.MailConfig
import com.pasitos.backend.mail.MailService
import com.pasitos.backend.mail.TemplateEngineFactory
import com.pasitos.backend.measurement.MeasurementRepository
import com.pasitos.backend.measurement.MeasurementService
import com.pasitos.backend.sync.SyncRegistry
import com.pasitos.backend.sync.SyncRepository
import com.pasitos.backend.sync.SyncService
import io.ktor.server.config.ApplicationConfig
import java.time.Clock

/**
 * Crea los servicios y repositorios a mano, sin framework de inyección (11 · Backend §1). Cada
 * dominio nuevo agrega aquí su repositorio y su servicio. Todo usa el mismo [clock], que las
 * pruebas reemplazan para adelantar la hora.
 */
class Dependencies(config: ApplicationConfig, clock: Clock) {

    private val argentinaClock = ArgentinaClock(clock)

    val jwtService = JwtService(JwtConfig.from(config), clock)

    // Las páginas públicas (`/web/...`) usan su propio motor Thymeleaf, instalado como plugin en
    // Application.kt con la misma configuración (ADR-09, ADR-27).
    private val mailService = MailService(MailConfig.from(config), TemplateEngineFactory.create())

    private val childRepository = ChildRepository()

    // Compartidos entre AuthService (login y sesión) y AccountService (edición de la cuenta propia).
    private val accountRepository = AccountRepository()
    private val sessionRepository = SessionRepository()
    private val consentRepository = ConsentRepository()
    private val emailTokenRepository = EmailTokenRepository()
    private val termsVersion = config.property("terms.version").getString()

    val authService = AuthService(
        accounts = accountRepository,
        sessions = sessionRepository,
        consents = consentRepository,
        emailTokens = emailTokenRepository,
        jwt = jwtService,
        mail = mailService,
        clock = argentinaClock,
        termsVersion = termsVersion,
    )
    val accountService = AccountService(
        accounts = accountRepository,
        sessions = sessionRepository,
        consents = consentRepository,
        emailTokens = emailTokenRepository,
        mail = mailService,
        clock = argentinaClock,
        termsVersion = termsVersion,
    )
    val childService = ChildService(childRepository, GuardianshipRepository(), argentinaClock)
    val measurementService = MeasurementService(MeasurementRepository(), childRepository, argentinaClock)

    // I0 no registra ninguna entidad real todavía (ADR-03); I1 registra "children",
    // "measurements", etc. a medida que cada dominio se adapta a sincronización (ADR-28).
    val syncRegistry = SyncRegistry()
    val syncService = SyncService(syncRegistry, SyncRepository())
}
