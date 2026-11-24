package com.pasitos.shared.api

import com.pasitos.shared.api.auth.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AuthContractsTest {
    @Test fun `I0-05 contratos viajan completos pero no exponen secretos en toString`() {
        val register = RegisterRequest("Nombre", "Apellido", "synthetic@example.test", "Synthetic123", true)
        assertEquals(register, Json.decodeFromString<RegisterRequest>(Json.encodeToString(register)))
        val account = AccountResponse("account-id", register.email, register.firstName, register.lastName,
            false, true, true, AccountMode.GUARDIAN, "1", false)
        val response = AuthResponse(account, "synthetic-access", "synthetic-refresh", "2026-11-22T12:15:00Z")
        assertEquals(response, Json.decodeFromString<AuthResponse>(Json.encodeToString(response)))
        val logs = listOf(register, LoginRequest(register.email, register.password), RefreshRequest(response.refreshToken), account, response).joinToString()
        for (sensitive in listOf(register.email, register.password, register.firstName, register.lastName,
            response.accessToken, response.refreshToken)) assertFalse(logs.contains(sensitive))
    }
}
