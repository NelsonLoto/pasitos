package com.pasitos.backend.auth

import com.pasitos.backend.support.TestClock
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.TestGoogle
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.GoogleAuthRequest
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.RefreshRequest
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
import java.time.Duration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals

/** `POST /api/v1/auth/google` (I1-B2): RF-CTA-03 y RN-CTA-04, con tokens firmados por [TestGoogle]. */
class GoogleAuthRoutesTest {

    @Test
    fun `RF-CTA-03 CA1 el primer ingreso sin aceptar los términos responde 409 y no guarda nada`() = apiTest { client ->
        val response = client.google(GoogleAuthRequest(TestGoogle.idToken()))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(ErrorCode.TERMS_REQUIRED, response.body<ErrorResponse>().code)
        assertEquals(0L, count("accounts"))
        assertEquals(0L, count("sessions"))
    }

    @Test
    fun `RF-CTA-03 CA1 al aceptar los términos crea la cuenta de tutor verificada, sin contraseña, y abre sesión`() = apiTest { client ->
        val token = TestGoogle.idToken(subject = "google-ana", email = "ana@gmail.com")

        val response = client.google(GoogleAuthRequest(token, acceptTerms = true, firstName = " Ana ", lastName = "Pérez"))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertEquals("ana@gmail.com", body.account.email)
        assertEquals("Ana", body.account.firstName)
        assertEquals("Pérez", body.account.lastName)
        assertEquals(true, body.account.emailVerified)
        assertEquals(true, body.account.isGuardian)
        assertEquals(false, body.account.hasPassword)
        assertEquals(listOf("google-ana"), TestDatabase.column<String>("SELECT google_subject FROM accounts"))
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM accounts WHERE password_hash IS NULL").single())
        assertEquals(
            listOf("TERMS ${TestDatabase.TERMS_VERSION}"),
            TestDatabase.column<String>("SELECT type || ' ' || version FROM consents"),
        )
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/me") { bearerAuth(body.tokens.accessToken) }.status)

        // Los ingresos siguientes ya no piden términos ni nombres.
        val again = client.google(GoogleAuthRequest(token))
        assertEquals(HttpStatusCode.OK, again.status)
        assertEquals(body.account.id, again.body<AuthResponse>().account.id)
        assertEquals(1L, count("accounts"))
    }

    @Test
    fun `RF-CTA-03 CA1 el primer ingreso exige nombre y apellido`() = apiTest { client ->
        val invalid = listOf(
            GoogleAuthRequest(TestGoogle.idToken(), acceptTerms = true, firstName = "Ana", lastName = " "),
            GoogleAuthRequest(TestGoogle.idToken(), acceptTerms = true, firstName = null, lastName = "Pérez"),
        )

        invalid.forEach { request ->
            val response = client.google(request)
            assertEquals(HttpStatusCode.BadRequest, response.status, "Pedido: $request")
            assertEquals(ErrorCode.INVALID_INPUT, response.body<ErrorResponse>().code)
        }
        assertEquals(0L, count("accounts"))
    }

    @Test
    fun `RF-CTA-03 CA2 y RN-CTA-04 con el email de una cuenta verificada ingresa a esa cuenta y asocia Google`() = apiTest { client ->
        val registered = client.register(email = "ana@gmail.com", password = PASSWORD)
        markAllEmailsVerified()

        val response = client.google(GoogleAuthRequest(TestGoogle.idToken(subject = "google-ana", email = "ana@gmail.com")))

        assertEquals(HttpStatusCode.OK, response.status)
        val account = response.body<AuthResponse>().account
        assertEquals(registered.account.id, account.id)
        assertEquals("Ana", account.firstName)
        assertEquals(true, account.hasPassword)
        assertEquals(listOf("google-ana"), TestDatabase.column<String>("SELECT google_subject FROM accounts"))
        // La contraseña sigue funcionando: la cuenta ahora tiene los dos métodos.
        assertEquals(HttpStatusCode.OK, client.login("ana@gmail.com", PASSWORD).status)
    }

