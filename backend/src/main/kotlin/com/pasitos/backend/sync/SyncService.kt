package com.pasitos.backend.sync

import com.pasitos.backend.auth.Caller
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.sync.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.UUID
import java.sql.Connection

class SyncFailure(val code: ErrorCode, override val message: String) : RuntimeException(code.wireName)

class SyncService(
    private val database: Database,
    private val registry: SyncRegistry,
    private val repository: SyncRepository = SyncRepository(),
) {
    fun changes(caller: Caller, cursor: Long): SyncChangesResponse = transaction(Connection.TRANSACTION_READ_COMMITTED, false, database) {
        repository.lockForRead()
        val ceiling = repository.committedVersion()
        if (cursor < 0 || cursor > ceiling) throw invalidSync("El cursor de sincronización no es válido")
        val children = repository.guardianChildren(caller.accountId)
        val rows = if (children.isEmpty()) emptyList() else registry.entities.flatMap { entity ->
            entity.changesSince(caller, children.keys, cursor, PAGE_SIZE + 1).map { entity.collectionName to it }
        }.also { rows ->
            check(rows.all { (_, row) -> row.childId in children && row.syncVersion > cursor && row.syncVersion <= ceiling }) {
                "El manejador devolvió una fila fuera del alcance o del cursor"
            }
        }.sortedBy { it.second.syncVersion }
        val hasMore = rows.size > PAGE_SIZE
        val page = rows.take(PAGE_SIZE)
        val next = if (hasMore) page.last().second.syncVersion else ceiling
        SyncChangesResponse(next, hasMore, page.groupBy({ it.first }, { it.second.data }),
            children.filterValues { it > cursor && it <= next }.keys.map(UUID::toString).sorted(),
            repository.revokedSince(caller.accountId, cursor, next))
    }

    fun snapshot(caller: Caller, childId: UUID): SyncSnapshotResponse = transaction(Connection.TRANSACTION_READ_COMMITTED, false, database) {
        repository.lockForRead()
        if (childId !in repository.guardianChildren(caller.accountId)) {
            throw SyncFailure(ErrorCode.NOT_FOUND, "No se encontró ese recurso")
        }
        SyncSnapshotResponse(registry.entities.associate { it.collectionName to it.snapshot(caller, childId) })
    }

    fun push(caller: Caller, request: SyncPushRequest): SyncPushResponse = SyncPushResponse(request.mutations.map { mutation ->
        try {
            val applied = transaction(database) {
                repository.lockForWrite()
                if (syncUuid(mutation.mutationId) == null || syncUuid((mutation.data["id"] as? JsonPrimitive)?.contentOrNull.orEmpty()) == null) {
                    throw SyncRejection("invalid_input", "Los identificadores deben ser UUID válidos")
                }
                val operation = SyncOperation.entries.firstOrNull { it.name == mutation.op }
                    ?: throw SyncRejection("invalid_input", "La operación de sincronización no es válida")
                if (mutation.entity !in WRITABLE_ENTITIES) throw SyncRejection("invalid_input", "Esta entidad no admite cambios por sincronización")
                val entity = registry[mutation.entity]
                    ?: throw SyncRejection("invalid_input", "Esta entidad todavía no está disponible para sincronizar")
                entity.apply(caller, operation, mutation.data).also {
                    check(it.syncVersion == null || it.syncVersion > 0) { "Versión de sincronización inválida" }
                }
            }
            SyncMutationResult(mutation.mutationId, SyncMutationStatus.APPLIED, syncVersion = applied.syncVersion)
        } catch (rejection: SyncRejection) {
            // Fuera de transaction: cualquier escritura de la mutación rechazada se revierte.
            SyncMutationResult(mutation.mutationId, SyncMutationStatus.REJECTED,
                code = rejection.code, rule = rejection.rule, message = rejection.message)
        }
    })

    companion object {
        const val PAGE_SIZE = 500
        private val WRITABLE_ENTITIES = setOf("MEASUREMENT", "HABIT", "EVENT", "MILESTONE_ACHIEVEMENT")
    }
}

fun invalidSync(message: String) = SyncFailure(ErrorCode.INVALID_INPUT, message)

internal fun syncUuid(value: String): UUID? = if (!value.matches(Regex("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))) null
    else try { UUID.fromString(value) } catch (_: IllegalArgumentException) { null }
