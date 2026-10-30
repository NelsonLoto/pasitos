package com.pasitos.shared.api.auth

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class AuthDtosTest {

    @Test
    fun `los momentos viajan en ISO 8601 en UTC`() {
        val tokens = TokenPair("acceso", Instant.parse("2026-10-16T15:15:00Z"), "renovación")

        val json = Json.encodeToString(tokens)

        assertEquals(
            """{"accessToken":"acceso","accessTokenExpiresAt":"2026-10-16T15:15:00Z","refreshToken":"renovación"}""",
            json,
        )
        assertEquals(tokens, Json.decodeFromString<TokenPair>(json))
    }

    @Test
    fun `la respuesta de GET me se lee igual de los dos lados`() {
        val me = MeResponse(
            account = AccountDto(
                id = "2f1c7a1e-6c4b-4f0e-9a52-3b1d2c4e5f60",
                email = "ana@example.com",
                firstName = "Ana",
                lastName = "Pérez",
                emailVerified = false,
                isGuardian = true,
                lastMode = AccountMode.GUARDIAN,
            ),
            pendingTermsVersion = null,
        )

        assertEquals(me, Json.decodeFromString<MeResponse>(Json.encodeToString(me)))
    }
}
