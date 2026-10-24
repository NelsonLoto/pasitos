package com.pasitos.app.data.measurement.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    /** De la más reciente a la más antigua (RF-CRE-03, CA1). */
    @Query("SELECT * FROM measurements WHERE child_id = :childId ORDER BY measured_on DESC, id")
    fun observeByChild(childId: String): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE id = :id")
    fun observe(id: String): Flow<MeasurementEntity?>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun find(id: String): MeasurementEntity?

    /** RN-CRE-12: la medición de ese autor en esa fecha, salvo [excludeId] (la que se está editando). */
    @Query(
        "SELECT * FROM measurements WHERE child_id = :childId AND author_account_id = :authorAccountId " +
            "AND measured_on = :date AND id != :excludeId LIMIT 1",
    )
    suspend fun findByAuthorOn(childId: String, authorAccountId: String, date: LocalDate, excludeId: String): MeasurementEntity?

    @Upsert
    suspend fun upsert(measurement: MeasurementEntity)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM measurements WHERE child_id = :childId")
    suspend fun deleteForChild(childId: String)
}
