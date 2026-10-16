package com.pasitos.backend.measurement

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class MeasurementRoutesTest {

    @Test
    fun `la carga de una medición devuelve lo guardado, sin percentiles`() = apiTest { client ->
        val token = client.register().token
        val child = client.createChild(token, birthDate = "2026-06-15")

        val response = client.postMeasurement(token, child.id, CreateMeasurementRequest("2026-10-01", 6.4, 62.55, 40.8))

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<MeasurementResponse>()
        UUID.fromString(body.id)
        assertEquals(
            MeasurementResponse(body.id, "2026-10-01", 6.4, 62.6, 40.8, null, null, null, outOfRange = false),
            body,
        )
        val stored = TestDatabase.column<String>(
            "SELECT author_role || ' ' || length_position FROM measurements WHERE id = ?",
            UUID.fromString(body.id),
        )
        assertEquals(listOf("GUARDIAN LYING"), stored)
    }

    @Test
    fun `RN-CRE-03 la talla de un niño de 24 meses o más se guarda como medida de pie`() = apiTest { client ->
        val token = client.register().token
        val child = client.createChild(token, birthDate = "2023-01-10")

        val body = client.postMeasurement(token, child.id, CreateMeasurementRequest("2026-10-01", lengthCm = 98.0))
            .body<MeasurementResponse>()

        assertEquals(
            listOf("STANDING"),
            TestDatabase.column<String>("SELECT length_position FROM measurements WHERE id = ?", UUID.fromString(body.id)),
        )
    }

    @Test
    fun `el historial devuelve las mediciones ordenadas por fecha`() = apiTest { client ->
        val token = client.register().token
        val child = client.createChild(token)
        client.postMeasurement(token, child.id, CreateMeasurementRequest("2026-09-15", weightKg = 6.1))
        client.postMeasurement(token, child.id, CreateMeasurementRequest("2026-07-15", weightKg = 4.2))
        client.postMeasurement(token, child.id, CreateMeasurementRequest("2026-08-15", weightKg = 5.3))

        val response = client.get("/children/${child.id}/measurements") { bearerAuth(token) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            listOf("2026-07-15", "2026-08-15", "2026-09-15"),
            response.body<List<MeasurementResponse>>().map { it.measuredOn },
        )
    }

    @Test
    fun `RN-CRE-12 una segunda medición del mismo autor en la misma fecha responde 422`() = apiTest { client ->
        val token = client.register().token
        val child = client.createChild(token)
        client.postMeasurement(token, child.id, CreateMeasurementRequest("2026-10-01", weightKg = 6.4))

        val response = client.postMeasurement(token, child.id, CreateMeasurementRequest("2026-10-01", weightKg = 6.5))

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val error = response.body<ErrorResponse>()
        assertEquals(ErrorCode.BUSINESS_RULE, error.code)
        assertEquals("RN-CRE-12", error.rule)
    }

    @Test
    fun `el hijo de otro tutor responde 404 como si no existiera`() = apiTest { client ->
        val ana = client.register(email = "ana@example.com").token
        val beto = client.register(email = "beto@example.com").token
        val child = client.createChild(ana)

        val post = client.postMeasurement(beto, child.id, CreateMeasurementRequest("2026-10-01", weightKg = 6.4))
        val get = client.get("/children/${child.id}/measurements") { bearerAuth(beto) }
        val unknown = client.get("/children/${UUID.randomUUID()}/measurements") { bearerAuth(ana) }

        listOf(post, get, unknown).forEach { response ->
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(ErrorCode.CHILD_NOT_FOUND, response.body<ErrorResponse>().code)
        }
    }

    @Test
    fun `sin valores, con fecha futura, con valores fuera de rango o con un id inválido responde 400`() = apiTest { client ->
        val token = client.register().token
        val child = client.createChild(token)
        val tomorrow = ArgentinaClock().today().plusDays(1).toString()

        val invalid = listOf(
            child.id to CreateMeasurementRequest("2026-10-01"),
            child.id to CreateMeasurementRequest(tomorrow, weightKg = 6.4),
            child.id to CreateMeasurementRequest("2026-10-01", weightKg = 0.0),
            child.id to CreateMeasurementRequest("2026-10-01", lengthCm = 100_000.0),
            "no-es-un-uuid" to CreateMeasurementRequest("2026-10-01", weightKg = 6.4),
        )

        invalid.forEach { (childId, request) ->
            val response = client.postMeasurement(token, childId, request)
            assertEquals(HttpStatusCode.BadRequest, response.status, "Pedido: $request")
            assertEquals(ErrorCode.INVALID_INPUT, response.body<ErrorResponse>().code)
        }
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM measurements").single())
    }

    @Test
    fun `RN-CRE-03 hasta los 24 meses se asume acostado y desde entonces de pie`() {
        val birth = LocalDate.parse("2024-10-16")

        assertEquals(LengthPosition.LYING, defaultLengthPosition(birth, LocalDate.parse("2026-10-15")))
        assertEquals(LengthPosition.STANDING, defaultLengthPosition(birth, LocalDate.parse("2026-10-16")))
    }

    private suspend fun HttpClient.postMeasurement(
        token: String,
        childId: String,
        request: CreateMeasurementRequest,
    ): HttpResponse = post("/children/$childId/measurements") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(request)
    }
}
