package com.pasitos.backend.child

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.child.ChildResponse
import com.pasitos.shared.api.child.CreateChildRequest
import com.pasitos.shared.api.child.DeletionPreviewResponse
import com.pasitos.shared.api.child.PatchChildRequest
import com.pasitos.shared.api.child.Sex
import com.pasitos.shared.api.sync.SyncChangesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Rutas de `/api/v1/children` (I1-B4: RF-HIJ-01 a 03, 09; RN-HIJ-01 a 08, 10, 11). */
class ChildRoutesTest {

    @Test
    fun `RF-HIJ-01 CA1 el alta crea el perfil y deja al tutor como principal, con la declaración registrada`() = apiTest { client ->
        val token = client.register().tokens.accessToken

        val response = client.postChild(token, createRequest(gestationalWeeks = 38))

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<ChildResponse>()
        assertEquals("Sofi", body.firstName)
        assertEquals("Pérez", body.lastName)
        assertEquals(Sex.FEMALE, body.sex)
        assertEquals(38, body.gestationalWeeks)
        assertEquals(false, body.gestationalWeeksUnknown)
        val childId = UUID.fromString(body.id)
        assertEquals(listOf("PRINCIPAL"), TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = ?", childId))
        assertEquals(
            listOf("GUARDIAN_DECLARATION ${TestDatabase.DECLARATION_VERSION}"),
            TestDatabase.column<String>("SELECT type || ' ' || version FROM consents WHERE child_id = ?", childId),
        )
    }

    @Test
    fun `RF-HIJ-01 CA5 sin semanas de gestación el niño queda con No lo sé`() = apiTest { client ->
        val token = client.register().tokens.accessToken

        val response = client.postChild(token, createRequest(gestationalWeeks = null))

        val body = response.body<ChildResponse>()
        assertEquals(null, body.gestationalWeeks)
        assertEquals(true, body.gestationalWeeksUnknown)
    }

    @Test
    fun `RN-HIJ-01 y RN-HIJ-03 el alta exige fecha válida, menos de 15 años y semanas entre 22 y 42`() = apiTest { client ->
        val token = client.register().tokens.accessToken
        val today = ArgentinaClock().today()

        val invalid = listOf(
            createRequest(birthDate = today.plusDays(1).toString()),
            createRequest(birthDate = today.minusYears(15).toString()),
            createRequest(birthDate = "15/06/2026"),
            createRequest(gestationalWeeks = 21),
            createRequest(gestationalWeeks = 43),
            createRequest(firstName = " "),
            createRequest(representationDeclared = false),
        )

        invalid.forEach { request ->
            val response = client.postChild(token, request)
            assertEquals(HttpStatusCode.BadRequest, response.status, "Pedido: $request")
            assertEquals(ErrorCode.INVALID_INPUT, response.body<ErrorResponse>().code)
        }
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM children").single())
    }

    @Test
    fun `RF-HIJ-02 y RF-HIJ-03 PATCH actualiza los datos editables, incluidas alergias y antecedentes`() = apiTest { client ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token)

        val response = client.patch("/api/v1/children/${child.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                PatchChildRequest(
                    firstName = "Sofía",
                    lastName = "Gómez",
                    sex = Sex.FEMALE,
                    birthDate = "2026-05-01",
                    gestationalWeeks = 40,
                    allergies = "Polen",
                    relevantHistory = "Ninguno",
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<ChildResponse>()
        assertEquals("Sofía", body.firstName)
        assertEquals("2026-05-01", body.birthDate)
        assertEquals("Polen", body.allergies)
        assertEquals("Ninguno", body.relevantHistory)
    }

    @Test
    fun `RN-HIJ-10 alergias y antecedentes tienen un máximo de 280 caracteres`() = apiTest { client ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token)
        val tooLong = "a".repeat(281)

        val response = client.patch("/api/v1/children/${child.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(patchRequest(allergies = tooLong))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ErrorCode.INVALID_INPUT, response.body<ErrorResponse>().code)
    }

    @Test
    fun `RN-HIJ-11 el avatar es uno de los predefinidos o ninguno`() = apiTest { client ->
        val token = client.register().tokens.accessToken

        val withAvatar = client.postChild(token, createRequest(avatarKey = "rabbit"))
        val withoutAvatar = client.postChild(token, createRequest(avatarKey = null))
        val unknown = client.postChild(token, createRequest(avatarKey = "https://example.com/foto.jpg"))
        val patchUnknown = client.patch("/api/v1/children/${withAvatar.body<ChildResponse>().id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(patchRequest(avatarKey = "dragon"))
        }

        assertEquals("rabbit", withAvatar.body<ChildResponse>().avatarKey)
        assertEquals(null, withoutAvatar.body<ChildResponse>().avatarKey)
        assertEquals(HttpStatusCode.BadRequest, unknown.status)
        assertEquals(ErrorCode.INVALID_INPUT, unknown.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.BadRequest, patchUnknown.status)
        assertEquals(
            listOf("rabbit", "sin avatar"),
            TestDatabase.column<String>("SELECT coalesce(avatar_key, 'sin avatar') FROM children ORDER BY created_at"),
        )
    }

    @Test
    fun `matriz de permisos otro tutor recibe 404 en GET, PATCH y DELETE`() = apiTest { client ->
        val ana = client.register(email = "ana@example.com").tokens.accessToken
        val beto = client.register(email = "beto@example.com").tokens.accessToken
        val child = client.createChild(ana)

        val get = client.get("/api/v1/children/${child.id}") { bearerAuth(beto) }
        val patch = client.patch("/api/v1/children/${child.id}") {
            bearerAuth(beto)
            contentType(ContentType.Application.Json)
            setBody(patchRequest())
        }
        val delete = client.delete("/api/v1/children/${child.id}") { bearerAuth(beto) }
        val unknownId = client.get("/api/v1/children/${UUID.randomUUID()}") { bearerAuth(ana) }

        assertEquals(HttpStatusCode.NotFound, get.status)
        assertEquals(ErrorCode.CHILD_NOT_FOUND, get.body<ErrorResponse>().code)
        assertEquals(HttpStatusCode.NotFound, patch.status)
        assertEquals(HttpStatusCode.NotFound, delete.status)
        assertEquals(HttpStatusCode.NotFound, unknownId.status)
    }

    @Test
    fun `RF-HIJ-09 CA1 el único tutor elimina el perfil entero`() = apiTest { client ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token)

        val preview = client.get("/api/v1/children/${child.id}/deletion-preview") { bearerAuth(token) }
        val delete = client.delete("/api/v1/children/${child.id}") { bearerAuth(token) }
        val afterDelete = client.get("/api/v1/children/${child.id}") { bearerAuth(token) }

        assertEquals(true, preview.body<DeletionPreviewResponse>().onlyGuardian)
        assertEquals(HttpStatusCode.NoContent, delete.status)
        assertEquals(HttpStatusCode.NotFound, afterDelete.status)
        assertTrue(TestDatabase.column<Boolean>("SELECT deleted_at IS NOT NULL FROM children WHERE id = '${child.id}'").single())
    }

