package com.pasitos.app.core.sync.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** Entidades que el teléfono puede enviar con `POST /sync/push` (14 · Sincronización, ADR-28). */
enum class MutationEntity { MEASUREMENT, HABIT, EVENT, MILESTONE_ACHIEVEMENT }

enum class MutationOp { UPSERT, DELETE }

/**
 * Cambio hecho en el teléfono que el servidor todavía no confirmó (bandeja de salida). Se envían
 * en el orden de [sequence]. El servidor responde por [mutationId] y aplica cada `UPSERT` por el
 * id del registro, así que reenviar un cambio no lo duplica (RNF-OFF-03).
 */
@Entity(
    tableName = "pending_mutations",
    indices = [Index(value = ["mutation_id"], unique = true), Index(value = ["child_id"])],
)
data class PendingMutationEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    @ColumnInfo(name = "mutation_id") val mutationId: String,
    val entity: MutationEntity,
    /** Id del registro, para marcarlo como sincronizado o con error según la respuesta. */
    @ColumnInfo(name = "entity_id") val entityId: String,
    /** Niño al que pertenece el registro, para descartar sus cambios si se pierde el acceso. */
    @ColumnInfo(name = "child_id") val childId: String,
    val op: MutationOp,
    /** Campo `data` del cambio, ya serializado en JSON con el DTO de `shared`. */
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
