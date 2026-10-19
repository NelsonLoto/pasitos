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
import com.pasitos.backend.common.AccessPolicy
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.guardianship.GuardianshipService
import com.pasitos.backend.invitation.InvitationRepository
import com.pasitos.backend.invitation.InvitationService
import com.pasitos.backend.mail.MailConfig
import com.pasitos.backend.mail.MailService
import com.pasitos.backend.mail.TemplateEngineFactory
import com.pasitos.backend.measurement.MeasurementRepository
import com.pasitos.backend.measurement.MeasurementService
import com.pasitos.backend.sync.ChildSyncEntity
import com.pasitos.backend.sync.GuardianshipSyncEntity
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
    private val guardianshipRepository = GuardianshipRepository()

    // Compartidos entre AuthService (login y sesión) y AccountService (edición de la cuenta propia).
    private val accountRepository = AccountRepository()
    private val sessionRepository = SessionRepository()
    private val consentRepository = ConsentRepository()
    private val emailTokenRepository = EmailTokenRepository()
    private val termsVersion = config.property("terms.version").getString()
    private val declarationVersion = config.property("declaration.version").getString()
    private val accessPolicy = AccessPolicy(guardianshipRepository)

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
    val childService = ChildService(
        children = childRepository,
        guardianships = guardianshipRepository,
        consents = consentRepository,
        accessPolicy = accessPolicy,
        clock = argentinaClock,
        declarationVersion = declarationVersion,
    )
    private val invitationRepository = InvitationRepository()
    val invitationService = InvitationService(
        invitations = invitationRepository,
        children = childRepository,
        guardianships = guardianshipRepository,
        accounts = accountRepository,
        consents = consentRepository,
        accessPolicy = accessPolicy,
        mail = mailService,
        clock = argentinaClock,
        declarationVersion = declarationVersion,
    )
    val guardianshipService = GuardianshipService(
        guardianships = guardianshipRepository,
        children = childRepository,
        accounts = accountRepository,
        accessPolicy = accessPolicy,
        childService = childService,
        mail = mailService,
        clock = argentinaClock,
    )
    val measurementService = MeasurementService(MeasurementRepository(), childRepository, argentinaClock)

    // I0 no registraba ninguna entidad real todavía (ADR-03); I1 va registrando cada dominio a
    // medida que se adapta a sincronización (ADR-28). "children" y "guardianships" son de
    // solo lectura acá: se escriben por REST, no por /sync/push (12 · API §Regla de escritura).
    val syncRegistry = SyncRegistry().apply {
        register(ChildSyncEntity(childRepository))
        register(GuardianshipSyncEntity(guardianshipRepository))
    }
    val syncService = SyncService(syncRegistry, SyncRepository())
}
