package com.pasitos.backend.auth

import com.pasitos.backend.support.TestClock
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.TestMailServer
import com.pasitos.backend.support.apiTest
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.EmailVerificationRequest
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.MeResponse
import com.pasitos.shared.api.auth.PasswordResetRequest
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.RegisterRequest
import com.pasitos.shared.api.auth.TokenPair
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Rutas de `/api/v1/auth/...` y `/api/v1/me` (I0-05, ADR-11). */
class AuthRoutesTest {

    @Test
    fun `RF-CTA-01 CA1 el registro crea la cuenta, guarda el consentimiento, abre una sesión y manda la verificación`() = apiTest { client ->
        val response = client.postJson("/api/v1/auth/register", registerRequest(email = " Ana@Example.com "))

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<AuthResponse>()
        assertEquals("ana@example.com", body.account.email)
        assertEquals("Ana", body.account.firstName)
        assertEquals("Pérez", body.account.lastName)
        assertEquals(false, body.account.emailVerified)
        assertEquals(true, body.account.isGuardian)
        assertEquals(
            listOf("TERMS ${TestDatabase.TERMS_VERSION}"),
            TestDatabase.column<String>("SELECT type || ' ' || version FROM consents"),
        )
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM sessions").single())
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM email_tokens WHERE purpose = 'VERIFY_EMAIL'").single())

        // RF-CTA-02: el email de verificación llegó de verdad, al servidor SMTP de prueba.
        val mail = TestMailServer.awaitMessageTo("ana@example.com")
        assertEquals("Confirmá tu email de Pasitos", mail?.subject)
        assertTrue(mail!!.html.contains("/web/verify-email?token="), "El HTML no tiene el link de verificación: ${mail.html}")
        assertTrue(mail.text.contains("/web/verify-email?token="), "El texto no tiene el link de verificación: ${mail.text}")
    }

    @Test
    fun `RN-CTA-01 y RN-CTA-02 el registro exige contraseña segura y aceptar los términos`() = apiTest { client ->
        val invalid = listOf(
            registerRequest(password = "corta1"),
            registerRequest(password = "sololetras"),
            registerRequest(password = "12345678"),
            registerRequest(acceptTerms = false),
            registerRequest(email = "no-es-un-email"),
            registerRequest(lastName = " "),
        )

        invalid.forEach { request ->
            val response = client.postJson("/api/v1/auth/register", request)
            assertEquals(HttpStatusCode.BadRequest, response.status, "Pedido: $request")
            assertEquals(ErrorCode.INVALID_INPUT, response.body<ErrorResponse>().code)
        }
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM accounts").single())
    }

    @Test
    fun `RN-CTA-03 el registro con un email en uso responde 409`() = apiTest { client ->
        client.registerV1()

        val response = client.postJson("/api/v1/auth/register", registerRequest(email = "ANA@example.com"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(ErrorCode.EMAIL_ALREADY_IN_USE, response.body<ErrorResponse>().code)
    }

    @Test
    fun `RF-CTA-04 CA1 el login correcto devuelve la cuenta y un par de tokens`() = apiTest { client ->
        val registered = client.registerAndVerifyV1()

        val response = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertEquals(registered.account.copy(emailVerified = true), body.account)
        assertNotEquals(registered.tokens.refreshToken, body.tokens.refreshToken)
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/me") { bearerAuth(body.tokens.accessToken) }.status)
    }

    @Test
    fun `RF-CTA-02 CA1 el login con el email sin verificar no abre sesión y no cuenta como intento fallido`() = apiTest { client ->
        client.registerV1()

        val response = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, response.body<ErrorResponse>().code)
        assertEquals(0, TestDatabase.column<Int>("SELECT failed_login_count FROM accounts").single())
        // El registro ya abrió una sesión (RF-CTA-01, CA1); el login bloqueado no agrega otra.
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM sessions").single())
    }

    @Test
    fun `RF-CTA-02 CA2 el link de verificación vigente marca el email como verificado`() = apiTest { client ->
        client.registerV1()
        val token = client.awaitVerificationToken("ana@example.com")

        val response = client.get("/web/verify-email?token=$token")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("verificado"))
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM accounts WHERE email_verified_at IS NOT NULL").single())
    }

    @Test
    fun `RF-CTA-02 CA3 un link de verificación vencido o ya usado informa el problema`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            client.registerV1()
            val token = client.awaitVerificationToken("ana@example.com")

            val unknown = client.get("/web/verify-email?token=no-existe")
            val usedTwice = listOf(client.get("/web/verify-email?token=$token"), client.get("/web/verify-email?token=$token"))

            client.registerV1(email = "beta@example.com")
            val betaToken = client.awaitVerificationToken("beta@example.com")
            clock.advance(Duration.ofHours(24))
            val expired = client.get("/web/verify-email?token=$betaToken")

