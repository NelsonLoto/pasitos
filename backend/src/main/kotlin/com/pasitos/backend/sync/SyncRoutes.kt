package com.pasitos.backend.sync

import com.pasitos.backend.auth.caller
import com.pasitos.shared.api.sync.SyncPushRequest
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Application.configureSync(service: SyncService) {
    routing {
        route("/api/v1/sync") {
            authenticate("session") {
                rateLimit(RateLimitName("account")) {
                    get("/changes") {
                        val rawCursor = call.request.queryParameters["cursor"]
                        val cursor = rawCursor?.toLongOrNull() ?: if (rawCursor == null) 0 else throw invalidSync("El cursor de sincronización no es válido")
                        val patients = call.request.queryParameters["patients"]?.takeIf { it.isNotEmpty() }?.split(',').orEmpty()
                        if (patients.size > 50 || patients.any { syncUuid(it) == null }) throw invalidSync("La lista de pacientes no es válida")
                        val caller = call.caller()
                        call.respond(withContext(Dispatchers.IO) { service.changes(caller, cursor) })
                    }
                    get("/snapshot/{childId}") {
                        val childId = syncUuid(call.parameters["childId"].orEmpty()) ?: throw invalidSync("El identificador no es válido")
                        val caller = call.caller()
                        call.respond(withContext(Dispatchers.IO) { service.snapshot(caller, childId) })
                    }
                    post("/push") {
                        val request = call.receive<SyncPushRequest>()
                        val caller = call.caller()
                        call.respond(withContext(Dispatchers.IO) { service.push(caller, request) })
                    }
                }
            }
        }
    }
}