    @Test
    fun `RF-HIJ-09 CA2 con otros tutores, el perfil sigue existiendo y el rol principal pasa al secundario más antiguo`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com").tokens.accessToken
        val child = client.createChild(principal)
        val secondaryAccountId = client.register(email = "beto@example.com").account.id
        seedSecondaryGuardianship(child.id, secondaryAccountId)

        val preview = client.get("/api/v1/children/${child.id}/deletion-preview") { bearerAuth(principal) }
        val delete = client.delete("/api/v1/children/${child.id}") { bearerAuth(principal) }

        assertEquals(false, preview.body<DeletionPreviewResponse>().onlyGuardian)
        assertEquals(HttpStatusCode.NoContent, delete.status)
        assertEquals(
            listOf(false),
            TestDatabase.column<Boolean>("SELECT deleted_at IS NOT NULL FROM children WHERE id = '${child.id}'"),
        )
        assertEquals(
            listOf("PRINCIPAL"),
            TestDatabase.column<String>(
                "SELECT role FROM guardianships WHERE child_id = '${child.id}' AND account_id = '$secondaryAccountId' AND deleted_at IS NULL",
            ),
        )
    }

    @Test
    fun `un tutor secundario no puede eliminar el perfil para todos, solo deja de ser tutor`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com").tokens.accessToken
        val child = client.createChild(principal)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        val delete = client.delete("/api/v1/children/${child.id}") { bearerAuth(secondary.tokens.accessToken) }

        assertEquals(HttpStatusCode.NoContent, delete.status)
        // El perfil sigue entero para el principal: no se eliminó para todos.
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/children/${child.id}") { bearerAuth(principal) }.status)
        assertEquals(
            listOf("PRINCIPAL"),
            TestDatabase.column<String>("SELECT role FROM guardianships WHERE child_id = '${child.id}' AND deleted_at IS NULL"),
        )
    }

    @Test
    fun `children y guardianships llegan por sincronización`() = apiTest { client ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token)

        val response = client.get("/api/v1/sync/changes?cursor=0") { bearerAuth(token) }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"CHILD\""), "No llegó CHILD en /sync/changes: $body")
        assertTrue(body.contains("\"GUARDIANSHIP\""), "No llegó GUARDIANSHIP en /sync/changes: $body")
        assertTrue(body.contains(child.id))
    }

    @Test
    fun `RF-HIJ-09 los otros teléfonos del único tutor reciben el niño en revokedChildIds`() = apiTest { client ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token)
        // Otro teléfono de la misma cuenta, que ya sincronizó al niño
        val cursor = client.syncChanges(token, cursor = 0).cursor

        client.delete("/api/v1/children/${child.id}") { bearerAuth(token) }

        assertEquals(listOf(child.id), client.syncChanges(token, cursor).revokedChildIds)
    }

    @Test
    fun `RF-HIJ-09 quien deja de ser tutor recibe el niño en revokedChildIds y los demás tutores no`() = apiTest { client ->
        val principal = client.register(email = "ana@example.com").tokens.accessToken
        val child = client.createChild(principal)
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)
        val secondaryCursor = client.syncChanges(secondary.tokens.accessToken, cursor = 0).cursor
        val principalCursor = client.syncChanges(principal, cursor = 0).cursor

        client.delete("/api/v1/children/${child.id}") { bearerAuth(secondary.tokens.accessToken) }

        assertEquals(listOf(child.id), client.syncChanges(secondary.tokens.accessToken, secondaryCursor).revokedChildIds)
        assertEquals(emptyList(), client.syncChanges(principal, principalCursor).revokedChildIds)
    }

    private suspend fun HttpClient.syncChanges(token: String, cursor: Long): SyncChangesResponse =
        get("/api/v1/sync/changes?cursor=$cursor") { bearerAuth(token) }.body()

    private fun seedSecondaryGuardianship(childId: String, accountId: String) {
        TestDatabase.connect().use {
            it.createStatement().execute(
                "INSERT INTO guardianships (child_id, account_id, role) VALUES ('$childId', '$accountId', 'SECONDARY')",
            )
        }
    }

    private fun createRequest(
        firstName: String = "Sofi",
        lastName: String = "Pérez",
        sex: Sex = Sex.FEMALE,
        birthDate: String = "2026-06-15",
        gestationalWeeks: Int? = 38,
        representationDeclared: Boolean = true,
        avatarKey: String? = null,
    ) = CreateChildRequest(
        firstName = firstName,
        lastName = lastName,
        sex = sex,
        birthDate = birthDate,
        gestationalWeeks = gestationalWeeks,
        avatarKey = avatarKey,
        representationDeclared = representationDeclared,
    )

    private fun patchRequest(allergies: String? = null, avatarKey: String? = null) = PatchChildRequest(
        firstName = "Sofi",
        lastName = "Pérez",
        sex = Sex.FEMALE,
        birthDate = "2026-06-15",
        gestationalWeeks = 38,
        allergies = allergies,
        avatarKey = avatarKey,
    )

    private suspend fun HttpClient.postChild(token: String, request: CreateChildRequest): HttpResponse = post("/api/v1/children") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(request)
    }
}
