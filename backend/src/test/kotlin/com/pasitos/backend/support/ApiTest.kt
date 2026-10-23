package com.pasitos.backend.support

import com.auth0.jwt.interfaces.RSAKeyProvider
import com.pasitos.backend.module
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.RegisterRequest
import com.pasitos.shared.api.child.ChildResponse
import com.pasitos.shared.api.child.CreateChildRequest
import com.pasitos.shared.api.child.Sex
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
 * [clock] la prueba controla la hora del backend: tokens, sesiones y bloqueos vencen según él. Los
 * ID tokens de Google se verifican con [googleKeys], las de [TestGoogle] salvo que la prueba pida
 * otras.
 */
fun apiTest(
    clock: Clock = Clock.systemUTC(),
    googleKeys: RSAKeyProvider = TestGoogle.keys,
    block: suspend ApplicationTestBuilder.(client: HttpClient) -> Unit,
) = testApplication {
    TestDatabase.reset()
    TestMailServer.reset()
    environment {
        config = TestDatabase.config().apply {
            TestMailServer.config().forEach { (path, value) -> put(path, value) }
            put("google.clientId", TestGoogle.CLIENT_ID)
        }
    }
    application { module(clock, googleKeys) }
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

/** Registra una cuenta de tutor; su sesión queda abierta. */
suspend fun HttpClient.register(
    email: String = "ana@example.com",
    password: String = "secreta123",
): AuthResponse {
    val response = post("/api/v1/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequest(firstName = "Ana", lastName = "Pérez", email = email, password = password, acceptTerms = true))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    return response.body()
}

suspend fun HttpClient.createChild(
    token: String,
    firstName: String = "Sofi",
    lastName: String = "Pérez",
    birthDate: String = "2026-06-15",
    sex: Sex = Sex.FEMALE,
    gestationalWeeks: Int? = null,
): ChildResponse {
    val response = post("/api/v1/children") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(
            CreateChildRequest(
                firstName = firstName,
                lastName = lastName,
                sex = sex,
                birthDate = birthDate,
                gestationalWeeks = gestationalWeeks,
                representationDeclared = true,
            ),
        )
    }
    assertEquals(HttpStatusCode.Created, response.status)
    return response.body()
}
