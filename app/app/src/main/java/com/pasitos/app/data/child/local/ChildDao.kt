package com.pasitos.app.data.child.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pasitos.app.data.child.model.GuardianRole
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChildDao {

    /** Del mayor al menor (RF-HIJ-04). */
    @Query("SELECT * FROM children ORDER BY birth_date, first_name")
    abstract fun observeAll(): Flow<List<ChildEntity>>

    @Upsert
    abstract suspend fun upsert(child: ChildEntity)

    @Query("DELETE FROM children WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Upsert
    abstract suspend fun upsertGuardianship(guardianship: GuardianshipEntity)

    @Query("DELETE FROM guardianships WHERE child_id = :childId AND account_id = :accountId")
    abstract suspend fun deleteGuardianship(childId: String, accountId: String)

    @Query("DELETE FROM guardianships WHERE child_id = :childId")
    abstract suspend fun deleteGuardianships(childId: String)

    /** Rol de [accountId] sobre el niño, o nulo si no es su tutor. */
    @Query("SELECT role FROM guardianships WHERE child_id = :childId AND account_id = :accountId")
    abstract fun observeRole(childId: String, accountId: String): Flow<GuardianRole?>

    /** El último hijo elegido. Puede ser uno que ya no está: el repositorio lo resuelve. */
    @Query("SELECT child_id FROM active_child WHERE id = ${ActiveChildEntity.SINGLE_ROW_ID}")
    abstract fun observeActiveChildId(): Flow<String?>

    @Upsert
    abstract suspend fun saveActiveChild(active: ActiveChildEntity)

    /** Alta (RF-HIJ-01): el niño, quien lo dio de alta como principal y el niño como activo (CA1). */
    @Transaction
    open suspend fun insertCreated(child: ChildEntity, principal: GuardianshipEntity) {
        upsert(child)
        upsertGuardianship(principal)
        saveActiveChild(ActiveChildEntity(childId = child.id))
    }
}
