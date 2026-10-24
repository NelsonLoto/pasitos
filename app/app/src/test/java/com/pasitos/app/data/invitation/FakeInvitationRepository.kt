package com.pasitos.app.data.invitation

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow

/** Invitaciones recibidas en memoria: [refresh] publica [server] y cada respuesta se registra. */
class FakeInvitationRepository(var server: List<ReceivedInvitation> = emptyList()) : InvitationRepository {

    override val pending = MutableStateFlow<List<ReceivedInvitation>>(emptyList())

    var refreshResult: Result<Unit> = Result.success(Unit)
    var respondResult: Result<Unit> = Result.success(Unit)
    var refreshes = 0

    val accepted = mutableListOf<Pair<String, Boolean>>()
    val rejected = mutableListOf<String>()

    override suspend fun refresh(): Result<Unit> {
        refreshes++
        return refreshResult.onSuccess { pending.value = server }
    }

    override suspend fun accept(invitationId: String, representationDeclared: Boolean): Result<Unit> =
        respondResult.onSuccess {
            accepted += invitationId to representationDeclared
            drop(invitationId)
        }

    override suspend fun reject(invitationId: String): Result<Unit> =
        respondResult.onSuccess {
            rejected += invitationId
            drop(invitationId)
        }

    override fun clear() {
        pending.value = emptyList()
    }

    private fun drop(invitationId: String) {
        server = server.filterNot { it.id == invitationId }
        pending.value = pending.value.filterNot { it.id == invitationId }
    }

    companion object {
        fun invitation(id: String = "inv-1", childFirstName: String = "Tomi") = ReceivedInvitation(
            id = id,
            childId = "child-$id",
            childFirstName = childFirstName,
            childLastName = "García",
            inviterFirstName = "Ana",
            inviterLastName = "Pérez",
            sentAt = Instant.parse("2026-10-19T15:00:00Z"),
            // 02/11/2026 a las 22:30 en Argentina, cuando en UTC ya es 03/11
            expiresAt = Instant.parse("2026-11-03T01:30:00Z"),
        )
    }
}
