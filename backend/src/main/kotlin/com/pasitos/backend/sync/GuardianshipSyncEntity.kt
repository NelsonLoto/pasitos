package com.pasitos.backend.sync

import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.guardianship.Guardianship
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.shared.api.sync.SyncOp
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * `guardianships` en la sincronización: solo lectura. Se escriben por los endpoints de
 * `/children/{id}/guardians` (I1-B5) y de `/children` (ADR-28): [apply] siempre rechaza.
 */
class GuardianshipSyncEntity(private val guardianships: GuardianshipRepository) : SyncEntity {

    override val name: String = "GUARDIANSHIP"

    override fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<EntityChange> =
        guardianships.changesSince(childIds, cursor, limit).map { EntityChange(it.syncVersion, it.toJson()) }

    override fun snapshot(childId: UUID): List<EntityChange> =
        guardianships.listForChild(childId).map { EntityChange(it.syncVersion, it.toJson()) }

    override fun apply(caller: Caller, op: SyncOp, data: JsonElement): EntityChange =
        throw InvalidInputException("Los tutores no se sincronizan por /sync/push")

    private fun Guardianship.toJson() = buildJsonObject {
        put("childId", childId.toString())
        put("accountId", accountId.toString())
        put("role", role.name)
        put("deleted", deletedAt != null)
    }
}
