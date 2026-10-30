package com.pasitos.app.data.guardian

import com.pasitos.app.data.child.model.GuardianRole
import java.time.Instant

/** Tutores en memoria: cada llamada se registra y los cambios se ven en la próxima [guardians]. */
class FakeGuardianRepository(
    var list: List<Guardian> = emptyList(),
    var sent: List<SentInvitation> = emptyList(),
) : GuardianRepository {

    var listResult: Result<Unit> = Result.success(Unit)
    var actionResult: Result<Unit> = Result.success(Unit)

    val calls = mutableListOf<String>()

    override suspend fun guardians(childId: String): Result<List<Guardian>> {
        calls += "guardians $childId"
        return listResult.map { list }
    }

    override suspend fun invite(childId: String, email: String): Result<Unit> =
        actionResult.onSuccess {
            calls += "invite $childId $email"
            sent = listOf(sentInvitation(id = "inv-${sent.size + 1}", email = email, expired = false)) + sent
        }

    override suspend fun sentInvitations(childId: String): Result<List<SentInvitation>> {
        calls += "sentInvitations $childId"
        return listResult.map { sent }
    }

    override suspend fun resend(invitationId: String): Result<Unit> =
        actionResult.onSuccess {
            calls += "resend $invitationId"
            sent = sent.map { if (it.id == invitationId) it.copy(expired = false) else it }
        }

    override suspend fun remove(childId: String, accountId: String): Result<Unit> =
        actionResult.onSuccess {
            calls += "remove $childId $accountId"
            list = list.filterNot { it.accountId == accountId }
        }

    override suspend fun transferPrincipal(childId: String, accountId: String): Result<Unit> =
        actionResult.onSuccess {
            calls += "transfer $childId $accountId"
            list = list.map { guardian ->
                guardian.copy(role = if (guardian.accountId == accountId) GuardianRole.PRINCIPAL else GuardianRole.SECONDARY)
            }
        }

    companion object {
        fun sentInvitation(id: String = "inv-1", email: String = "cami@pasitos.test", expired: Boolean = true) = SentInvitation(
            id = id,
            email = email,
            sentAt = Instant.parse("2026-10-01T15:00:00Z"),
            // 16/10/2026 a las 22:30 en Argentina, cuando en UTC ya es 17/10
            expiresAt = Instant.parse("2026-10-17T01:30:00Z"),
            expired = expired,
        )
    }
}
