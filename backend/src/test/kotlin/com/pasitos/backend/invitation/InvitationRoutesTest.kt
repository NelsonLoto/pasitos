package com.pasitos.backend.invitation

import com.pasitos.backend.support.TestClock
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.TestMailServer
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.TokenPair
import com.pasitos.shared.api.guardianship.AcceptInvitationRequest
import com.pasitos.shared.api.guardianship.InvitationResponse
import com.pasitos.shared.api.guardianship.InviteGuardianRequest
import com.pasitos.shared.api.guardianship.SentInvitationResponse
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `/me/invitations`, `/invitations/{id}/...` (I1-B5: RF-HIJ-05 CA3, RF-HIJ-06; RN-HIJ-07). */
class InvitationRoutesTest {

    @Test
    fun `RF-HIJ-06 CA1 acepta la invitación, queda como tutor secundario y el principal recibe el aviso`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val invitee = client.register(email = "beto@example.com")
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
        val invitationId = client.pendingInvitationId(invitee.tokens.accessToken)

        val response = client.post("/api/v1/invitations/$invitationId/accept") {
            bearerAuth(invitee.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(AcceptInvitationRequest(representationDeclared = true))
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(
            listOf("SECONDARY"),
            TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = '${child.id}' AND account_id = '${invitee.account.id}'"),
        )
        assertEquals(
            listOf("GUARDIAN_DECLARATION"),
            TestDatabase.column<String>("SELECT type FROM consents WHERE account_id = '${invitee.account.id}' AND child_id = '${child.id}'"),
        )
        assertNotNull(TestMailServer.awaitMessageTo("ana@example.com"))
    }

    @Test
    fun `aceptar sin declarar la representación responde 400`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val invitee = client.register(email = "beto@example.com")
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
        val invitationId = client.pendingInvitationId(invitee.tokens.accessToken)

        val response = client.post("/api/v1/invitations/$invitationId/accept") {
            bearerAuth(invitee.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(AcceptInvitationRequest(representationDeclared = false))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ErrorCode.INVALID_INPUT, response.body<ErrorResponse>().code)
    }

    @Test
    fun `RF-HIJ-06 CA2 rechaza la invitación y el principal recibe el aviso`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val invitee = client.register(email = "beto@example.com")
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
        val invitationId = client.pendingInvitationId(invitee.tokens.accessToken)

        val response = client.post("/api/v1/invitations/$invitationId/reject") { bearerAuth(invitee.tokens.accessToken) }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(emptyList(), TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = '${child.id}' AND account_id = '${invitee.account.id}'"))
        assertNotNull(TestMailServer.awaitMessageTo("ana@example.com"))
    }

    @Test
    fun `RN-HIJ-07 una invitación vencida no se puede aceptar ni rechazar`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            val principal = client.register(email = "ana@example.com")
            val child = client.createChild(principal.tokens.accessToken)
            val invitee = client.register(email = "beto@example.com")
            client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
            val invitationId = client.pendingInvitationId(invitee.tokens.accessToken)
            clock.advance(Duration.ofDays(16))
            // El access token también venció: lo renueva (sin exigir email verificado, a diferencia del login).
            val freshToken = client.post("/api/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken = invitee.tokens.refreshToken))
            }.body<TokenPair>().accessToken

            val response = client.post("/api/v1/invitations/$invitationId/accept") {
                bearerAuth(freshToken)
                contentType(ContentType.Application.Json)
                setBody(AcceptInvitationRequest(representationDeclared = true))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(ErrorCode.INVITATION_NOT_FOUND, response.body<ErrorResponse>().code)
        }
    }

    @Test
    fun `RF-HIJ-06 GET me invitations solo devuelve las vigentes del email de quien pregunta`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val invitee = client.register(email = "beto@example.com")
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")

