package com.pasitos.backend.sync

import com.pasitos.backend.auth.Caller
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/** La versión solo ordena en el servidor: no se agrega a los datos de cada fila en la API. */
data class SyncRow(val syncVersion: Long, val childId: UUID, val data: JsonObject) {
    override fun toString() = "SyncRow(syncVersion=$syncVersion, data=[redacted])"
}

enum class SyncOperation { UPSERT, DELETE }
data class SyncApplied(val syncVersion: Long?)

/** Rechazo esperado de un dominio. El mensaje no debe contener datos ingresados por usuarios. */
class SyncRejection(val code: String, override val message: String, val rule: String? = null) : RuntimeException(code)

interface SyncEntity {
    val name: String
    val collectionName: String

    /** Ascendente, hasta limit filas, incluidas las lápidas; solo de childIds. */
    fun changesSince(caller: Caller, childIds: Set<UUID>, cursor: Long, limit: Int): List<SyncRow>
    /** Todas las filas vigentes de ese niño. */
    fun snapshot(caller: Caller, childId: UUID): List<JsonObject>

    /**
     * Corre dentro de su propia transacción. El dominio valida alcance, autoría y reglas.
     * Mismo id y mismos datos conservan el resultado; una lápida nunca se resucita.
     * DELETE repetido es APPLIED. No se escribe por otro camino (ADR-28).
     */
    fun apply(caller: Caller, op: SyncOperation, data: JsonObject): SyncApplied =
        throw SyncRejection("invalid_input", "Esta entidad no admite cambios por sincronización")
}

class SyncRegistry(entities: List<SyncEntity> = emptyList()) {
    val entities = entities.toList()
    private val byName = this.entities.associateBy { it.name }
    init {
        require(byName.size == this.entities.size) { "Entidad de sincronización duplicada" }
        require(this.entities.map { it.collectionName }.distinct().size == this.entities.size) { "Colección duplicada" }
        require(this.entities.all { it.name.matches(Regex("[A-Z][A-Z0-9_]*")) && it.collectionName.matches(Regex("[a-z][a-z0-9_]*")) })
    }
    operator fun get(name: String): SyncEntity? = byName[name]
}
