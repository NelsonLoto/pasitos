package com.pasitos.backend.sync

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import com.pasitos.shared.api.sync.SyncPushRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** Endpoints bajo `/api/v1/sync` (14 · Sincronización). Nuevos desde el inicio (ADR-04). */
fun Route.syncRoutes(syncService: SyncService) {
    authenticate(AUTH_JWT) {
        get("/sync/changes") {
            val cursor = call.request.queryParameters["cursor"]?.toLongOrNull() ?: 0L
            val patients = call.request.queryParameters["patients"]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.map { it.toUuidOrNull() ?: throw InvalidInputException("Id de paciente inválido") }
                ?.toSet()
            call.respond(HttpStatusCode.OK, syncService.changes(call.caller(), cursor, patients))
        }

        get("/sync/snapshot/{childId}") {
            val childId = call.parameters["childId"]?.toUuidOrNull()
                ?: throw InvalidInputException("Id de hijo inválido")
            call.respond(HttpStatusCode.OK, syncService.snapshot(call.caller(), childId))
        }

        post("/sync/push") {
            val request = call.receive<SyncPushRequest>()
            call.respond(HttpStatusCode.OK, syncService.push(call.caller(), request.mutations))
        }
    }
}