        val response = client.get("/api/v1/me/invitations") { bearerAuth(invitee.tokens.accessToken) }
        val none = client.get("/api/v1/me/invitations") { bearerAuth(principal.tokens.accessToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        val invitations = response.body<List<InvitationResponse>>()
        assertEquals(1, invitations.size)
        assertEquals(child.id, invitations.single().childId)
        assertTrue(none.body<List<InvitationResponse>>().isEmpty())
    }

    @Test
    fun `RF-HIJ-05 CA3 reenviar cancela la invitación anterior y crea una nueva con un plazo nuevo`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        client.invite(principal.tokens.accessToken, child.id, "nueva@example.com")
        val originalId = TestDatabase.column<String>("SELECT id::text FROM invitations WHERE email = 'nueva@example.com'").single()

        val response = client.post("/api/v1/invitations/$originalId/resend") { bearerAuth(principal.tokens.accessToken) }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(
            listOf("CANCELLED"),
            TestDatabase.column<String>("SELECT status FROM invitations WHERE id = '$originalId'"),
        )
        assertEquals(
            listOf("PENDING"),
            TestDatabase.column<String>("SELECT status FROM invitations WHERE email = 'nueva@example.com' AND id <> '$originalId'"),
        )
    }

    @Test
    fun `RF-HIJ-05 CA3 el principal ve sus invitaciones pendientes, vigentes y vencidas, de la más nueva a la más vieja`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            val principal = client.register(email = "ana@example.com")
            val child = client.createChild(principal.tokens.accessToken)
            client.invite(principal.tokens.accessToken, child.id, "vieja@example.com")
            clock.advance(Duration.ofDays(16))
            val token = client.refreshed(principal.tokens)
            client.invite(token, child.id, "nueva@example.com")

