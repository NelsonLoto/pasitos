package com.pasitos.backend.auth

import com.pasitos.backend.common.ErrorResponse
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthRoutesTest {

    @Test
    fun `el registro crea una cuenta de tutor y abre una sesión`() = apiTest { client ->
        val response = client.register(email = "  Ana@Example.com ", name = "Ana Pérez")

        assertEquals("ana@example.com", response.email)
        assertEquals("Ana Pérez", response.name)
        assertTrue(response.token.isNotBlank())
        assertEquals(listOf(true), TestDatabase.column<Boolean>("SELECT is_guardian FROM accounts WHERE email = 'ana@example.com'"))
        val hashes = TestDatabase.column<String>("SELECT password_hash FROM accounts")
        assertNotEquals("secreta123", hashes.single())
        assertEquals(1, sessionCount())
    }

    @Test
    fun `el registro con un email en uso responde 409`() = apiTest { client ->
        client.register(email = "ana@example.com")

        val response = client.postJson("/auth/register", RegisterRequest("Otra", "ANA@example.com", "secreta123"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("email_already_in_use", response.body<ErrorResponse>().error)
    }

    @Test
    fun `el registro con datos incompletos responde 400`() = apiTest { client ->
        val response = client.postJson("/auth/register", RegisterRequest("Ana", "ana@example.com", "corta"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_input", response.body<ErrorResponse>().error)
    }

    @Test
    fun `el login con la contraseña correcta abre otra sesión`() = apiTest { client ->
        val registered = client.register(email = "ana@example.com", password = "secreta123")

        val response = client.postJson("/auth/login", LoginRequest("ANA@example.com", "secreta123"))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertEquals(registered.userId, body.userId)
        assertEquals("Ana Pérez", body.name)
        assertEquals(2, sessionCount())
    }

    @Test
    fun `el login con contraseña incorrecta o email desconocido responde 401`() = apiTest { client ->
        client.register(email = "ana@example.com", password = "secreta123")

        val wrongPassword = client.postJson("/auth/login", LoginRequest("ana@example.com", "otraClave1"))
        val unknownEmail = client.postJson("/auth/login", LoginRequest("nadie@example.com", "secreta123"))

        assertEquals(HttpStatusCode.Unauthorized, wrongPassword.status)
        assertEquals("invalid_credentials", wrongPassword.body<ErrorResponse>().error)
        assertEquals(HttpStatusCode.Unauthorized, unknownEmail.status)
    }

    @Test
    fun `el logout revoca la sesión y el token deja de servir`() = apiTest { client ->
        val token = client.register().token

        val logout = client.post("/auth/logout") { bearerAuth(token) }
        val afterLogout = client.get("/children") { bearerAuth(token) }

        assertEquals(HttpStatusCode.NoContent, logout.status)
        assertEquals(0, sessionCount())
        assertEquals(HttpStatusCode.Unauthorized, afterLogout.status)
    }

    @Test
    fun `una sesión vencida no sirve aunque el token siga vigente`() = apiTest { client ->
        val token = client.register().token
        TestDatabase.connect().use { it.createStatement().execute("UPDATE sessions SET expires_at = now() - interval '1 minute'") }

        val response = client.get("/children") { bearerAuth(token) }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `las rutas protegidas sin token o con un token inválido responden 401`() = apiTest { client ->
        val withoutToken = client.get("/children")
        val invalidToken = client.get("/children") { bearerAuth("no-es-un-jwt") }

        assertEquals(HttpStatusCode.Unauthorized, withoutToken.status)
        assertEquals("unauthorized", withoutToken.body<ErrorResponse>().error)
        assertEquals(HttpStatusCode.Unauthorized, invalidToken.status)
    }

    private fun sessionCount() = TestDatabase.column<Long>("SELECT count(*) FROM sessions").single().toInt()

    private suspend inline fun <reified T> HttpClient.postJson(path: String, body: T): HttpResponse = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}
