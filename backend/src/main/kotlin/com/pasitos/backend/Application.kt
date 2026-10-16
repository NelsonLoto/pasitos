package com.pasitos.backend

import com.pasitos.backend.auth.authRoutes
import com.pasitos.backend.auth.configureAuthentication
import com.pasitos.backend.child.childRoutes
import com.pasitos.backend.common.configureErrorHandling
import com.pasitos.backend.config.DatabaseFactory
import com.pasitos.backend.measurement.measurementRoutes
import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

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

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    configureErrorHandling()

    val dataSource = DatabaseFactory.init(environment.config)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }

    val dependencies = Dependencies(environment.config)
    configureAuthentication(dependencies.jwtService, dependencies.authService)

    routing {
        get("/health") {
            call.respondText("OK")
        }
        authRoutes(dependencies.authService)
        childRoutes(dependencies.childService)
        measurementRoutes(dependencies.measurementService)
    }
}
