package com.pasitos.backend.support

import com.pasitos.backend.auth.AuthResponse
import com.pasitos.backend.auth.RegisterRequest
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
import kotlin.test.assertEquals

/** Levanta el backend completo contra el PostgreSQL de prueba, con todas las tablas vacías. */
fun apiTest(block: suspend ApplicationTestBuilder.(client: HttpClient) -> Unit) = testApplication {
    TestDatabase.reset()
    environment { config = TestDatabase.config() }
    application { module() }
    val client = createClient {
        install(ContentNegotiation) { json() }
    }
    block(client)
}

suspend fun HttpClient.register(
    email: String = "ana@example.com",
    password: String = "secreta123",
    name: String = "Ana Pérez",
): AuthResponse {
    val response = post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequest(name = name, email = email, password = password))
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
