package com.pasitos.app.core.network

import com.pasitos.shared.api.auth.TokenPair

/** Tokens de la sesión para el cliente de la API ([createHttpClient]). Lo implementa `SessionRepository`. */
interface SessionTokens {

    /** Los tokens vigentes, o nulo sin sesión. Si la sesión guardada todavía se está restaurando, la espera. */
    suspend fun current(): TokenPair?

    /**
     * Renueva los tokens porque el servidor rechazó [rejectedAccessToken]. Si ya no es el vigente,
     * otro pedido los renovó: devuelve los actuales sin llamar a [renew], porque usar dos veces el
     * mismo token de renovación hace que el servidor revoque la sesión. Devuelve nulo si no se
     * pudo renovar; si el servidor rechazó el token de renovación, además la sesión vence.
     */
    suspend fun refresh(rejectedAccessToken: String?, renew: suspend (refreshToken: String) -> RefreshResult): TokenPair?
}

/** Respuesta de `POST /api/v1/auth/refresh`. */
sealed interface RefreshResult {
    data class Renewed(val tokens: TokenPair) : RefreshResult

    /** 401: la sesión venció o fue revocada. */
    data object Rejected : RefreshResult

    /** Cualquier otra respuesta: se vuelve a intentar en el próximo pedido. */
    data object Unavailable : RefreshResult
}
