package com.pasitos.shared.api.sync

import com.pasitos.shared.api.ErrorCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Contratos del protocolo de sincronización (14 · Sincronización, ADR-15). Cada fila de
 * [SyncChangesResponse.changes] / [SyncSnapshotResponse.changes] es [JsonElement] porque el
 * motor genérico no conoce la forma de cada entidad - solo el dominio dueño la sabe.
 */
@Serializable
data class SyncChangesResponse(
    val cursor: Long,
    val hasMore: Boolean,
    /** Clave: nombre de la entidad (`"MEASUREMENT"`). Valor: sus filas, lápidas incluidas. */
    val changes: Map<String, List<JsonElement>>,
    /** Niños que entraron al alcance desde el cursor anterior (14 · Sincronización §2). */
    val newChildIds: List<String> = emptyList(),
    /** Niños a los que se perdió acceso desde el cursor anterior. */
    val revokedChildIds: List<String> = emptyList(),
)

@Serializable
data class SyncSnapshotResponse(
    val cursor: Long,
    val changes: Map<String, List<JsonElement>>,
)

enum class SyncOp { UPSERT, DELETE }

@Serializable
data class SyncMutation(
    val mutationId: String,
    /** Nombre de la entidad, ej. `"MEASUREMENT"` (ADR-28: solo las 4 entidades de esa decisión). */
    val entity: String,
    val op: SyncOp,
    val data: JsonElement,
)

@Serializable
data class SyncPushRequest(
    val mutations: List<SyncMutation>,
)

enum class SyncMutationStatus { APPLIED, REJECTED }

@Serializable
data class SyncMutationResult(
    val mutationId: String,
    val status: SyncMutationStatus,
    /** Solo si `APPLIED`. */
    val syncVersion: Long? = null,
    /** Solo si `REJECTED`: mismo formato que [com.pasitos.shared.api.ErrorResponse]. */
    val code: ErrorCode? = null,
    val rule: String? = null,
    val message: String? = null,
)

@Serializable
data class SyncPushResponse(
    val results: List<SyncMutationResult>,
)
