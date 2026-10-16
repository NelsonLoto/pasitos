package com.pasitos.backend.child

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.ErrorResponse
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ChildRoutesTest {

    @Test
    fun `RF-HIJ-01 quien da de alta al hijo queda como su tutor principal`() = apiTest { client ->
        val token = client.register().token

        val child = client.createChild(token, name = "Sofi", birthDate = "2026-06-15", sex = "F")

        assertEquals(ChildResponse(child.id, "Sofi", "2026-06-15", "F"), child)
        val childId = UUID.fromString(child.id)
        assertEquals(listOf("PRINCIPAL"), TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = ?", childId))
        assertEquals(listOf("FEMALE"), TestDatabase.column<String>("SELECT sex FROM children WHERE id = ?", childId))
        // RN-HIJ-03: la app actual no pide semanas de gestación, quedan como "No lo sé".
        assertEquals(listOf(true), TestDatabase.column<Boolean>("SELECT gestational_weeks_unknown FROM children WHERE id = ?", childId))
    }

    @Test
    fun `cada tutor ve solo sus hijos, en orden de alta`() = apiTest { client ->
        val ana = client.register(email = "ana@example.com").token
        val beto = client.register(email = "beto@example.com").token
        client.createChild(ana, name = "Sofi")
        client.createChild(beto, name = "Tomi", sex = "M")
        client.createChild(ana, name = "Juli", sex = "M")

        val response = client.get("/children") { bearerAuth(ana) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("Sofi", "Juli"), response.body<List<ChildResponse>>().map { it.name })
    }

    @Test
    fun `el alta con nombre vacío, fecha futura o inválida, o sexo desconocido responde 400`() = apiTest { client ->
        val token = client.register().token
        val tomorrow = ArgentinaClock().today().plusDays(1).toString()

        val invalid = listOf(
            CreateChildRequest(name = " ", birthDate = "2026-06-15", sex = "F"),
            CreateChildRequest(name = "Sofi", birthDate = tomorrow, sex = "F"),
            CreateChildRequest(name = "Sofi", birthDate = "15/06/2026", sex = "F"),
            CreateChildRequest(name = "Sofi", birthDate = "2026-06-15", sex = "X"),
        )

        invalid.forEach { request ->
            val response = client.postChild(token, request)
            assertEquals(HttpStatusCode.BadRequest, response.status, "Pedido: $request")
            assertEquals("invalid_input", response.body<ErrorResponse>().error)
        }
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM children").single())
    }

    @Test
    fun `un pedido con JSON mal formado responde 400`() = apiTest { client ->
        val token = client.register().token

        val response = client.post("/children") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"nombre": "Sofi"""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_input", response.body<ErrorResponse>().error)
    }

    private suspend fun HttpClient.postChild(token: String, request: CreateChildRequest): HttpResponse = post("/children") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(request)
    }
}
