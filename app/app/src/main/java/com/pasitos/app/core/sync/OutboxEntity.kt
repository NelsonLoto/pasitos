package com.pasitos.app.core.sync

import com.pasitos.app.core.sync.local.MutationEntity
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.shared.api.ErrorCode

/** Estado de un registro que se escribe en el teléfono y viaja por la bandeja de salida (RNF-OFF-04). */
enum class SyncState {
    /** Igual que en el servidor. */
    SYNCED,

    /** Tiene un cambio en `pending_mutations` que el servidor todavía no respondió. */
    PENDING,

    /** El servidor rechazó el último cambio: se muestra el motivo para corregirlo o descartarlo (14 · Sincronización §3). */
    ERROR,
}

/**
 * Una entidad que el teléfono escribe y envía con `POST /sync/push` (ADR-28). Cada dominio la
 * registra con `@IntoSet` en su módulo de Hilt, igual que su [SyncedEntity]. El [SyncEngine] saca
 * el cambio de la bandeja y después llama a una de las dos funciones, dentro de su transacción: si
 * el usuario volvió a editar el registro mientras se enviaba, la bandeja todavía tiene ese cambio.
 */
interface OutboxEntity {

    val entity: MutationEntity

    /** El servidor aplicó el cambio. */
    suspend fun onApplied(mutation: PendingMutationEntity)

    /** El servidor rechazó el cambio con su código y su motivo. */
    suspend fun onRejected(mutation: PendingMutationEntity, code: ErrorCode?, message: String?)
}
