package com.pasitos.app.core.sync.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMutationDao {

    @Insert
    suspend fun insert(mutation: PendingMutationEntity)

    /** Los cambios más antiguos primero: el servidor los aplica en el orden en que llegan. */
    @Query("SELECT * FROM pending_mutations ORDER BY sequence LIMIT :limit")
    suspend fun oldest(limit: Int): List<PendingMutationEntity>

    /** Saca de la bandeja los cambios que el servidor ya respondió, aplicados o rechazados. */
    @Query("DELETE FROM pending_mutations WHERE mutation_id IN (:mutationIds)")
    suspend fun delete(mutationIds: List<String>)

    /** Descarta los cambios de un niño al que el usuario perdió el acceso (RNF-OFF-05). */
    @Query("DELETE FROM pending_mutations WHERE child_id = :childId")
    suspend fun deleteForChild(childId: String)

    @Query("SELECT COUNT(*) FROM pending_mutations")
    suspend fun count(): Int

    /** Registros de [entity] con algún cambio sin confirmar: la sincronización no los pisa con la copia del servidor. */
    @Query("SELECT DISTINCT entity_id FROM pending_mutations WHERE entity = :entity")
    suspend fun entityIds(entity: MutationEntity): List<String>

    /** Si el registro todavía tiene cambios sin confirmar, por ejemplo uno hecho mientras se enviaba el anterior. */
    @Query("SELECT EXISTS(SELECT 1 FROM pending_mutations WHERE entity = :entity AND entity_id = :entityId)")
    suspend fun hasPending(entity: MutationEntity, entityId: String): Boolean

    /** Para advertir antes de cerrar sesión que hay cambios sin sincronizar (RF-CTA-07, CA2). */
    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun observeCount(): Flow<Int>
}
