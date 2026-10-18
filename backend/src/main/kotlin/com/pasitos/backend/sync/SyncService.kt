package com.pasitos.backend.sync

import com.pasitos.backend.child.ChildNotFoundException
import com.pasitos.backend.common.ApiException
import com.pasitos.backend.common.BusinessRuleException
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.tx
import com.pasitos.shared.api.sync.SyncChangesResponse
import com.pasitos.shared.api.sync.SyncMutation
import com.pasitos.shared.api.sync.SyncMutationResult
import com.pasitos.shared.api.sync.SyncMutationStatus
import com.pasitos.shared.api.sync.SyncPushResponse
import com.pasitos.shared.api.sync.SyncSnapshotResponse
import java.util.UUID

private const val PAGE_LIMIT = 500

/**
 * Motor genérico de sincronización (14 · Sincronización, ADR-15). No conoce ninguna entidad en
 * particular: todo lo resuelve a través de las [SyncEntity] que estén en [registry]. En I0 el
 * registro está vacío en producción (`Dependencies.kt`); las pruebas registran la suya.
 */
class SyncService(
    private val registry: SyncRegistry,
    private val syncRepository: SyncRepository,
) {

    /** `GET /sync/changes` (14 · Sincronización §2). */
    suspend fun changes(caller: Caller, cursor: Long, patients: Set<UUID>?): SyncChangesResponse = tx {
        val guardianScope = syncRepository.scopeForGuardian(caller.accountId)
        // "patients" acota el alcance del tutor (uso real recién con el pediatra, I3) - si no
        // viene, el alcance es todos sus hijos.
        val scope = if (patients != null) guardianScope.intersect(patients) else guardianScope

        // Se pide un resultado más que el límite a cada entidad: si una sola entidad ya lo supera,
        // el total fusionado da más de PAGE_LIMIT y así se detecta hasMore sin contarlo mal.
        val fetched = registry.all
            .flatMap { entity -> entity.changesSince(scope, cursor, PAGE_LIMIT + 1).map { entity.name to it } }
            .sortedBy { (_, change) -> change.syncVersion }

        val page = fetched.take(PAGE_LIMIT)
        val newCursor = page.lastOrNull()?.second?.syncVersion ?: cursor

        SyncChangesResponse(
            cursor = newCursor,
            hasMore = fetched.size > PAGE_LIMIT,
            changes = page.groupBy({ it.first }, { it.second.json }),
            newChildIds = syncRepository.newChildIdsSince(caller.accountId, cursor).map { it.toString() },
            revokedChildIds = syncRepository.revokedChildIdsSince(caller.accountId, cursor).map { it.toString() },
        )
    }

    /** `GET /sync/snapshot/{childId}`: foto completa para un niño que entró al alcance (newChildIds). */
    suspend fun snapshot(caller: Caller, childId: UUID): SyncSnapshotResponse = tx {
        // 404, no 403: para quien no tiene acceso, el niño no existe (11 · Backend §4).
        if (childId !in syncRepository.scopeForGuardian(caller.accountId)) throw ChildNotFoundException()

        val rows = registry.all.flatMap { entity -> entity.snapshot(childId).map { entity.name to it } }
        SyncSnapshotResponse(
            cursor = rows.maxOfOrNull { (_, change) -> change.syncVersion } ?: 0,
            changes = rows.groupBy({ it.first }, { it.second.json }),
        )
    }

    /** `POST /sync/push`: cada mutación en su propia transacción, en el orden recibido (14 · Sincronización §3). */
    suspend fun push(caller: Caller, mutations: List<SyncMutation>): SyncPushResponse =
        SyncPushResponse(mutations.map { applyOne(caller, it) })

    private suspend fun applyOne(caller: Caller, mutation: SyncMutation): SyncMutationResult = try {
        val entity = registry.get(mutation.entity)
            ?: throw InvalidInputException("Entidad de sincronización desconocida: ${mutation.entity}")
        val change = tx { entity.apply(caller, mutation.op, mutation.data) }
        SyncMutationResult(mutation.mutationId, SyncMutationStatus.APPLIED, syncVersion = change.syncVersion)
    } catch (e: ApiException) {
        // Un rechazo de una mutación no aborta las demás del lote (14 · Sincronización §3).
        SyncMutationResult(
            mutation.mutationId,
            SyncMutationStatus.REJECTED,
            code = e.code,
            rule = (e as? BusinessRuleException)?.rule,
            message = e.message,
        )
    }
}
