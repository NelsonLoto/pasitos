package com.ninosano.app.data.vaccination

import com.ninosano.app.data.vaccination.model.AppliedDose
import com.ninosano.app.data.vaccination.model.VaccineDoseSchedule
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato del calendario de vacunación (RF10, RF11). Mismo patrón mock/local que el resto
 * de los repositorios — el día que exista el backend, el schedule pasa a venir de ahí en vez
 * de estar fijo en el cliente (ver nota en VaccineDoseSchedule sobre RN04).
 */
interface VaccinationRepository {
    val schedule: List<VaccineDoseSchedule>
    val appliedDoses: StateFlow<List<AppliedDose>>

    suspend fun markApplied(childId: String, doseScheduleId: String, appliedDate: LocalDate): Result<Unit>
}
