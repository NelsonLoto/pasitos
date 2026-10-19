package com.pasitos.backend.sync

import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.vaccination.OtherVaccine
import com.pasitos.backend.vaccination.OtherVaccineRepository
import com.pasitos.shared.api.sync.SyncOp
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * `other_vaccines` en la sincronización: solo lectura. Se escriben por
 * `/children/{id}/other-vaccines` (RNF-OFF-06, ADR-28): [apply] siempre rechaza.
 */
class OtherVaccineSyncEntity(private val otherVaccines: OtherVaccineRepository) : SyncEntity {

    override val name: String = "OTHER_VACCINE"

    override fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<EntityChange> =
        otherVaccines.changesSince(childIds, cursor, limit).map { EntityChange(it.syncVersion, it.toJson()) }

    override fun snapshot(childId: UUID): List<EntityChange> =
        otherVaccines.listActiveForChild(childId).map { EntityChange(it.syncVersion, it.toJson()) }

    override fun apply(caller: Caller, op: SyncOp, data: JsonElement): EntityChange =
        throw InvalidInputException("Las otras vacunas no se sincronizan por /sync/push: usá /children/{id}/other-vaccines")

    private fun OtherVaccine.toJson() = buildJsonObject {
        put("id", id.toString())
        put("childId", childId.toString())
        put("name", name)
        put("appliedOn", appliedOn.toString())
        lot?.let { put("lot", it) }
        note?.let { put("note", it) }
        put("authorAccountId", authorAccountId.toString())
        put("authorRole", authorRole.name)
        put("deleted", deletedAt != null)
    }
}
