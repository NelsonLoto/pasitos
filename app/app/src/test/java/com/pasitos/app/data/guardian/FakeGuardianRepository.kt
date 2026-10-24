package com.pasitos.app.data.guardian

import com.pasitos.app.data.child.model.GuardianRole

/** Tutores en memoria: cada llamada se registra y los cambios se ven en la próxima [guardians]. */
class FakeGuardianRepository(var list: List<Guardian> = emptyList()) : GuardianRepository {

    var listResult: Result<Unit> = Result.success(Unit)
    var actionResult: Result<Unit> = Result.success(Unit)

    val calls = mutableListOf<String>()

    override suspend fun guardians(childId: String): Result<List<Guardian>> {
        calls += "guardians $childId"
        return listResult.map { list }
    }

    override suspend fun invite(childId: String, email: String): Result<Unit> =
        actionResult.onSuccess { calls += "invite $childId $email" }

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
}
