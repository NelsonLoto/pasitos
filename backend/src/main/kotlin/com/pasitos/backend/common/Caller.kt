package com.pasitos.backend.common

import com.pasitos.backend.auth.JwtService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

/** Cuenta autenticada que hace el pedido y la sesión desde la que lo hace, según el token de acceso. */
data class Caller(val accountId: UUID, val sessionId: UUID)

/** Solo dentro de `authenticate { }`: la validación del token ya garantizó que ambos IDs existen. */
fun ApplicationCall.caller(): Caller {
    val payload = checkNotNull(principal<JWTPrincipal>()) { "caller() fuera de una ruta autenticada" }.payload
    return Caller(
        accountId = UUID.fromString(payload.subject),
        sessionId = UUID.fromString(payload.getClaim(JwtService.SESSION_CLAIM).asString()),
    )
}
