package com.pasitos.backend.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String, val database: String)

fun Route.healthRoutes(isDatabaseHealthy: () -> Boolean) {
    get("/health") {
        val healthy = withContext(Dispatchers.IO) { isDatabaseHealthy() }
        call.respond(if (healthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
            HealthResponse(if (healthy) "ok" else "unavailable", if (healthy) "connected" else "unavailable"))
    }
}
