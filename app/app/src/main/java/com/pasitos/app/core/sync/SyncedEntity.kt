package com.pasitos.app.core.sync

import kotlinx.serialization.json.JsonObject

/**
 * Una entidad que llega del servidor por `GET /sync/changes` (14 · Sincronización §6). Cada dominio
 * registra la suya con `@IntoSet` en su módulo de Hilt: el [SyncEngine] no conoce ninguna en
 * particular, igual que el `SyncRegistry` del backend.
 */
interface SyncedEntity {

    /** Nombre de la entidad en el servidor, por ejemplo `"CHILD"`. */
    val name: String

    /**
     * Guarda una fila del servidor o, si es una lápida (`"deleted": true`), borra la copia local.
     * Corre dentro de la transacción del motor. Si la fila no se puede leer, lanza
     * `SerializationException` o `IllegalArgumentException` y el motor la saltea.
     */
    suspend fun apply(row: JsonObject)

    /** Borra todas las filas locales de un niño, porque se eliminó o se perdió el acceso (RNF-OFF-05). */
    suspend fun deleteChild(childId: String)
}
