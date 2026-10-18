package com.pasitos.backend.auth

import com.pasitos.backend.support.TestClock
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.MeResponse
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.RegisterRequest
import com.pasitos.shared.api.auth.TokenPair
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
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** Rutas de `/api/v1/auth/...` y `/api/v1/me` (I0-05, ADR-11). */
class AuthRoutesTest {

    @Test
    fun `RF-CTA-01 el registro crea la cuenta, guarda el consentimiento y abre una sesión`() = apiTest { client ->
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
        val registered = client.registerV1()

        val response = client.postJson("/api/v1/auth/login", LoginRequest("ana@example.com", PASSWORD))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertEquals(registered.account, body.account)
        assertNotEquals(registered.tokens.refreshToken, body.tokens.refreshToken)
        // La verificación de email todavía no se exige: llega en I1-B1.
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/me") { bearerAuth(body.tokens.accessToken) }.status)
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
            client.registerV1()

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
        client.registerV1()

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
    fun `un token con una fecha fuera de rango responde 401 y no 500`() = apiTest { client ->
        // Fechas que no entran en un Instant. java-jwt las lee antes de verificar la firma, así que
        // cualquiera puede mandarlas sin estar autenticado (#98)
        val outOfRange = listOf(
            "31556889864403200", // un segundo más que el máximo de Instant
            "9223372036854775807", // Long.MAX_VALUE
            "-9223372036854775808", // Long.MIN_VALUE
            "1e30",
            "99999999999999999999999", // no entra en un Long
        )

        for (exp in outOfRange) {
            for (claim in listOf("exp", "nbf", "iat")) {
                val token = unsignedToken("""{"sub":"x","$claim":$exp}""")

                val response = client.get("/api/v1/me") { bearerAuth(token) }

                assertEquals(HttpStatusCode.Unauthorized, response.status, "$claim = $exp")
            }
        }
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

    private suspend fun HttpClient.registerV1(): AuthResponse {
        val response = postJson("/api/v1/auth/register", registerRequest())
        assertEquals(HttpStatusCode.Created, response.status)
        return response.body()
    }

    private suspend fun HttpClient.refresh(refreshToken: String): HttpResponse =
        postJson("/api/v1/auth/refresh", RefreshRequest(refreshToken))

    private suspend inline fun <reified T> HttpClient.postJson(path: String, body: T): HttpResponse = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    /** Un JWT bien formado con el [payload] dado y una firma que no es válida. */
    private fun unsignedToken(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        fun part(text: String) = encoder.encodeToString(text.toByteArray())
        return "${part("""{"alg":"HS256","typ":"JWT"}""")}.${part(payload)}.${part("firma")}"
    }

    private companion object {
        const val PASSWORD = "secreta123"
    }
}
