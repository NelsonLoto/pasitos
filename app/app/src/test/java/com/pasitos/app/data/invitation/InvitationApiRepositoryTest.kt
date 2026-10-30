package com.pasitos.app.data.invitation

import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.FakeServer
import com.pasitos.app.core.network.FakeServer.Companion.bodyText
import com.pasitos.app.core.network.FakeServer.Companion.error
import com.pasitos.app.core.network.FakeServer.Companion.json
import com.pasitos.app.core.network.createHttpClient
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.data.child.FixedSessionTokens
import com.pasitos.app.data.child.RecordingSyncScheduler
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.guardianship.AcceptInvitationRequest
import com.pasitos.shared.api.guardianship.InvitationResponse
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** `/api/v1/me/invitations` e `/invitations/{id}` desde la app (RF-HIJ-06). */
@RunWith(RobolectricTestRunner::class)
class InvitationApiRepositoryTest {

    private val server = FakeServer(validAccessToken = "access-1")
    private val scheduler = RecordingSyncScheduler()
    private val repository = InvitationApiRepository(createHttpClient(server.engine, "http://localhost", FixedSessionTokens()), scheduler)

    private val response = InvitationResponse(
        id = "inv-1",
        childId = "child-1",
        childFirstName = "Tomi",
        childLastName = "García",
        inviterFirstName = "Ana",
        inviterLastName = "Pérez",
        sentAt = Instant.parse("2026-10-19T15:00:00Z"),
        expiresAt = Instant.parse("2026-11-03T15:00:00Z"),
    )

    @Test
    fun `RF-HIJ-06 refresh publica las invitaciones vigentes`() = runTest {
        server.routes["/api/v1/me/invitations"] = { json(Json.encodeToString(listOf(response))) }

        repository.refresh().getOrThrow()

        val pending = repository.pending.value.single()
        assertEquals("inv-1", pending.id)
        assertEquals("Tomi", pending.childFirstName)
        assertEquals(Instant.parse("2026-11-03T15:00:00Z"), pending.expiresAt)
    }

    @Test
    fun `RF-HIJ-06 CA1 aceptar manda la declaración, la saca de la lista y pide sincronizar para traer al niño`() = runTest {
        server.routes["/api/v1/me/invitations"] = { json(Json.encodeToString(listOf(response))) }
        server.routes["/api/v1/invitations/inv-1/accept"] = { respond("", HttpStatusCode.NoContent) }
        repository.refresh().getOrThrow()

        repository.accept("inv-1", representationDeclared = true).getOrThrow()

        val sent = Json.decodeFromString<AcceptInvitationRequest>(server.requestsTo("/api/v1/invitations/inv-1/accept").single().bodyText())
        assertTrue(sent.representationDeclared)
        assertTrue(repository.pending.value.isEmpty())
        assertEquals(listOf(SyncTrigger.LOCAL_CHANGE), scheduler.requests)
    }

    @Test
    fun `RF-HIJ-06 CA2 rechazar la saca de la lista sin sincronizar`() = runTest {
        server.routes["/api/v1/me/invitations"] = { json(Json.encodeToString(listOf(response))) }
        server.routes["/api/v1/invitations/inv-1/reject"] = { request ->
            assertEquals(HttpMethod.Post, request.method)
            respond("", HttpStatusCode.NoContent)
        }
        repository.refresh().getOrThrow()

        repository.reject("inv-1").getOrThrow()

        assertTrue(repository.pending.value.isEmpty())
        assertTrue(scheduler.requests.isEmpty())
    }

    @Test
    fun `RF-HIJ-06 CA3 una invitación vencida devuelve el código y el mensaje del servidor y sigue en la lista`() = runTest {
        server.routes["/api/v1/me/invitations"] = { json(Json.encodeToString(listOf(response))) }
        server.routes["/api/v1/invitations/inv-1/accept"] = {
            error(HttpStatusCode.NotFound, ErrorCode.INVITATION_NOT_FOUND, "Esa invitación venció. Pedí que te inviten de nuevo")
        }
        repository.refresh().getOrThrow()

        val error = repository.accept("inv-1", representationDeclared = true).exceptionOrNull() as ApiException

        assertEquals(ErrorCode.INVITATION_NOT_FOUND, error.code)
        assertEquals("Esa invitación venció. Pedí que te inviten de nuevo", error.message)
        assertEquals(listOf("inv-1"), repository.pending.value.map { it.id })
    }

    @Test
    fun `al cerrar sesión se vacía la lista`() = runTest {
        server.routes["/api/v1/me/invitations"] = { json(Json.encodeToString(listOf(response))) }
        repository.refresh().getOrThrow()

        repository.clear()

        assertTrue(repository.pending.value.isEmpty())
    }
}