            val response = client.get("/api/v1/children/${child.id}/guardian-invitations") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val sent = response.body<List<SentInvitationResponse>>()
            assertEquals(listOf("nueva@example.com" to false, "vieja@example.com" to true), sent.map { it.email to it.expired })
        }
    }

    @Test
    fun `las invitaciones aceptadas, rechazadas y canceladas no aparecen entre las enviadas`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val accepting = client.register(email = "beto@example.com")
        val rejecting = client.register(email = "cami@example.com")
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
        client.invite(principal.tokens.accessToken, child.id, "cami@example.com")
        client.invite(principal.tokens.accessToken, child.id, "dani@example.com")
        client.accept(accepting.tokens.accessToken, client.pendingInvitationId(accepting.tokens.accessToken))
        client.post("/api/v1/invitations/${client.pendingInvitationId(rejecting.tokens.accessToken)}/reject") {
            bearerAuth(rejecting.tokens.accessToken)
        }
        val cancelledId = TestDatabase.column<String>("SELECT id::text FROM invitations WHERE email = 'dani@example.com'").single()
        client.post("/api/v1/invitations/$cancelledId/resend") { bearerAuth(principal.tokens.accessToken) }

        val sent = client.get("/api/v1/children/${child.id}/guardian-invitations") {
            bearerAuth(principal.tokens.accessToken)
        }.body<List<SentInvitationResponse>>()

        assertEquals(listOf("dani@example.com"), sent.map { it.email })
        assertTrue(sent.single().id != cancelledId)
    }

    @Test
    fun `solo el tutor principal ve las invitaciones enviadas`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
        client.accept(secondary.tokens.accessToken, client.pendingInvitationId(secondary.tokens.accessToken))
        val stranger = client.register(email = "cami@example.com")

        val asSecondary = client.get("/api/v1/children/${child.id}/guardian-invitations") { bearerAuth(secondary.tokens.accessToken) }
        val asStranger = client.get("/api/v1/children/${child.id}/guardian-invitations") { bearerAuth(stranger.tokens.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, asSecondary.status)
        assertEquals(ErrorCode.FORBIDDEN, asSecondary.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.NotFound, asStranger.status)
    }

    @Test
    fun `reenviar una invitación aceptada o rechazada responde 404 y no manda email`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val accepting = client.register(email = "beto@example.com")
        val rejecting = client.register(email = "cami@example.com")
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
        client.invite(principal.tokens.accessToken, child.id, "cami@example.com")
        val acceptedId = client.pendingInvitationId(accepting.tokens.accessToken)
        val rejectedId = client.pendingInvitationId(rejecting.tokens.accessToken)
        client.accept(accepting.tokens.accessToken, acceptedId)
        client.post("/api/v1/invitations/$rejectedId/reject") { bearerAuth(rejecting.tokens.accessToken) }
        // Que no quede en la casilla el email de la invitación original y se confunda con uno nuevo
        assertNotNull(TestMailServer.awaitMessageTo("beto@example.com"))
        assertNotNull(TestMailServer.awaitMessageTo("cami@example.com"))
        TestMailServer.reset()

        val resendAccepted = client.post("/api/v1/invitations/$acceptedId/resend") { bearerAuth(principal.tokens.accessToken) }
        val resendRejected = client.post("/api/v1/invitations/$rejectedId/resend") { bearerAuth(principal.tokens.accessToken) }

        assertEquals(HttpStatusCode.NotFound, resendAccepted.status)
        assertEquals(ErrorCode.INVITATION_NOT_FOUND, resendAccepted.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.NotFound, resendRejected.status)
        assertEquals(
            listOf("ACCEPTED", "REJECTED"),
            TestDatabase.column<String>("SELECT status FROM invitations WHERE child_id = '${child.id}' ORDER BY email"),
        )
        assertNull(TestMailServer.awaitMessageTo("beto@example.com", Duration.ofSeconds(2)))
        assertNull(TestMailServer.awaitMessageTo("cami@example.com", Duration.ofSeconds(2)))
    }

    @Test
    fun `invitar de nuevo a un email con una invitación vencida la cancela`() {
        val clock = TestClock()
        apiTest(clock) { client ->
            val principal = client.register(email = "ana@example.com")
            val child = client.createChild(principal.tokens.accessToken)
            client.invite(principal.tokens.accessToken, child.id, "nueva@example.com")
            val expiredId = TestDatabase.column<String>("SELECT id::text FROM invitations WHERE email = 'nueva@example.com'").single()
            clock.advance(Duration.ofDays(16))

            val response = client.invite(client.refreshed(principal.tokens), child.id, "nueva@example.com")

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals(listOf("CANCELLED"), TestDatabase.column<String>("SELECT status FROM invitations WHERE id = '$expiredId'"))
            assertEquals(
                listOf("PENDING"),
                TestDatabase.column<String>("SELECT status FROM invitations WHERE email = 'nueva@example.com' AND id <> '$expiredId'"),
            )
        }
    }

    @Test
    fun `aceptar una invitación que no es de quien pide responde 404`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        client.invite(principal.tokens.accessToken, child.id, "beto@example.com")
        val invitationId = TestDatabase.column<String>("SELECT id::text FROM invitations WHERE email = 'beto@example.com'").single()
        val other = client.register(email = "cami@example.com")

        val response = client.post("/api/v1/invitations/$invitationId/accept") {
            bearerAuth(other.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(AcceptInvitationRequest(representationDeclared = true))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ErrorCode.INVITATION_NOT_FOUND, response.body<ErrorResponse>().code)
    }

    private suspend fun HttpClient.invite(token: String, childId: String, email: String): HttpResponse =
        post("/api/v1/children/$childId/guardian-invitations") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(InviteGuardianRequest(email = email))
        }

    private suspend fun HttpClient.pendingInvitationId(token: String): String =
        get("/api/v1/me/invitations") { bearerAuth(token) }.body<List<InvitationResponse>>().single().id

    private suspend fun HttpClient.accept(token: String, invitationId: String): HttpResponse =
        post("/api/v1/invitations/$invitationId/accept") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(AcceptInvitationRequest(representationDeclared = true))
        }

    /** Después de adelantar el reloj el access token venció: lo renueva con el refresh token. */
    private suspend fun HttpClient.refreshed(tokens: TokenPair): String =
        post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken = tokens.refreshToken))
        }.body<TokenPair>().accessToken
}
