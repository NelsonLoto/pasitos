package com.pasitos.app.data.vaccination.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccinationDao {

    @Query("SELECT * FROM vaccination_calendar WHERE id = ${VaccinationCalendarEntity.SINGLE_ROW_ID}")
    fun observeCalendar(): Flow<VaccinationCalendarEntity?>

    @Query("SELECT etag FROM vaccination_calendar WHERE id = ${VaccinationCalendarEntity.SINGLE_ROW_ID}")
    suspend fun calendarEtag(): String?

    @Upsert
    suspend fun saveCalendar(calendar: VaccinationCalendarEntity)

    @Query("SELECT * FROM applied_doses WHERE child_id = :childId")
    fun observeAppliedDoses(childId: String): Flow<List<AppliedDoseEntity>>

    @Upsert
    suspend fun upsertAppliedDoses(doses: List<AppliedDoseEntity>)

    @Query("DELETE FROM applied_doses WHERE id = :id")
    suspend fun deleteAppliedDose(id: String)

    @Query("DELETE FROM applied_doses WHERE child_id = :childId")
    suspend fun deleteAppliedDoses(childId: String)

    /** Por fecha de aplicación, la más reciente primero. */
    @Query("SELECT * FROM other_vaccines WHERE child_id = :childId ORDER BY applied_on DESC, name")
    fun observeOtherVaccines(childId: String): Flow<List<OtherVaccineEntity>>

    @Upsert
    suspend fun upsertOtherVaccine(vaccine: OtherVaccineEntity)

    @Query("DELETE FROM other_vaccines WHERE id = :id")
    suspend fun deleteOtherVaccine(id: String)

    @Query("DELETE FROM other_vaccines WHERE child_id = :childId")
    suspend fun deleteOtherVaccines(childId: String)
}
