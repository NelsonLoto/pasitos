package com.pasitos.backend.sync

import com.pasitos.backend.common.Caller
import com.pasitos.shared.api.sync.SyncOp
import java.util.UUID
import kotlinx.serialization.json.JsonElement

/** Una fila ya convertida al contrato de `shared/api`, con su `sync_version` para el cursor. */
data class EntityChange(val syncVersion: Long, val json: JsonElement)

/**
 * Lo que un dominio sincronizable implementa para enchufarse al motor genérico de
 * `/sync/changes`, `/sync/snapshot` y `/sync/push` (14 · Sincronización, 11 · Backend §2). El
 * motor no conoce la forma de cada entidad - solo el dominio sabe mapear sus filas Exposed al
 * contrato de `shared/api` y aplicar sus propias reglas de negocio.
 *
 * En I0 ninguna entidad real se registra todavía (I1 registra `MEASUREMENT`, etc.); las pruebas
 * de este dominio registran una propia, ajena a cualquier tabla de producción.
 *
 * Todas las funciones asumen una transacción Exposed abierta (igual que los repositorios).
 */
interface SyncEntity {

    /** Nombre con el que viaja en el JSON, ej. `"MEASUREMENT"` (ADR-28: siempre en mayúsculas). */
    val name: String

    /**
     * Filas de algún niño en [childIds] con `sync_version` mayor a [cursor], hasta [limit],
     * ordenadas por `sync_version` ascendente. Incluye lápidas (`deleted_at`) serializadas con
     * `"deleted": true` en el JSON - el motor no decide ese formato, es responsabilidad de cada
     * entidad.
     */
    fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<EntityChange>

    /** Todas las filas vigentes de un solo niño, para la foto completa de un `newChildId`. */
    fun snapshot(childId: UUID): List<EntityChange>

    /**
     * Aplica una mutación del teléfono (`UPSERT` o `DELETE`). Debe ser idempotente por el `id`
     * que trae [data] (ADR-15: reenviar el mismo lote produce el mismo resultado; un `DELETE`
     * de algo ya borrado devuelve igual el estado actual, no un error).
     *
     * Lanza [com.pasitos.backend.common.ApiException] si se rechaza (permisos, autoría, rango,
     * regla de negocio) - [SyncService] la traduce a un resultado `REJECTED`, nunca propaga la
     * excepción a las demás mutaciones del lote.
     */
    fun apply(caller: Caller, op: SyncOp, data: JsonElement): EntityChange
}
