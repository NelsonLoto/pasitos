package com.pasitos.backend.support

import com.pasitos.backend.auth.LegacyAuthResponse
import com.pasitos.backend.auth.LegacyRegisterRequest
import com.pasitos.backend.child.ChildResponse
import com.pasitos.backend.child.CreateChildRequest
import com.pasitos.backend.module
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals

/**
 * Levanta el backend completo contra el PostgreSQL de prueba, con todas las tablas vacías. Con
 * [clock] la prueba controla la hora del backend: tokens, sesiones y bloqueos vencen según él.
 */
fun apiTest(
    clock: Clock = Clock.systemUTC(),
    block: suspend ApplicationTestBuilder.(client: HttpClient) -> Unit,
) = testApplication {
    TestDatabase.reset()
    environment { config = TestDatabase.config() }
    application { module(clock) }
    val client = createClient {
        install(ContentNegotiation) { json() }
    }
    block(client)
}

/** Reloj que avanza solo cuando la prueba lo pide. */
class TestClock(private var now: Instant = Instant.parse("2026-10-16T15:00:00Z")) : Clock() {

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}

/** Registro por el endpoint viejo de /auth, que usa la app actual hasta I0-06. */
suspend fun HttpClient.register(
    email: String = "ana@example.com",
    password: String = "secreta123",
    name: String = "Ana Pérez",
): LegacyAuthResponse {
    val response = post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(LegacyRegisterRequest(name = name, email = email, password = password))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    return response.body()
}

suspend fun HttpClient.createChild(
    token: String,
    name: String = "Sofi",
    birthDate: String = "2026-06-15",
    sex: String = "F",
): ChildResponse {
    val response = post("/children") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(CreateChildRequest(name = name, birthDate = birthDate, sex = sex))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    return response.body()
}