            assertEquals(HttpStatusCode.BadRequest, unknown.status)
            assertEquals(HttpStatusCode.OK, usedTwice[0].status)
            assertEquals(HttpStatusCode.BadRequest, usedTwice[1].status, "RN-CTA-06: un link usado no sirve dos veces")
            assertEquals(HttpStatusCode.BadRequest, expired.status, "RN-CTA-06: vence a las 24 horas")
        }
    }

    @Test
    fun `RF-CTA-02 reenviar la verificación manda un email nuevo solo si la cuenta existe y no está verificada`() = apiTest { client ->
        client.registerV1()
        TestMailServer.reset()

        val resent = client.postJson("/api/v1/auth/email-verification", EmailVerificationRequest("ana@example.com"))
        val unknown = client.postJson("/api/v1/auth/email-verification", EmailVerificationRequest("nadie@example.com"))

        assertEquals(HttpStatusCode.NoContent, resent.status)
        assertEquals(HttpStatusCode.NoContent, unknown.status)
        assertEquals("Confirmá tu email de Pasitos", TestMailServer.awaitMessageTo("ana@example.com")?.subject)
        assertNull(TestMailServer.awaitMessageTo("nadie@example.com", timeout = Duration.ofMillis(500)))

        val token = client.awaitVerificationToken("ana@example.com")
        client.get("/web/verify-email?token=$token")
        TestMailServer.reset()
        val afterVerified = client.postJson("/api/v1/auth/email-verification", EmailVerificationRequest("ana@example.com"))

        assertEquals(HttpStatusCode.NoContent, afterVerified.status)
        assertNull(TestMailServer.awaitMessageTo("ana@example.com", timeout = Duration.ofMillis(500)))
    }

    @Test
    fun `RF-CTA-05 CA1 pedir recuperación responde lo mismo exista o no la cuenta`() = apiTest { client ->
        client.registerV1()

        val known = client.postJson("/api/v1/auth/password-reset", PasswordResetRequest("ana@example.com"))
        val unknown = client.postJson("/api/v1/auth/password-reset", PasswordResetRequest("nadie@example.com"))

        assertEquals(HttpStatusCode.NoContent, known.status)
        assertEquals(HttpStatusCode.NoContent, unknown.status)
        assertEquals("Recuperá tu contraseña de Pasitos", TestMailServer.awaitMessageTo("ana@example.com")?.subject)
        assertNull(TestMailServer.awaitMessageTo("nadie@example.com", timeout = Duration.ofMillis(500)))
    }

    @Test
    fun `RF-CTA-05 CA2 el link vigente cambia la contraseña y cierra todas las sesiones`() = apiTest { client ->
        client.registerAndVerifyV1()
        // Dos sesiones abiertas (dos dispositivos): ambas tienen que caer, no solo la que pidió el reset.
        client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))
        val token = client.awaitResetToken("ana@example.com")

        val reset = client.submitResetPassword(token, "nuevaClave99")

        assertEquals(HttpStatusCode.OK, reset.status)
        assertTrue(reset.bodyAsText().contains("actualizó"))
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM sessions").single())
        val loginOld = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))
        val loginNew = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", "nuevaClave99"))
        assertEquals(HttpStatusCode.Unauthorized, loginOld.status)
        assertEquals(HttpStatusCode.OK, loginNew.status)
    }

    @Test
    fun `RF-CTA-05 CA3 un link de recuperación vencido o ya usado informa el problema`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            client.registerV1()
            val token = client.awaitResetToken("ana@example.com")

            val unknown = client.submitResetPassword("no-existe", "nuevaClave99")
            val usedTwice = listOf(client.submitResetPassword(token, "nuevaClave99"), client.submitResetPassword(token, "otraClave99"))

            val secondToken = client.awaitResetToken("ana@example.com")
            clock.advance(Duration.ofMinutes(60))
            val expired = client.submitResetPassword(secondToken, "nuevaClave99")

            assertEquals(HttpStatusCode.BadRequest, unknown.status)
            assertEquals(HttpStatusCode.OK, usedTwice[0].status)
            assertEquals(HttpStatusCode.BadRequest, usedTwice[1].status, "RN-CTA-06: un link usado no sirve dos veces")
            assertEquals(HttpStatusCode.BadRequest, expired.status, "RN-CTA-06: vence a los 60 minutos")
        }
    }

    @Test
    fun `RN-CTA-01 el formulario de recuperación exige una contraseña válida y no gasta el link`() = apiTest { client ->
        client.registerV1()
        val token = client.awaitResetToken("ana@example.com")

        val weak = client.submitResetPassword(token, "corta1")
        val retry = client.submitResetPassword(token, "nuevaClave99")

        assertEquals(HttpStatusCode.OK, weak.status)
        assertFalse(weak.bodyAsText().contains("actualizó"))
        assertEquals(HttpStatusCode.OK, retry.status)
        assertTrue(retry.bodyAsText().contains("actualizó"))
    }

    @Test
    fun `RF-CTA-04 CA2 credenciales incorrectas responden lo mismo sin decir cuál falló`() = apiTest { client ->
        client.registerV1()

        val wrongPassword = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", "otraClave99"))
        val unknownEmail = client.postJson("/api/v1/auth/login", LoginRequest("nadie@example.com", PASSWORD))

        listOf(wrongPassword, unknownEmail).forEach { response ->
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals(ErrorCode.INVALID_CREDENTIALS, error.code)
            assertEquals("Email o contraseña incorrectos", error.message)
        }
    }

    @Test
    fun `RF-CTA-09 GET me devuelve la cuenta y los términos pendientes`() = apiTest { client ->
        val session = client.registerV1()

        val upToDate = client.get("/api/v1/me") { bearerAuth(session.tokens.accessToken) }.body<MeResponse>()
        TestDatabase.connect().use { it.createStatement().execute("UPDATE consents SET version = 'vieja'") }
        val outdated = client.get("/api/v1/me") { bearerAuth(session.tokens.accessToken) }.body<MeResponse>()

        assertEquals(session.account, upToDate.account)
        assertNull(upToDate.pendingTermsVersion)
        assertEquals(TestDatabase.TERMS_VERSION, outdated.pendingTermsVersion)
    }

    @Test
    fun `ADR-11 el token de acceso vence a los 15 minutos y se renueva con el de renovación`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            val session = client.registerV1()

            clock.advance(Duration.ofMinutes(16))
            val expired = client.get("/api/v1/me") { bearerAuth(session.tokens.accessToken) }
            val renewed = client.refresh(session.tokens.refreshToken)

            assertEquals(HttpStatusCode.Unauthorized, expired.status)
            assertEquals(HttpStatusCode.OK, renewed.status)
            val tokens = renewed.body<TokenPair>()
            assertEquals(clock.instant().plus(Duration.ofMinutes(15)), tokens.accessTokenExpiresAt)
            assertEquals(HttpStatusCode.OK, client.get("/api/v1/me") { bearerAuth(tokens.accessToken) }.status)
        }
    }

    @Test
    fun `ADR-11 rotación, el token de renovación viejo ya no sirve y reutilizarlo revoca la sesión`() = apiTest { client ->
        val session = client.registerV1()
        val rotated = client.refresh(session.tokens.refreshToken).body<TokenPair>()

        val reused = client.refresh(session.tokens.refreshToken)
        val afterReuse = client.refresh(rotated.refreshToken)
        val meAfterReuse = client.get("/api/v1/me") { bearerAuth(rotated.accessToken) }

        assertEquals(HttpStatusCode.Unauthorized, reused.status)
        assertEquals(ErrorCode.UNAUTHORIZED, reused.body<ErrorResponse>().code)
        // Alguien más tenía el token: la sesión entera se revoca, también para el dueño legítimo.
        assertEquals(HttpStatusCode.Unauthorized, afterReuse.status)
        assertEquals(HttpStatusCode.Unauthorized, meAfterReuse.status)
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM sessions").single())
    }

    @Test
    fun `RN-CTA-07 la sesión vence tras 30 días sin uso y cada renovación extiende el plazo`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            val session = client.registerV1()

            clock.advance(Duration.ofDays(20))
            val day20 = client.refresh(session.tokens.refreshToken).body<TokenPair>()
            clock.advance(Duration.ofDays(20))
            val day40 = client.refresh(day20.refreshToken)
            val tokensDay40 = day40.body<TokenPair>()
            clock.advance(Duration.ofDays(31))
            val day71 = client.refresh(tokensDay40.refreshToken)

            assertEquals(HttpStatusCode.OK, day40.status)
            // RF-CTA-06 CA2: más de 30 días sin abrir la app obliga a iniciar sesión de nuevo.
            assertEquals(HttpStatusCode.Unauthorized, day71.status)
        }
    }

    @Test
    fun `RF-CTA-07 el logout revoca la sesión y sus dos tokens`() = apiTest { client ->
        val session = client.registerV1()

        val logout = client.post("/api/v1/auth/logout") { bearerAuth(session.tokens.accessToken) }

        assertEquals(HttpStatusCode.NoContent, logout.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/me") { bearerAuth(session.tokens.accessToken) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(session.tokens.refreshToken).status)
    }

    @Test
    fun `RN-CTA-05 el quinto intento fallido bloquea el inicio con contraseña por 15 minutos`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            client.registerAndVerifyV1()

            repeat(5) {
                val failed = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", "otraClave99"))
                assertEquals(HttpStatusCode.Unauthorized, failed.status)
            }
            val locked = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))
            clock.advance(Duration.ofMinutes(15))
            val afterLock = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))

            // RF-CTA-04 CA3: el sexto intento ya está bloqueado, aunque la contraseña sea correcta.
            assertEquals(HttpStatusCode.Locked, locked.status)
            val error = locked.body<ErrorResponse>()
            assertEquals(ErrorCode.ACCOUNT_LOCKED, error.code)
            assertEquals("Hubo demasiados intentos fallidos. Probá de nuevo en 15 minutos", error.message)
            assertEquals(HttpStatusCode.OK, afterLock.status)
        }
    }

    @Test
    fun `RN-CTA-05 un login correcto reinicia la cuenta de intentos fallidos`() = apiTest { client ->
        client.registerAndVerifyV1()

        repeat(4) { client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", "otraClave99")) }
        client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))
        repeat(4) { client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", "otraClave99")) }
        val response = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `RNF-SEG-07 más de 10 logins por minuto desde la misma IP responden 429`() = apiTest { client ->
        repeat(10) {
            val response = client.postJson("/api/v1/auth/login", LoginRequest("nadie@example.com", PASSWORD))
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        val limited = client.postJson("/api/v1/auth/login", LoginRequest("nadie@example.com", PASSWORD))

        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertEquals(ErrorCode.RATE_LIMITED, limited.body<ErrorResponse>().code)
    }

    @Test
    fun `RNF-SEG-04 una sesión vencida no sirve aunque el token de acceso siga vigente`() = apiTest { client ->
        val session = client.registerV1()
        TestDatabase.connect().use { it.createStatement().execute("UPDATE sessions SET expires_at = now() - interval '1 minute'") }

        val response = client.get("/api/v1/me") { bearerAuth(session.tokens.accessToken) }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `las rutas protegidas sin token o con un token inválido responden 401`() = apiTest { client ->
        val withoutToken = client.get("/api/v1/me")
        val invalidToken = client.get("/api/v1/me") { bearerAuth("no-es-un-jwt") }

        assertEquals(HttpStatusCode.Unauthorized, withoutToken.status)
        assertEquals(ErrorCode.UNAUTHORIZED, withoutToken.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.Unauthorized, invalidToken.status)
    }

    @Test
    fun `los endpoints viejos de auth ya no existen`() = apiTest { client ->
        val login = client.postJson("/auth/login", LoginRequest("ana@example.com", PASSWORD))

        assertEquals(HttpStatusCode.NotFound, login.status)
    }

    private fun registerRequest(
        email: String = "ana@example.com",
        password: String = PASSWORD,
        lastName: String = "Pérez",
        acceptTerms: Boolean = true,
    ) = RegisterRequest(firstName = "Ana", lastName = lastName, email = email, password = password, acceptTerms = acceptTerms)

    private suspend fun HttpClient.registerV1(email: String = "ana@example.com"): AuthResponse {
        val response = postJson("/api/v1/auth/register", registerRequest(email = email))
        assertEquals(HttpStatusCode.Created, response.status)
        return response.body()
    }

    /** Registra y además verifica el email, siguiendo el link que llegó de verdad al SMTP de prueba. */
    private suspend fun HttpClient.registerAndVerifyV1(email: String = "ana@example.com"): AuthResponse {
        val registered = registerV1(email)
        val token = awaitVerificationToken(email)
        val verify = get("/web/verify-email?token=$token")
        assertEquals(HttpStatusCode.OK, verify.status)
        return registered
    }

    private suspend fun HttpClient.awaitVerificationToken(email: String): String =
        extractToken(TestMailServer.awaitMessageTo(email)?.text ?: error("No llegó el email de verificación a $email"))

    private suspend fun HttpClient.awaitResetToken(email: String): String {
        postJson("/api/v1/auth/password-reset", PasswordResetRequest(email))
        return extractToken(TestMailServer.awaitMessageTo(email)?.text ?: error("No llegó el email de recuperación a $email"))
    }

    private fun extractToken(text: String): String =
        Regex("token=([^\\s&]+)").find(text)?.groupValues?.get(1) ?: error("El email no tiene un link con token: $text")

    private suspend fun HttpClient.submitResetPassword(token: String, newPassword: String): HttpResponse =
        submitForm(
            url = "/web/reset-password",
            formParameters = Parameters.build {
                append("token", token)
                append("newPassword", newPassword)
            },
        )

    private suspend fun HttpClient.refresh(refreshToken: String): HttpResponse =
        postJson("/api/v1/auth/refresh", RefreshRequest(refreshToken))

    private suspend inline fun <reified T> HttpClient.postJson(path: String, body: T): HttpResponse = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val PASSWORD = "secreta123"
    }
}
