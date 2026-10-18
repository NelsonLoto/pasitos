package com.pasitos.backend.sync

import com.pasitos.backend.child.Child
import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.shared.api.sync.SyncOp
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * `children` en la sincronización: solo lectura (`GET /sync/changes`, `GET /sync/snapshot`). Los
 * hijos se escriben por `/children` (ADR-28, 12 · API §Regla de escritura): [apply] siempre
 * rechaza.
 */
class ChildSyncEntity(private val children: ChildRepository) : SyncEntity {

    override val name: String = "CHILD"

    override fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<EntityChange> =
        children.changesSince(childIds, cursor, limit).map { EntityChange(it.syncVersion, it.toJson()) }

    override fun snapshot(childId: UUID): List<EntityChange> =
        children.findById(childId)
            ?.takeIf { it.deletedAt == null }
            ?.let { listOf(EntityChange(it.syncVersion, it.toJson())) }
            ?: emptyList()

    override fun apply(caller: Caller, op: SyncOp, data: JsonElement): EntityChange =
        throw InvalidInputException("Los hijos no se sincronizan por /sync/push: usá POST o PATCH /children")

    private fun Child.toJson() = buildJsonObject {
        put("id", id.toString())
        put("firstName", firstName)
        put("lastName", lastName)
        put("sex", sex.name)
        put("birthDate", birthDate.toString())
        gestationalWeeks?.let { put("gestationalWeeks", it) }
        put("gestationalWeeksUnknown", gestationalWeeksUnknown)
        birthWeightKg?.let { put("birthWeightKg", it.toDouble()) }
        birthLengthCm?.let { put("birthLengthCm", it.toDouble()) }
        birthHeadCircumferenceCm?.let { put("birthHeadCircumferenceCm", it.toDouble()) }
        allergies?.let { put("allergies", it) }
        relevantHistory?.let { put("relevantHistory", it) }
        avatarKey?.let { put("avatarKey", it) }
        put("status", status.name)
        put("deleted", deletedAt != null)
    }
}
