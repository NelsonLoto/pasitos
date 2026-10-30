package com.pasitos.app.data.vaccination

import com.pasitos.app.data.vaccination.model.AppliedDoseRecord
import com.pasitos.app.data.vaccination.model.DoseApplicationInput
import com.pasitos.app.data.vaccination.model.OtherVaccine
import com.pasitos.app.data.vaccination.model.OtherVaccineInput
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Vacunación de los hijos (RF-VAC-01 a 05, 07). Se lee de la base local, también sin conexión; las
 * escrituras van por REST y solo funcionan en línea (RNF-OFF-06). Sin red devuelven
 * `NoConnectionException`; si el servidor rechaza, `ApiException` con su mensaje.
 */
interface VaccinationRepository {

    /** El calendario vigente guardado en el teléfono; [VaccinationCalendar.EMPTY] mientras no se bajó. */
    val calendar: StateFlow<VaccinationCalendar>

    fun observeAppliedDoses(childId: String): Flow<List<AppliedDoseRecord>>

    /** Por fecha de aplicación, la más reciente primero. */
    fun observeOtherVaccines(childId: String): Flow<List<OtherVaccine>>

    /** Pide el calendario si cambió, para quien lo necesita antes de la próxima sincronización. */
    suspend fun refreshCalendar(): Result<Unit>

    /** RF-VAC-02. Si otro tutor la acaba de registrar (RN-VAC-09), falla y pide sincronizar para traer ese registro. */
    suspend fun registerDose(childId: String, doseCode: String, input: DoseApplicationInput): Result<AppliedDoseRecord>

    /** RF-VAC-03 y RF-VAC-04 CA3: solo el autor. */
    suspend fun updateDose(doseId: String, input: DoseApplicationInput): Result<AppliedDoseRecord>

    suspend fun deleteDose(doseId: String): Result<Unit>

    /** RF-VAC-04: dosis aplicadas con fecha no registrada. Devuelve las que se crearon. */
    suspend fun registerPreviousDoses(childId: String, doseCodes: List<String>): Result<List<AppliedDoseRecord>>

    /** RF-VAC-05: crea una vacuna si [vaccineId] es nulo, o la edita. */
    suspend fun saveOtherVaccine(childId: String, vaccineId: String?, input: OtherVaccineInput): Result<OtherVaccine>

    suspend fun deleteOtherVaccine(vaccineId: String): Result<Unit>
}
