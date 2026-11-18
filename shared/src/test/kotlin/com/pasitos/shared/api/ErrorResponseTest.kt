package com.pasitos.shared.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorResponseTest {
    @Test fun `I0-02 ErrorCode viaja en snake_case`() {
        for (code in ErrorCode.entries) {
            assertTrue(Regex("[a-z]+(_[a-z]+)*").matches(code.wireName), code.name)
            assertEquals(code.name.lowercase(), code.wireName)
        }
        assertEquals(
            """{"code":"not_found","message":"No se encontró ese recurso"}""",
            Json.encodeToString(ErrorResponse(ErrorCode.NOT_FOUND, "No se encontró ese recurso")),
        )
    }

    @Test fun `I0-02 ErrorResponse se lee con sus campos`() {
        val json = """{"code":"invalid_input","message":"Revisá los datos","fields":{"email":"Falta el email"}}"""
        assertEquals(
            ErrorResponse(ErrorCode.INVALID_INPUT, "Revisá los datos", mapOf("email" to "Falta el email")),
            Json.decodeFromString<ErrorResponse>(json),
        )
    }

    @Test fun `I0-02 un codigo desconocido se lee como UNKNOWN`() {
        val json = """{"code":"child_not_found","message":"No se encontró ese perfil"}"""
        assertEquals(ErrorCode.UNKNOWN, Json.decodeFromString<ErrorResponse>(json).code)
    }
}