    @Test
    fun `RN-CTA-04 con el email de una cuenta sin verificar la verifica, borra la contraseña y cierra sus sesiones`() = apiTest { client ->
        // Alguien registró el email de Ana antes que ella, con su propia contraseña, y quedó con sesión.
        val earlier = client.register(email = "ana@gmail.com", password = PASSWORD)

        val response = client.google(GoogleAuthRequest(TestGoogle.idToken(email = "ana@gmail.com")))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertEquals(earlier.account.id, body.account.id)
        assertEquals(true, body.account.emailVerified)
        assertEquals(false, body.account.hasPassword)
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM accounts WHERE password_hash IS NULL").single())

        val oldLogin = client.login("ana@gmail.com", PASSWORD)
        assertEquals(HttpStatusCode.Unauthorized, oldLogin.status)
        assertEquals(ErrorCode.INVALID_CREDENTIALS, oldLogin.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/me") { bearerAuth(earlier.tokens.accessToken) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(earlier.tokens.refreshToken).status)
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/me") { bearerAuth(body.tokens.accessToken) }.status)
    }

    @Test
    fun `la cuenta se encuentra por su cuenta de Google aunque el email de Google cambie`() = apiTest { client ->
        val created = client.createWithGoogle(subject = "google-ana", email = "ana@gmail.com")

        val response = client.google(GoogleAuthRequest(TestGoogle.idToken(subject = "google-ana", email = "ana.perez@gmail.com")))

        assertEquals(HttpStatusCode.OK, response.status)
        val account = response.body<AuthResponse>().account
        assertEquals(created.account.id, account.id)
        assertEquals("ana@gmail.com", account.email)
    }

    @Test
    fun `un email que ya usa otra cuenta de Google responde 409 sin asociarlo`() = apiTest { client ->
        client.createWithGoogle(subject = "google-anterior", email = "secretaria@escuela.edu.ar")

        val response = client.google(GoogleAuthRequest(TestGoogle.idToken(subject = "google-nueva", email = "secretaria@escuela.edu.ar")))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(ErrorCode.EMAIL_ALREADY_IN_USE, response.body<ErrorResponse>().code)
        assertEquals(listOf("google-anterior"), TestDatabase.column<String>("SELECT google_subject FROM accounts"))
    }

    @Test
    fun `RN-CTA-05 con el inicio con contraseña bloqueado, el inicio con Google sigue disponible`() = apiTest { client ->
        client.register(email = "ana@gmail.com", password = PASSWORD)
        markAllEmailsVerified()
        repeat(AuthService.MAX_FAILED_LOGINS) { client.login("ana@gmail.com", "incorrecta1") }
        assertEquals(HttpStatusCode.Locked, client.login("ana@gmail.com", PASSWORD).status)

        val response = client.google(GoogleAuthRequest(TestGoogle.idToken(email = "ana@gmail.com")))

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `un token vencido responde 401 invalid_google_token`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            val token = TestGoogle.idToken(issuedAt = clock.instant())
            clock.advance(Duration.ofHours(2))

            val response = client.google(GoogleAuthRequest(token, acceptTerms = true, firstName = "Ana", lastName = "Pérez"))

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals(ErrorCode.INVALID_GOOGLE_TOKEN, response.body<ErrorResponse>().code)
            assertEquals(0L, count("accounts"))
        }
    }

    @Test
    fun `si Google no responde, 503 google_unavailable`() = apiTest(googleKeys = TestGoogle.unavailableKeys) { client ->
        val response = client.google(GoogleAuthRequest(TestGoogle.idToken()))

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals(ErrorCode.GOOGLE_UNAVAILABLE, response.body<ErrorResponse>().code)
    }

    @Test
    fun `dos primeros ingresos simultáneos con la misma cuenta de Google crean una sola cuenta`() = apiTest { client ->
        val request = GoogleAuthRequest(TestGoogle.idToken(), acceptTerms = true, firstName = "Ana", lastName = "Pérez")

        val responses = coroutineScope { List(2) { async { client.google(request) } }.awaitAll() }

        responses.forEach { assertEquals(HttpStatusCode.OK, it.status) }
        assertEquals(1, responses.map { it.body<AuthResponse>().account.id }.toSet().size)
        assertEquals(1L, count("accounts"))
    }

    private suspend fun HttpClient.createWithGoogle(subject: String, email: String): AuthResponse {
        val response = google(
            GoogleAuthRequest(TestGoogle.idToken(subject = subject, email = email), acceptTerms = true, firstName = "Ana", lastName = "Pérez"),
        )
        assertEquals(HttpStatusCode.OK, response.status)
        return response.body()
    }

    private suspend fun HttpClient.google(request: GoogleAuthRequest): HttpResponse = postJson("/api/v1/auth/google", request)

    private suspend fun HttpClient.login(email: String, password: String): HttpResponse =
        postJson("/api/v1/auth/login", LoginRequest(email, password))

    private suspend fun HttpClient.refresh(refreshToken: String): HttpResponse =
        postJson("/api/v1/auth/refresh", RefreshRequest(refreshToken))

    private suspend inline fun <reified T> HttpClient.postJson(path: String, body: T): HttpResponse = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun count(table: String): Long = TestDatabase.column<Long>("SELECT count(*) FROM $table").single()

    private fun markAllEmailsVerified() {
        TestDatabase.connect().use { it.createStatement().executeUpdate("UPDATE accounts SET email_verified_at = now()") }
    }

    private companion object {
        const val PASSWORD = "secreta123"
    }
}
