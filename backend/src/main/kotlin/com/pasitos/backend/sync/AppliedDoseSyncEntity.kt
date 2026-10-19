package com.pasitos.backend.sync

import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.vaccination.AppliedDose
import com.pasitos.backend.vaccination.AppliedDoseRepository
import com.pasitos.shared.api.sync.SyncOp
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * `applied_doses` en la sincronización: solo lectura. Las dosis se escriben por
 * `/children/{id}/applied-doses` porque requieren conexión (RNF-OFF-06, ADR-28): [apply] siempre
 * rechaza.
 */
class AppliedDoseSyncEntity(private val appliedDoses: AppliedDoseRepository) : SyncEntity {

    override val name: String = "APPLIED_DOSE"

    override fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<EntityChange> =
        appliedDoses.changesSince(childIds, cursor, limit).map { EntityChange(it.syncVersion, it.toJson()) }

    override fun snapshot(childId: UUID): List<EntityChange> =
        appliedDoses.listActiveForChild(childId).map { EntityChange(it.syncVersion, it.toJson()) }

    override fun apply(caller: Caller, op: SyncOp, data: JsonElement): EntityChange =
        throw InvalidInputException("Las dosis no se sincronizan por /sync/push: usá /children/{id}/applied-doses")

    private fun AppliedDose.toJson() = buildJsonObject {
        put("id", id.toString())
        put("childId", childId.toString())
        put("doseCode", doseCode)
        appliedOn?.let { put("appliedOn", it.toString()) }
        lot?.let { put("lot", it) }
        place?.let { put("place", it) }
        put("authorAccountId", authorAccountId.toString())
        put("authorRole", authorRole.name)
        put("deleted", deletedAt != null)
    }
}
