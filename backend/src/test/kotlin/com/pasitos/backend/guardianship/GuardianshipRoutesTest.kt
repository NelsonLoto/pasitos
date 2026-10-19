package com.pasitos.backend.guardianship

import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.TestMailServer
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.guardianship.GuardianResponse
import com.pasitos.shared.api.guardianship.InviteGuardianRequest
import com.pasitos.shared.api.guardianship.TransferPrincipalRequest
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** `/children/{id}/guardians`, `/guardian-invitations`, `/principal-transfer` (I1-B5: RF-HIJ-05, 07, 08, 09). */
class GuardianshipRoutesTest {

    @Test
    fun `RF-HIJ-07 cualquier tutor ve la lista, con el principal primero`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val response = client.get("/api/v1/children/${child.id}/guardians") { bearerAuth(secondary.tokens.accessToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        val guardians = response.body<List<GuardianResponse>>()
        assertEquals(listOf("PRINCIPAL", "SECONDARY"), guardians.map { it.role.name })
        assertEquals(principal.account.id, guardians[0].accountId)
    }

    @Test
    fun `RF-HIJ-05 CA1 el principal invita a un email con cuenta y le llega el mail`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        client.register(email = "beto@example.com")

        val response = client.invite(principal.tokens.accessToken, child.id, "beto@example.com")

        assertEquals(HttpStatusCode.NoContent, response.status)
        val mail = TestMailServer.awaitMessageTo("beto@example.com")
        assertNotNull(mail, "No le llegó la invitación a beto@example.com")
        assertEquals(listOf("GUARDIAN"), TestDatabase.column<String>("SELECT type FROM invitations WHERE email = 'beto@example.com'"))
    }

    @Test
    fun `RF-HIJ-05 CA2 el principal invita a un email sin cuenta y le llega el mail con la invitación a registrarse`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)

        val response = client.invite(principal.tokens.accessToken, child.id, "nueva@example.com")

        assertEquals(HttpStatusCode.NoContent, response.status)
        val mail = TestMailServer.awaitMessageTo("nueva@example.com")
        assertNotNull(mail)
        assertEquals("ana te invitó a pasitos", mail.subject.lowercase())
    }

    @Test
    fun `RF-HIJ-05 CA4 no se puede invitar a quien ya es tutor`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val response = client.invite(principal.tokens.accessToken, child.id, "beto@example.com")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(ErrorCode.ALREADY_GUARDIAN, response.body<ErrorResponse>().code)
    }

    @Test
    fun `no se puede invitar dos veces al mismo email mientras la invitación esté vigente`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)

        client.invite(principal.tokens.accessToken, child.id, "nueva@example.com")
        val second = client.invite(principal.tokens.accessToken, child.id, "nueva@example.com")

        assertEquals(HttpStatusCode.Conflict, second.status)
        assertEquals(ErrorCode.INVITATION_ALREADY_PENDING, second.body<ErrorResponse>().code)
    }

    @Test
    fun `solo el tutor principal puede invitar`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val response = client.invite(secondary.tokens.accessToken, child.id, "nueva@example.com")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ErrorCode.FORBIDDEN, response.body<ErrorResponse>().code)
    }

    @Test
    fun `RF-HIJ-09 un tutor secundario puede abandonar y el principal recibe el aviso`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val response = client.delete("/api/v1/children/${child.id}/guardians/${secondary.account.id}") {
            bearerAuth(secondary.tokens.accessToken)
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNotNull(TestMailServer.awaitMessageTo("ana@example.com"))
        assertEquals(
            listOf("PRINCIPAL"),
            TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = '${child.id}' AND deleted_at IS NULL"),
        )
    }

    @Test
    fun `RF-HIJ-09 el principal saca a un tutor secundario y este recibe el aviso`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val response = client.delete("/api/v1/children/${child.id}/guardians/${secondary.account.id}") {
            bearerAuth(principal.tokens.accessToken)
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNotNull(TestMailServer.awaitMessageTo("beto@example.com"))
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/children/${child.id}") { bearerAuth(secondary.tokens.accessToken) }.status)
    }

    @Test
    fun `un tutor secundario no puede sacar a otro tutor`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)
        val other = client.register(email = "cami@example.com")
        seedSecondaryGuardianship(child.id, other.account.id)

        val response = client.delete("/api/v1/children/${child.id}/guardians/${other.account.id}") {
            bearerAuth(secondary.tokens.accessToken)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `RF-HIJ-08 el principal cede su rol a otro tutor y ambos reciben el aviso`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val response = client.post("/api/v1/children/${child.id}/principal-transfer") {
            bearerAuth(principal.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(TransferPrincipalRequest(accountId = secondary.account.id))
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNotNull(TestMailServer.awaitMessageTo("ana@example.com"))
        assertNotNull(TestMailServer.awaitMessageTo("beto@example.com"))
        assertEquals(
            listOf("SECONDARY"),
            TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = '${child.id}' AND account_id = '${principal.account.id}'"),
        )
        assertEquals(
            listOf("PRINCIPAL"),
            TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = '${child.id}' AND account_id = '${secondary.account.id}'"),
        )
    }

    @Test
    fun `no se puede ceder el rol principal a quien no es tutor del niño`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)

        val response = client.post("/api/v1/children/${child.id}/principal-transfer") {
            bearerAuth(principal.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(TransferPrincipalRequest(accountId = UUID.randomUUID().toString()))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `solo el principal puede ceder el rol principal`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com")
        val child = client.createChild(principal.tokens.accessToken)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val response = client.post("/api/v1/children/${child.id}/principal-transfer") {
            bearerAuth(secondary.tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(TransferPrincipalRequest(accountId = principal.account.id))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun seedSecondaryGuardianship(childId: String, accountId: String) {
        TestDatabase.connect().use {
            it.createStatement().execute(
                "INSERT INTO guardianships (child_id, account_id, role) VALUES ('$childId', '$accountId', 'SECONDARY')",
            )
        }
    }

    private suspend fun io.ktor.client.HttpClient.invite(token: String, childId: String, email: String) =
        post("/api/v1/children/$childId/guardian-invitations") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(InviteGuardianRequest(email = email))
        }
}
