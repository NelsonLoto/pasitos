package com.pasitos.app.data.vaccination

import com.pasitos.app.core.sync.ReferenceData
import com.pasitos.app.core.sync.SyncedEntity
import com.pasitos.app.data.vaccination.local.VaccinationCalendarEntity
import com.pasitos.app.data.vaccination.local.VaccinationDao
import com.pasitos.app.data.vaccination.remote.AppliedDoseRow
import com.pasitos.app.data.vaccination.remote.CalendarFetch
import com.pasitos.app.data.vaccination.remote.OtherVaccineRow
import com.pasitos.app.data.vaccination.remote.VaccinationRemoteDataSource
import com.pasitos.shared.api.vaccination.VaccinationCalendarResponse
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal val vaccinationJson = Json { ignoreUnknownKeys = true }

/** Dosis aplicadas en la sincronización. Llegan por acá y se escriben por REST (ADR-28). */
class AppliedDoseSyncedEntity @Inject constructor(private val dao: VaccinationDao) : SyncedEntity {

    override val name: String = "APPLIED_DOSE"

    override suspend fun apply(row: JsonObject) {
        val dose = vaccinationJson.decodeFromJsonElement(AppliedDoseRow.serializer(), row)
        if (dose.deleted) dao.deleteAppliedDose(dose.id) else dao.upsertAppliedDoses(listOf(dose.toEntity()))
    }

    override suspend fun deleteChild(childId: String) = dao.deleteAppliedDoses(childId)
}

/** Otras vacunas en la sincronización. Llegan por acá y se escriben por REST (ADR-28). */
class OtherVaccineSyncedEntity @Inject constructor(private val dao: VaccinationDao) : SyncedEntity {

    override val name: String = "OTHER_VACCINE"

    override suspend fun apply(row: JsonObject) {
        val vaccine = vaccinationJson.decodeFromJsonElement(OtherVaccineRow.serializer(), row)
        if (vaccine.deleted) dao.deleteOtherVaccine(vaccine.id) else dao.upsertOtherVaccine(vaccine.toEntity())
    }

    override suspend fun deleteChild(childId: String) = dao.deleteOtherVaccines(childId)
}

/**
 * El Calendario Nacional como dato de referencia: se pide después de cada sincronización con el
 * `ETag` de la copia local, y solo se reemplaza si cambió (14 · Sincronización).
 */
class VaccinationCalendarReferenceData @Inject constructor(
    private val dao: VaccinationDao,
    private val remote: VaccinationRemoteDataSource,
) : ReferenceData {

    override val name: String = "VACCINATION_CALENDAR"

    override suspend fun refresh() {
        when (val fetch = remote.calendar(dao.calendarEtag())) {
            CalendarFetch.NotModified -> Unit
            is CalendarFetch.Updated -> {
                // Se lee antes de guardar: una respuesta que no se entiende no reemplaza la copia buena
                vaccinationJson.decodeFromString(VaccinationCalendarResponse.serializer(), fetch.json)
                dao.saveCalendar(VaccinationCalendarEntity(etag = fetch.etag, json = fetch.json))
            }
        }
    }
}
