package com.pasitos.shared.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class ErrorResponseTest {

    @Test
    fun `el código viaja en snake_case`() {
        val json = Json.encodeToString(ErrorResponse(ErrorCode.CHILD_NOT_FOUND, "No se encontró ese hijo/a"))

        assertEquals("""{"code":"child_not_found","message":"No se encontró ese hijo/a"}""", json)
    }

    @Test
    fun `una regla de negocio incluye su ID`() {
        val error = ErrorResponse(ErrorCode.BUSINESS_RULE, "Ya cargaste una medición en esa fecha", rule = "RN-CRE-12")

        assertEquals(error, Json.decodeFromString<ErrorResponse>(Json.encodeToString(error)))
    }

    @Test
    fun `un código desconocido se lee como UNKNOWN en vez de fallar`() {
        val error = Json.decodeFromString<ErrorResponse>("""{"code":"code_from_the_future","message":"Algo nuevo"}""")

        assertEquals(ErrorCode.UNKNOWN, error.code)
        assertEquals("Algo nuevo", error.message)
    }
}
