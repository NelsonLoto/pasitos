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
}
