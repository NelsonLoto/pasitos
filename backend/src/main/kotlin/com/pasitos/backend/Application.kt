package com.pasitos.backend

import com.auth0.jwt.interfaces.RSAKeyProvider
import com.pasitos.backend.account.accountRoutes
import com.pasitos.backend.auth.authRoutes
import com.pasitos.backend.auth.configureAuthentication
import com.pasitos.backend.auth.google.GooglePublicKeys
import com.pasitos.backend.child.childRoutes
import com.pasitos.backend.common.configureErrorHandling
import com.pasitos.backend.common.configureRequestLogging
import com.pasitos.backend.config.DatabaseFactory
import com.pasitos.backend.guardianship.guardianshipRoutes
import com.pasitos.backend.invitation.invitationRoutes
import com.pasitos.backend.mail.TemplateEngineFactory
import com.pasitos.backend.sync.syncRoutes
import com.pasitos.backend.vaccination.vaccinationRoutes
import com.pasitos.backend.web.webRoutes
import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.Thymeleaf
import java.time.Clock

fun main(args: Array<String>) {
    // En desarrollo local carga backend/.env (no versionado) a system properties,
    // para que application.conf las resuelva con ${?DB_HOST}. Si el archivo no
    // existe (Docker/CI, donde las variables ya están en el entorno), no hace nada.
    dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }
    EngineMain.main(args)
}

/**
 * [clock] es el reloj de todo el backend. Las pruebas pasan uno propio para adelantar la hora
 * (vencimiento de tokens y sesiones, bloqueo por intentos). [googleKeys] son las claves con las
 * que se verifican los ID tokens de Google; las pruebas pasan una clave propia.
 */
fun Application.module(
    clock: Clock = Clock.systemUTC(),
    googleKeys: RSAKeyProvider = GooglePublicKeys.remote(),
) {
    configureRequestLogging()
    install(ContentNegotiation) {
        json()
    }
    configureErrorHandling()

    val dataSource = DatabaseFactory.init(environment.config)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }

    val dependencies = Dependencies(environment.config, clock, googleKeys)
    configureAuthentication(dependencies.jwtService, dependencies.authService)
    install(Thymeleaf) {
        TemplateEngineFactory.configure(this)
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }
        webRoutes(dependencies.authService, dependencies.accountService)

        // API nueva (ADR-04). Los endpoints de arriba se mudan acá a medida que la app los adopta.
        route("/api/v1") {
            authRoutes(dependencies.authService)
            accountRoutes(dependencies.accountService)
            childRoutes(dependencies.childService)
            guardianshipRoutes(dependencies.guardianshipService, dependencies.invitationService)
            invitationRoutes(dependencies.invitationService)
            vaccinationRoutes(dependencies.vaccinationService)
            syncRoutes(dependencies.syncService)
        }
    }
}
