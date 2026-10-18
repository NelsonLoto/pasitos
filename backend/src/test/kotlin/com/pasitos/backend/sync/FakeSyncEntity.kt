package com.pasitos.backend.sync

import com.pasitos.backend.common.BusinessRuleException
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.NotFoundException
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.sync.SyncOp
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * [SyncEntity] de prueba para I0-08, ajena a cualquier tabla real: solo prueba el motor genérico
 * de sincronización, nunca se registra en `Dependencies.kt` de producción. Guarda todo en memoria
 * -alcanza para una corrida de prueba-, no en PostgreSQL.
 *
 * Regla de prueba: un `UPSERT` con `value = "REJECT"` se rechaza siempre, para poder probar el
 * camino de rechazo con código y regla sin depender de ninguna regla de negocio real.
 */
class FakeSyncEntity(override val name: String = "TEST_ENTITY") : SyncEntity {

    private data class Row(val id: UUID, val childId: UUID, val value: String, val deleted: Boolean, val syncVersion: Long)

    private val rows = mutableMapOf<UUID, Row>()
    private var counter = 0L

    override fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<EntityChange> =
        rows.values
            .filter { it.childId in childIds && it.syncVersion > cursor }
            .sortedBy { it.syncVersion }
            .take(limit)
            .map { EntityChange(it.syncVersion, it.toJson()) }

    override fun snapshot(childId: UUID): List<EntityChange> =
        rows.values
            .filter { it.childId == childId && !it.deleted }
            .map { EntityChange(it.syncVersion, it.toJson()) }

    override fun apply(caller: Caller, op: SyncOp, data: JsonElement): EntityChange {
        val obj = data.jsonObject
        val id = UUID.fromString(obj.getValue("id").jsonPrimitive.content)
        return when (op) {
            SyncOp.UPSERT -> {
                val childId = UUID.fromString(obj.getValue("childId").jsonPrimitive.content)
                val value = obj["value"]?.jsonPrimitive?.content.orEmpty()
                if (value == "REJECT") throw BusinessRuleException("RN-TEST-01", "Rechazado a propósito para la prueba")
                val row = Row(id, childId, value, deleted = false, syncVersion = ++counter)
                rows[id] = row
                EntityChange(row.syncVersion, row.toJson())
            }
            SyncOp.DELETE -> {
                // Idempotente (ADR-15): borrar algo ya borrado no falla, solo registra una
                // versión nueva de la misma lápida.
                val existing = rows[id] ?: throw NotFoundException(ErrorCode.CHILD_NOT_FOUND, "No existe esa fila de prueba")
                val row = existing.copy(deleted = true, syncVersion = ++counter)
                rows[id] = row
                EntityChange(row.syncVersion, row.toJson())
            }
        }
    }

    /** Inserta una fila directo, sin pasar por [apply], para preparar el estado de una prueba. */
    fun seed(childId: UUID, value: String = "inicial"): UUID {
        val id = UUID.randomUUID()
        rows[id] = Row(id, childId, value, deleted = false, syncVersion = ++counter)
        return id
    }

    private fun Row.toJson() = buildJsonObject {
        put("id", id.toString())
        put("childId", childId.toString())
        put("value", value)
        put("deleted", deleted)
    }
}
