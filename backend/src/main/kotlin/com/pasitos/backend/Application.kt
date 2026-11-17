package com.pasitos.backend

import com.pasitos.backend.common.AppConfig
import com.pasitos.backend.common.DatabaseFactory
import com.pasitos.backend.common.configureErrors
import com.pasitos.backend.common.configureRequestLogging
import com.pasitos.backend.health.healthRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, host = config.host, port = config.port) { module(config) }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.fromEnvironment()) {
    val database = DatabaseFactory(config.database)
    monitor.subscribe(ApplicationStopped) { database.close() }
    configureHttp(database::isHealthy)
}

internal fun Application.configureHttp(isDatabaseHealthy: () -> Boolean) {
    install(ContentNegotiation) { json() }
    configureRequestLogging()
    configureErrors()
    routing { healthRoutes(isDatabaseHealthy) }
}
