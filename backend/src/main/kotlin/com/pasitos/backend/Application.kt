package com.pasitos.backend

import com.pasitos.backend.db.DatabaseFactory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
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

    DatabaseFactory.init(environment.config)

    routing {
        get("/health") {
            call.respondText("OK")
        }
    }
}
