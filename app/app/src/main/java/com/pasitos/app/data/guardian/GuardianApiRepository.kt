package com.pasitos.app.data.guardian

import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.shared.api.guardianship.GuardianResponse
import com.pasitos.shared.api.guardianship.GuardianshipRole
import com.pasitos.shared.api.guardianship.InviteGuardianRequest
import com.pasitos.shared.api.guardianship.SentInvitationResponse
import com.pasitos.shared.api.guardianship.TransferPrincipalRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

/** [GuardianRepository] contra `/api/v1/children/{id}/guardians`, `/invitations/{id}/resend` y vecinos (12 · API). */
@Singleton
class GuardianApiRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val syncScheduler: SyncScheduler,
) : GuardianRepository {

    override suspend fun guardians(childId: String): Result<List<Guardian>> = runCatching {
        send(HttpStatusCode.OK) { httpClient.get("/api/v1/children/$childId/guardians") }
            .body<List<GuardianResponse>>()
            .map { it.toGuardian() }
    }

    override suspend fun invite(childId: String, email: String): Result<Unit> = runCatching {
        send(HttpStatusCode.NoContent) {
            httpClient.post("/api/v1/children/$childId/guardian-invitations") {
                contentType(ContentType.Application.Json)
                setBody(InviteGuardianRequest(email = email.trim()))
            }
        }
    }

    override suspend fun sentInvitations(childId: String): Result<List<SentInvitation>> = runCatching {
        send(HttpStatusCode.OK) { httpClient.get("/api/v1/children/$childId/guardian-invitations") }
            .body<List<SentInvitationResponse>>()
            .map { SentInvitation(id = it.id, email = it.email, sentAt = it.sentAt, expiresAt = it.expiresAt, expired = it.expired) }
    }

    override suspend fun resend(invitationId: String): Result<Unit> = runCatching {
        send(HttpStatusCode.NoContent) { httpClient.post("/api/v1/invitations/$invitationId/resend") }
    }

    override suspend fun remove(childId: String, accountId: String): Result<Unit> = runCatching {
        send(HttpStatusCode.NoContent) { httpClient.delete("/api/v1/children/$childId/guardians/$accountId") }
        // Las tutorías del teléfono se actualizan con la sincronización
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
    }

    override suspend fun transferPrincipal(childId: String, accountId: String): Result<Unit> = runCatching {
        send(HttpStatusCode.NoContent) {
            httpClient.post("/api/v1/children/$childId/principal-transfer") {
                contentType(ContentType.Application.Json)
                setBody(TransferPrincipalRequest(accountId = accountId))
            }
        }
        // El rol propio que lee el perfil del niño llega con la sincronización
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
    }

    private suspend fun send(success: HttpStatusCode, request: suspend () -> HttpResponse): HttpResponse =
        withConnection {
            val response = request()
            if (response.status != success) throw response.toApiException()
            response
        }

    private fun GuardianResponse.toGuardian() = Guardian(
        accountId = accountId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        role = when (role) {
            GuardianshipRole.PRINCIPAL -> GuardianRole.PRINCIPAL
            GuardianshipRole.SECONDARY -> GuardianRole.SECONDARY
        },
    )
}
