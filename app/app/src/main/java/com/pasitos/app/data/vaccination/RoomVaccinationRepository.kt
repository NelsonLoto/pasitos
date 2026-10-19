package com.pasitos.app.data.vaccination

import android.util.Log
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.sync.SyncEngine
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.data.vaccination.local.VaccinationDao
import com.pasitos.app.data.vaccination.model.AppliedDoseRecord
import com.pasitos.app.data.vaccination.model.DoseApplicationInput
import com.pasitos.app.data.vaccination.model.OtherVaccine
import com.pasitos.app.data.vaccination.model.OtherVaccineInput
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import com.pasitos.app.data.vaccination.remote.VaccinationRemoteDataSource
import com.pasitos.app.data.vaccination.remote.toDomain
import com.pasitos.app.data.vaccination.remote.toEntity
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.vaccination.CreateAppliedDoseRequest
import com.pasitos.shared.api.vaccination.OtherVaccineRequest
import com.pasitos.shared.api.vaccination.PatchAppliedDoseRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesRequest
import com.pasitos.shared.api.vaccination.VaccinationCalendarResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * [VaccinationRepository] sobre Room (15 · App Android). Las respuestas se guardan con el `Mutex`
 * del [SyncEngine], para que una sincronización en curso no las pise con una versión vieja
 * (14 · Sincronización §6).
 */
@Singleton
class RoomVaccinationRepository @Inject constructor(
    private val dao: VaccinationDao,
    private val remote: VaccinationRemoteDataSource,
    private val calendarData: VaccinationCalendarReferenceData,
    private val syncEngine: SyncEngine,
    private val syncScheduler: SyncScheduler,
) : VaccinationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val calendar: StateFlow<VaccinationCalendar> = dao.observeCalendar()
        .map { entity ->
            entity?.let { vaccinationJson.decodeFromString(VaccinationCalendarResponse.serializer(), it.json).toDomain() }
                ?: VaccinationCalendar.EMPTY
        }
        // Al cerrar sesión la base se borra mientras se la observa, y la app se reinicia enseguida
        .catch { e ->
            Log.w(TAG, "No se pudo leer el calendario local: ${e::class.simpleName}")
            emit(VaccinationCalendar.EMPTY)
        }
        .stateIn(scope, SharingStarted.Eagerly, VaccinationCalendar.EMPTY)

    override fun observeAppliedDoses(childId: String): Flow<List<AppliedDoseRecord>> =
        dao.observeAppliedDoses(childId).map { rows -> rows.map { it.toDomain() } }

    override fun observeOtherVaccines(childId: String): Flow<List<OtherVaccine>> =
        dao.observeOtherVaccines(childId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshCalendar(): Result<Unit> = runCatching { calendarData.refresh() }

    override suspend fun registerDose(childId: String, doseCode: String, input: DoseApplicationInput): Result<AppliedDoseRecord> =
        write {
            val request = CreateAppliedDoseRequest(
                doseCode = doseCode,
                appliedOn = requireNotNull(input.appliedOn) { "Falta la fecha de aplicación" }.toString(),
                lot = input.lot.cleaned(),
                place = input.place.cleaned(),
            )
            val dose = try {
                remote.createDose(childId, request).toEntity()
            } catch (e: ApiException) {
                // Otro tutor la acaba de registrar (RN-VAC-09): la sincronización trae ese registro
                if (e.code == ErrorCode.BUSINESS_RULE) syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
                throw e
            }
            dao.upsertAppliedDoses(listOf(dose))
            dose.toDomain()
        }

    override suspend fun updateDose(doseId: String, input: DoseApplicationInput): Result<AppliedDoseRecord> =
        write {
            val request = PatchAppliedDoseRequest(
                appliedOn = input.appliedOn?.toString(),
                lot = input.lot.cleaned(),
                place = input.place.cleaned(),
            )
            val dose = remote.updateDose(doseId, request).toEntity()
            dao.upsertAppliedDoses(listOf(dose))
            dose.toDomain()
        }

    override suspend fun deleteDose(doseId: String): Result<Unit> =
        write {
            remote.deleteDose(doseId)
            dao.deleteAppliedDose(doseId)
        }

    override suspend fun registerPreviousDoses(childId: String, doseCodes: List<String>): Result<List<AppliedDoseRecord>> =
        write {
            val created = remote.registerRetroactive(childId, RetroactiveDosesRequest(doseCodes)).created.map { it.toEntity() }
            dao.upsertAppliedDoses(created)
            created.map { it.toDomain() }
        }

    override suspend fun saveOtherVaccine(childId: String, vaccineId: String?, input: OtherVaccineInput): Result<OtherVaccine> =
        write {
            val request = OtherVaccineRequest(
                name = input.name.trim(),
                appliedOn = input.appliedOn.toString(),
                lot = input.lot.cleaned(),
                note = input.note.cleaned(),
            )
            val response = if (vaccineId == null) {
                remote.createOtherVaccine(childId, request)
            } else {
                remote.updateOtherVaccine(vaccineId, request)
            }
            val vaccine = response.toEntity()
            dao.upsertOtherVaccine(vaccine)
            vaccine.toDomain()
        }

    override suspend fun deleteOtherVaccine(vaccineId: String): Result<Unit> =
        write {
            remote.deleteOtherVaccine(vaccineId)
            dao.deleteOtherVaccine(vaccineId)
        }

    /** Pedido y guardado sin ninguna sincronización en curso; después se sincroniza para dejar el cursor al día. */
    private suspend fun <T> write(block: suspend () -> T): Result<T> = runCatching {
        val result = syncEngine.withLock { block() }
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
        result
    }

    private fun String?.cleaned(): String? = this?.trim()?.ifEmpty { null }

    private companion object {
        const val TAG = "RoomVaccinationRepository"
    }
}
