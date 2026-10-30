package com.pasitos.app.data.invitation

import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.shared.api.guardianship.AcceptInvitationRequest
import com.pasitos.shared.api.guardianship.InvitationResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** [InvitationRepository] contra `/api/v1/me/invitations` e `/invitations/{id}` (12 · API). */
@Singleton
class InvitationApiRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val syncScheduler: SyncScheduler,
) : InvitationRepository {

    private val _pending = MutableStateFlow<List<ReceivedInvitation>>(emptyList())
    override val pending: StateFlow<List<ReceivedInvitation>> = _pending.asStateFlow()

    override suspend fun refresh(): Result<Unit> = runCatching {
        _pending.value = send(HttpStatusCode.OK) { httpClient.get("/api/v1/me/invitations") }
            .body<List<InvitationResponse>>()
            .map { it.toInvitation() }
    }

    override suspend fun accept(invitationId: String, representationDeclared: Boolean): Result<Unit> = runCatching {
        send(HttpStatusCode.NoContent) {
            httpClient.post("/api/v1/invitations/$invitationId/accept") {
                contentType(ContentType.Application.Json)
                setBody(AcceptInvitationRequest(representationDeclared = representationDeclared))
            }
        }
        _pending.update { list -> list.filterNot { it.id == invitationId } }
        // Trae al niño ahora, sin esperar la sincronización periódica
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
    }

    override suspend fun reject(invitationId: String): Result<Unit> = runCatching {
        send(HttpStatusCode.NoContent) { httpClient.post("/api/v1/invitations/$invitationId/reject") }
        _pending.update { list -> list.filterNot { it.id == invitationId } }
    }

    override fun clear() {
        _pending.value = emptyList()
    }

    private suspend fun send(success: HttpStatusCode, request: suspend () -> HttpResponse): HttpResponse =
        withConnection {
            val response = request()
            if (response.status != success) throw response.toApiException()
            response
        }

    private fun InvitationResponse.toInvitation() = ReceivedInvitation(
        id = id,
        childId = childId,
        childFirstName = childFirstName,
        childLastName = childLastName,
        inviterFirstName = inviterFirstName,
        inviterLastName = inviterLastName,
        sentAt = sentAt,
        expiresAt = expiresAt,
    )
}
