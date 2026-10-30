package com.pasitos.app.data.guardian

import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.FakeServer
import com.pasitos.app.core.network.FakeServer.Companion.bodyText
import com.pasitos.app.core.network.FakeServer.Companion.error
import com.pasitos.app.core.network.FakeServer.Companion.json
import com.pasitos.app.core.network.createHttpClient
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.data.child.ChildTestData.SOFIA
import com.pasitos.app.data.child.FixedSessionTokens
import com.pasitos.app.data.child.RecordingSyncScheduler
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.guardianship.GuardianResponse
import com.pasitos.shared.api.guardianship.GuardianshipRole
import com.pasitos.shared.api.guardianship.InviteGuardianRequest
import com.pasitos.shared.api.guardianship.SentInvitationResponse
import com.pasitos.shared.api.guardianship.TransferPrincipalRequest
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

/** `/api/v1/children/{id}/guardians` y vecinos desde la app (RF-HIJ-05, 07, 08). */
@RunWith(RobolectricTestRunner::class)
class GuardianApiRepositoryTest {

    private val server = FakeServer(validAccessToken = "access-1")
    private val scheduler = RecordingSyncScheduler()
    private val repository = GuardianApiRepository(createHttpClient(server.engine, "http://localhost", FixedSessionTokens()), scheduler)

    @Test
    fun `trae los tutores con su rol`() = runTest {
        server.routes["/api/v1/children/$SOFIA/guardians"] = {
            json(
                Json.encodeToString(
                    listOf(
                        GuardianResponse("a", "Ana", "Pérez", "ana@pasitos.test", GuardianshipRole.PRINCIPAL),
                        GuardianResponse("b", "Beto", "Gómez", "beto@pasitos.test", GuardianshipRole.SECONDARY),
                    ),
                ),
            )
        }

        val guardians = repository.guardians(SOFIA).getOrThrow()

        assertEquals(listOf(GuardianRole.PRINCIPAL, GuardianRole.SECONDARY), guardians.map { it.role })
        assertEquals("Beto Gómez", guardians[1].fullName)
    }

    @Test
    fun `RF-HIJ-05 invitar manda el email sin espacios`() = runTest {
        server.routes["/api/v1/children/$SOFIA/guardian-invitations"] = { respond("", HttpStatusCode.NoContent) }

        repository.invite(SOFIA, "  cami@pasitos.test ").getOrThrow()

        val sent = Json.decodeFromString<InviteGuardianRequest>(server.requestsTo("/api/v1/children/$SOFIA/guardian-invitations").single().bodyText())
        assertEquals("cami@pasitos.test", sent.email)
        assertTrue(scheduler.requests.isEmpty())
    }

    @Test
    fun `RF-HIJ-05 CA4 si ya es tutor devuelve el mensaje del servidor`() = runTest {
        server.routes["/api/v1/children/$SOFIA/guardian-invitations"] = {
            error(HttpStatusCode.Conflict, ErrorCode.ALREADY_GUARDIAN, "Ese email ya es tutor de este hijo/a")
        }

        val error = repository.invite(SOFIA, "beto@pasitos.test").exceptionOrNull() as ApiException

        assertEquals(ErrorCode.ALREADY_GUARDIAN, error.code)
        assertEquals("Ese email ya es tutor de este hijo/a", error.message)
    }

    @Test
    fun `RF-HIJ-05 CA3 trae las invitaciones enviadas con el vencimiento que calculó el servidor`() = runTest {
        server.routes["/api/v1/children/$SOFIA/guardian-invitations"] = { request ->
            assertEquals(HttpMethod.Get, request.method)
            json(
                Json.encodeToString(
                    listOf(
                        SentInvitationResponse("inv-2", "nueva@pasitos.test", Instant.parse("2026-10-20T15:00:00Z"), Instant.parse("2026-11-04T15:00:00Z"), expired = false),
                        SentInvitationResponse("inv-1", "vieja@pasitos.test", Instant.parse("2026-10-01T15:00:00Z"), Instant.parse("2026-10-16T15:00:00Z"), expired = true),
                    ),
                ),
            )
        }

        val sent = repository.sentInvitations(SOFIA).getOrThrow()

        assertEquals(listOf("nueva@pasitos.test" to false, "vieja@pasitos.test" to true), sent.map { it.email to it.expired })
    }

    @Test
    fun `RF-HIJ-05 CA3 reenviar usa el id de la invitación`() = runTest {
        server.routes["/api/v1/invitations/inv-1/resend"] = { request ->
            assertEquals(HttpMethod.Post, request.method)
            respond("", HttpStatusCode.NoContent)
        }

        repository.resend("inv-1").getOrThrow()

        assertEquals(1, server.requestsTo("/api/v1/invitations/inv-1/resend").size)
    }

    @Test
    fun `RF-HIJ-07 CA1 quitar usa DELETE y pide sincronizar las tutorías`() = runTest {
        server.routes["/api/v1/children/$SOFIA/guardians/b"] = { request ->
            assertEquals(HttpMethod.Delete, request.method)
            respond("", HttpStatusCode.NoContent)
        }

        repository.remove(SOFIA, "b").getOrThrow()

        assertEquals(listOf(SyncTrigger.LOCAL_CHANGE), scheduler.requests)
    }

    @Test
    fun `RF-HIJ-08 ceder el rol manda a quién y pide sincronizar`() = runTest {
        server.routes["/api/v1/children/$SOFIA/principal-transfer"] = { respond("", HttpStatusCode.NoContent) }

        repository.transferPrincipal(SOFIA, "b").getOrThrow()

        val sent = Json.decodeFromString<TransferPrincipalRequest>(server.requestsTo("/api/v1/children/$SOFIA/principal-transfer").single().bodyText())
        assertEquals("b", sent.accountId)
        assertEquals(listOf(SyncTrigger.LOCAL_CHANGE), scheduler.requests)
    }
}
