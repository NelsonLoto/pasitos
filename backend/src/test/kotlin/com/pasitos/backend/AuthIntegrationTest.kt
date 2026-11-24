package com.pasitos.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import com.pasitos.backend.account.AccountsTable
import com.pasitos.backend.account.ConsentsTable
import com.pasitos.backend.auth.*
import com.pasitos.backend.common.DatabaseConfig
import com.pasitos.backend.common.DatabaseFactory
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

@Testcontainers
class AuthIntegrationTest {
    companion object {
        @Container @JvmStatic val postgres = PostgreSQLContainer("postgres:16-alpine")
        private lateinit var factory: DatabaseFactory
        private lateinit var passwords: PasswordHasher
        private val secret = "synthetic-test-signing-key-only-".repeat(3)
        @BeforeAll @JvmStatic fun setup() {
            factory = DatabaseFactory(DatabaseConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password))
            passwords = PasswordHasher()
        }
        @AfterAll @JvmStatic fun close() = factory.close()
    }

    private val clock = MutableClock(Instant.parse("2026-11-22T12:00:00Z"))
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach fun clearAccounts() = transaction(factory.database) {
        SessionsTable.deleteAll(); ConsentsTable.deleteAll(); AccountsTable.deleteAll(); Unit
    }

    private fun Application.auth(limit: Int = 500, terms: String = "1") {
        configureHttp(factory::isHealthy)
        val config = AuthConfig(secret, terms, limit)
        configureAuth(AuthService(factory.database, config, AuthTokens(config, clock), passwords, clock), config)
    }

    private fun registration(email: String = "synthetic@example.test") = RegisterRequest(
        "Prueba", "Sintética", email, "Prueba12345", true,
    )

    private suspend inline fun <reified T> HttpClient.send(path: String, body: T): HttpResponse = post("/api/v1$path") {
        contentType(ContentType.Application.Json); setBody(Json.encodeToString(body))
    }

    private suspend fun HttpClient.register(request: RegisterRequest = registration()): AuthResponse {
        val response = send("/auth/register", request)
        assertEquals(HttpStatusCode.Created, response.status)
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun HttpClient.me(access: String) = get("/api/v1/me") { bearerAuth(access) }
    private suspend fun error(response: HttpResponse, expected: HttpStatusCode, code: ErrorCode) {
        assertEquals(expected, response.status)
        assertEquals(code, json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test fun `I0-05 registro guarda bcrypt y consentimiento y permite login sin verificar email`() = testApplication {
        application { auth() }
        val session = client.register(registration("  SYNTHETIC@EXAMPLE.TEST  "))
        assertEquals("synthetic@example.test", session.account.email)
        assertFalse(session.account.emailVerified)
        assertFalse(session.account.termsPending)
        assertTrue(session.account.hasPassword)
        val jwt = JWT.decode(session.accessToken)
        assertEquals(session.account.id, jwt.subject)
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.issuedAtAsInstant, jwt.expiresAtAsInstant))
        assertEquals("HS256", jwt.algorithm)
        transaction(factory.database) {
            val account = AccountsTable.selectAll().single()
            assertTrue(account[AccountsTable.passwordHash]!!.startsWith("\$2a\$12\$"))
            assertNotEquals("Prueba12345", account[AccountsTable.passwordHash])
            assertEquals("1", ConsentsTable.selectAll().single()[ConsentsTable.version])
            val stored = SessionsTable.selectAll().single()[SessionsTable.refreshTokenHash]
            assertEquals(32, stored.size)
            assertContentEquals(AuthTokens(AuthConfig(secret), clock).hash(session.refreshToken), stored)
        }
        error(client.get("/api/v1/me"), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        assertEquals(HttpStatusCode.OK, client.me(session.accessToken).status)
        val login = client.send("/auth/login", LoginRequest("SYNTHETIC@example.test", "Prueba12345"))
        assertEquals(HttpStatusCode.OK, login.status)
        assertEquals(session.account.id, json.decodeFromString<AuthResponse>(login.bodyAsText()).account.id)
    }

    @Test fun `RN-CTA-01 y 02 registro rechaza datos invalidos terminos y email duplicado`() = testApplication {
        application { auth() }
        error(client.send("/auth/register", registration().copy(password = "abcdefgh")), HttpStatusCode.BadRequest, ErrorCode.INVALID_INPUT)
        error(client.send("/auth/register", registration().copy(password = "12345678")), HttpStatusCode.BadRequest, ErrorCode.INVALID_INPUT)
        error(client.send("/auth/register", registration().copy(acceptTerms = false)), HttpStatusCode.Conflict, ErrorCode.TERMS_REQUIRED)
        error(client.send("/auth/register", registration().copy(termsVersion = "old")), HttpStatusCode.Conflict, ErrorCode.TERMS_REQUIRED)
        transaction(factory.database) { assertEquals(0L, AccountsTable.selectAll().count()) }
        client.register()
        error(client.send("/auth/register", registration()), HttpStatusCode.Conflict, ErrorCode.EMAIL_ALREADY_IN_USE)
        transaction(factory.database) { assertEquals(1L, AccountsTable.selectAll().count()) }
    }

    @Test fun `RF-CTA-04 credenciales incorrectas no distinguen email desconocido de password incorrecta`() = testApplication {
        application { auth() }
        client.register()
        val known = client.send("/auth/login", LoginRequest("synthetic@example.test", "incorrecta"))
        val unknown = client.send("/auth/login", LoginRequest("unknown@example.test", "incorrecta"))
        error(known, HttpStatusCode.Unauthorized, ErrorCode.INVALID_CREDENTIALS)
        assertEquals(known.status, unknown.status)
        assertEquals(known.bodyAsText(), unknown.bodyAsText())
        transaction(factory.database) { assertEquals(1, AccountsTable.selectAll().single()[AccountsTable.failedLoginCount]) }
    }

    @Test fun `RN-CTA-05 cinco errores persisten bloquean y se recupera el acceso a los 15 minutos`() = testApplication {
        application { auth() }
        client.register()
        repeat(5) { error(client.send("/auth/login", LoginRequest("synthetic@example.test", "incorrecta")), HttpStatusCode.Unauthorized, ErrorCode.INVALID_CREDENTIALS) }
        error(client.send("/auth/login", LoginRequest("synthetic@example.test", "Prueba12345")), HttpStatusCode(423, "Locked"), ErrorCode.ACCOUNT_LOCKED)
        transaction(factory.database) { assertEquals(5, AccountsTable.selectAll().single()[AccountsTable.failedLoginCount]) }
        clock.advance(Duration.ofMinutes(15))
        assertEquals(HttpStatusCode.OK, client.send("/auth/login", LoginRequest("synthetic@example.test", "Prueba12345")).status)
        transaction(factory.database) {
            assertEquals(0, AccountsTable.selectAll().single()[AccountsTable.failedLoginCount])
            assertNull(AccountsTable.selectAll().single()[AccountsTable.lockedUntil])
        }
    }

    @Test fun `RF-CTA-06 acceso vence a los 15 minutos y refresh rota sin cambiar cuenta`() = testApplication {
        application { auth() }
        val original = client.register()
        clock.advance(Duration.ofMinutes(15))
        error(client.me(original.accessToken), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        val response = client.send("/auth/refresh", RefreshRequest(original.refreshToken))
        assertEquals(HttpStatusCode.OK, response.status)
        val replacement = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertNotEquals(original.refreshToken, replacement.refreshToken)
        assertEquals(original.account.id, replacement.account.id)
        assertEquals(HttpStatusCode.OK, client.me(replacement.accessToken).status)
        error(client.send("/auth/refresh", RefreshRequest(original.refreshToken)), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        // Reutilizar el token anterior revoca también el permiso de acceso nuevo.
        error(client.me(replacement.accessToken), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        transaction(factory.database) { assertEquals(0L, SessionsTable.selectAll().count()) }
    }

    @Test fun `I0-05 un refresh mas antiguo que el anterior no revoca la sesion actual`() = testApplication {
        application { auth() }
        val first = client.register()
        val second = json.decodeFromString<AuthResponse>(client.send("/auth/refresh", RefreshRequest(first.refreshToken)).bodyAsText())
        val third = json.decodeFromString<AuthResponse>(client.send("/auth/refresh", RefreshRequest(second.refreshToken)).bodyAsText())
        error(client.send("/auth/refresh", RefreshRequest(first.refreshToken)), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        assertEquals(HttpStatusCode.OK, client.me(third.accessToken).status)
    }

    @Test fun `RN-CTA-07 renovar extiende 30 dias y una sesion inactiva vence`() = testApplication {
        application { auth() }
        val first = client.register()
        clock.advance(Duration.ofDays(29))
        val second = json.decodeFromString<AuthResponse>(client.send("/auth/refresh", RefreshRequest(first.refreshToken)).bodyAsText())
        clock.advance(Duration.ofDays(29))
        val third = json.decodeFromString<AuthResponse>(client.send("/auth/refresh", RefreshRequest(second.refreshToken)).bodyAsText())
        clock.advance(Duration.ofDays(30))
        error(client.send("/auth/refresh", RefreshRequest(third.refreshToken)), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
    }

    @Test fun `RF-CTA-07 logout invalida acceso y refresh solo de esa sesion`() = testApplication {
        application { auth() }
        val first = client.register()
        val second = json.decodeFromString<AuthResponse>(client.send("/auth/login", LoginRequest("synthetic@example.test", "Prueba12345")).bodyAsText())
        assertEquals(HttpStatusCode.NoContent, client.post("/api/v1/auth/logout") { bearerAuth(first.accessToken) }.status)
        error(client.me(first.accessToken), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        error(client.send("/auth/refresh", RefreshRequest(first.refreshToken)), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        assertEquals(HttpStatusCode.OK, client.me(second.accessToken).status)
    }

    @Test fun `I0-05 dos renovaciones simultaneas detectan reuso sin dejar sesion activa`() = testApplication {
        application { auth() }
        val session = client.register()
        val responses = coroutineScope { List(2) { async { client.send("/auth/refresh", RefreshRequest(session.refreshToken)) } }.awaitAll() }
        assertEquals(listOf(200, 401), responses.map { it.status.value }.sorted())
        transaction(factory.database) { assertEquals(0L, SessionsTable.selectAll().count()) }
    }

    @Test fun `I0-05 firma manipulada algoritmo none y sid de otra cuenta se rechazan`() = testApplication {
        application { auth() }
        val session = client.register()
        val original = JWT.decode(session.accessToken)
        fun forged(algorithm: Algorithm, subject: String = session.account.id, sid: String = original.getClaim("sid").asString()) = JWT.create()
            .withIssuer("pasitos").withAudience("pasitos-app").withSubject(subject).withClaim("sid", sid)
            .withJWTId(UUID.randomUUID().toString()).withIssuedAt(clock.instant()).withExpiresAt(clock.instant().plusSeconds(900)).sign(algorithm)
        for (token in listOf(forged(Algorithm.none()), forged(Algorithm.HMAC256("wrong")),
            forged(Algorithm.HMAC256(secret), UUID.randomUUID().toString()), forged(Algorithm.HMAC256(secret), sid = "not-a-uuid"))) {
            error(client.me(token), HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED)
        }
    }

    @Test fun `I0-05 limite acordado admite diez pedidos y rechaza el undecimo por la misma conexion`() = testApplication {
        application { auth(limit = 10) }
        repeat(10) { error(client.send("/auth/login", LoginRequest("unknown@example.test", "incorrecta")), HttpStatusCode.Unauthorized, ErrorCode.INVALID_CREDENTIALS) }
        val rejected = client.send("/auth/login", LoginRequest("unknown@example.test", "incorrecta"))
        error(rejected, HttpStatusCode.TooManyRequests, ErrorCode.RATE_LIMITED)
        assertNotNull(rejected.headers[HttpHeaders.RetryAfter])
    }

    @Test fun `RN-CTA-07 usar una ruta autenticada extiende la inactividad`() = testApplication {
        application { auth() }
        val session = client.register()
        clock.advance(Duration.ofMinutes(10))
        assertEquals(HttpStatusCode.OK, client.me(session.accessToken).status)
        transaction(factory.database) {
            val row = SessionsTable.selectAll().single()
            assertEquals(clock.instant(), row[SessionsTable.lastUsedAt])
            assertEquals(clock.instant().plus(Duration.ofDays(30)), row[SessionsTable.expiresAt])
        }
    }

    @Test fun `I0-05 registros simultaneos del mismo email no crean cuentas ni consentimientos duplicados`() = testApplication {
        application { auth() }
        val responses = coroutineScope { List(2) { async { client.send("/auth/register", registration()) } }.awaitAll() }
        assertEquals(listOf(201, 409), responses.map { it.status.value }.sorted())
        transaction(factory.database) {
            assertEquals(1L, AccountsTable.selectAll().count())
            assertEquals(1L, ConsentsTable.selectAll().count())
            assertEquals(1L, SessionsTable.selectAll().count())
        }
    }

    @Test fun `RNF-SEG registros de autenticacion omiten datos personales contrasenas y tokens`() = testApplication {
        application { auth() }
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val logs = ListAppender<ILoggingEvent>().also { it.start(); root.addAppender(it) }
        try {
            val request = registration().copy(firstName = "NombrePrivadoUnico", lastName = "ApellidoPrivadoUnico")
            val session = client.register(request)
            client.send("/auth/login", LoginRequest(request.email, "IncorrectaPrivada123"))
            client.me(session.accessToken)
            client.send("/auth/refresh", RefreshRequest(session.refreshToken))
            client.send("/auth/refresh", RefreshRequest(session.refreshToken))
            val output = logs.list.joinToString { it.formattedMessage }
            assertTrue(output.contains("/auth/register"))
            for (sensitive in listOf(request.email, request.firstName, request.lastName, request.password,
                "IncorrectaPrivada123", session.accessToken, session.refreshToken, secret)) {
                assertFalse(output.contains(sensitive))
            }
        } finally { root.detachAppender(logs); logs.stop() }
    }

    private class MutableClock(initial: Instant) : Clock() {
        private val value = AtomicReference(initial)
        fun advance(duration: Duration) { value.updateAndGet { it.plus(duration) } }
        override fun instant(): Instant = value.get()
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
}
