package com.pasitos.backend.account

import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.TestMailServer
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.account.ChangePasswordRequest
import com.pasitos.shared.api.account.EmailChangeRequest
import com.pasitos.shared.api.account.PatchMeRequest
import com.pasitos.shared.api.auth.AccountDto
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.MeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Rutas de edición de la cuenta propia (I1-B3: RF-CTA-09 CA1, RF-CTA-11, RN-CTA-02). */
class AccountRoutesTest {

    @Test
    fun `RF-CTA-11 CA1 PATCH me actualiza nombre y apellido`() = apiTest { client ->
        val session = client.register()

        val response = client.patchJson(session.tokens.accessToken, PatchMeRequest("Ana María", "Gómez"))

        assertEquals(HttpStatusCode.OK, response.status)
        val account = response.body<AccountDto>()
        assertEquals("Ana María", account.firstName)
        assertEquals("Gómez", account.lastName)
        assertEquals(session.account.email, account.email)
    }

    @Test
    fun `PATCH me exige nombre y apellido`() = apiTest { client ->
        val session = client.register()

        val response = client.patchJson(session.tokens.accessToken, PatchMeRequest("  ", "Gómez"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ErrorCode.INVALID_INPUT, response.body<ErrorResponse>().code)
    }

    @Test
    fun `RF-CTA-11 CA2 cambiar la contraseña pide la actual y cierra las demás sesiones, no la actual`() = apiTest { client ->
        val first = client.register()
        // El login exige email verificado (RF-CTA-02): se verifica para poder abrir una segunda sesión.
        client.verifyEmail("ana@example.com")
        val second = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", "secreta123")).body<AuthResponse>()

        val wrongCurrent = client.post("/api/v1/me/password") {
            bearerAuth(first.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(currentPassword = "otraClave99", newPassword = "nuevaClave99"))
        }
        val weakNew = client.post("/api/v1/me/password") {
            bearerAuth(first.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(currentPassword = "secreta123", newPassword = "corta1"))
        }
        val ok = client.post("/api/v1/me/password") {
            bearerAuth(first.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(currentPassword = "secreta123", newPassword = "nuevaClave99"))
        }

        assertEquals(HttpStatusCode.Unauthorized, wrongCurrent.status)
        assertEquals(ErrorCode.INVALID_CREDENTIALS, wrongCurrent.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.BadRequest, weakNew.status)
        assertEquals(HttpStatusCode.NoContent, ok.status)
        // La sesión que pidió el cambio sigue viva.
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/me") { bearerAuth(first.tokens.accessToken) }.status)
        // La otra sesión (segundo login) se cerró.
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/me") { bearerAuth(second.tokens.accessToken) }.status)
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM sessions").single())
    }

    @Test
    fun `RF-CTA-11 CA1 pedir cambio de email pide la contraseña y manda la verificación al email nuevo, sin cambiarlo todavía`() = apiTest { client ->
        val session = client.register()
        client.register(email = "en-uso@example.com")
        TestMailServer.reset()

        val wrongPassword = client.requestEmailChange(session.tokens.accessToken, "nueva@example.com", "otraClave99")
        val inUse = client.requestEmailChange(session.tokens.accessToken, "en-uso@example.com", "secreta123")
        val ok = client.requestEmailChange(session.tokens.accessToken, "nueva@example.com", "secreta123")

        assertEquals(HttpStatusCode.Unauthorized, wrongPassword.status)
        assertEquals(HttpStatusCode.Conflict, inUse.status)
        assertEquals(ErrorCode.EMAIL_ALREADY_IN_USE, inUse.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.NoContent, ok.status)
        assertEquals("ana@example.com", TestDatabase.column<String>("SELECT email FROM accounts WHERE id = '${session.account.id}'").single())
        val mail = TestMailServer.awaitMessageTo("nueva@example.com")
        assertEquals("Confirmá tu nuevo email de Pasitos", mail?.subject)
        assertTrue(mail!!.text.contains("/web/confirm-email-change?token="))
    }

    @Test
    fun `RF-CTA-11 CA1 confirmar el cambio reemplaza el email y avisa al anterior`() = apiTest { client ->
        val session = client.register()
        client.requestEmailChange(session.tokens.accessToken, "nueva@example.com", "secreta123")
        val token = extractToken(TestMailServer.awaitMessageTo("nueva@example.com")!!.text)
        TestMailServer.reset()

        val response = client.get("/web/confirm-email-change?token=$token")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("nueva@example.com", TestDatabase.column<String>("SELECT email FROM accounts WHERE id = '${session.account.id}'").single())
        val notice = TestMailServer.awaitMessageTo("ana@example.com")
        assertEquals("Tu email de Pasitos cambió", notice?.subject)
        assertTrue(notice!!.text.contains("nueva@example.com"))
    }

    @Test
    fun `un link de cambio de email vencido o ya usado informa el problema`() = apiTest { client ->
        val session = client.register()
        client.requestEmailChange(session.tokens.accessToken, "nueva@example.com", "secreta123")
        val token = extractToken(TestMailServer.awaitMessageTo("nueva@example.com")!!.text)

        val unknown = client.get("/web/confirm-email-change?token=no-existe")
        val first = client.get("/web/confirm-email-change?token=$token")
        val reused = client.get("/web/confirm-email-change?token=$token")

        assertEquals(HttpStatusCode.BadRequest, unknown.status)
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(HttpStatusCode.BadRequest, reused.status, "RN-CTA-06: un link usado no sirve dos veces")
    }

    @Test
    fun `RN-CTA-02 aceptar los terminos limpia la version pendiente`() = apiTest { client ->
        val session = client.register()
        // El registro ya acepta la versión vigente (RF-CTA-01): se simula una versión vieja, como
        // si los términos hubieran cambiado desde entonces.
        TestDatabase.connect().use { it.createStatement().execute("UPDATE consents SET version = 'vieja'") }
        val before = client.get("/api/v1/me") { bearerAuth(session.tokens.accessToken) }.body<MeResponse>()

        val response = client.post("/api/v1/me/consents") { bearerAuth(session.tokens.accessToken) }
        val after = client.get("/api/v1/me") { bearerAuth(session.tokens.accessToken) }.body<MeResponse>()

        assertEquals(TestDatabase.TERMS_VERSION, before.pendingTermsVersion)
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNull(after.pendingTermsVersion)
    }

    private suspend fun HttpClient.patchJson(token: String, body: PatchMeRequest): HttpResponse = patch("/api/v1/me") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun HttpClient.requestEmailChange(token: String, newEmail: String, password: String): HttpResponse =
        post("/api/v1/me/email-change") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(EmailChangeRequest(newEmail, password))
        }

    private suspend fun HttpClient.verifyEmail(email: String) {
        val token = extractToken(TestMailServer.awaitMessageTo(email)!!.text)
        get("/web/verify-email?token=$token")
    }

    private fun extractToken(text: String): String =
        Regex("token=([^\\s&]+)").find(text)?.groupValues?.get(1) ?: error("El email no tiene un link con token: $text")

    private suspend inline fun <reified T> HttpClient.postJson(path: String, body: T): HttpResponse = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}
