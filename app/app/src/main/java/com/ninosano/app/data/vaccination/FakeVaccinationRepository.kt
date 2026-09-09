package com.ninosano.app.data.vaccination

import com.ninosano.app.data.vaccination.model.AppliedDose
import com.ninosano.app.data.vaccination.model.VaccineDoseSchedule
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class FakeVaccinationRepository @Inject constructor() : VaccinationRepository {

    override val schedule: List<VaccineDoseSchedule> = listOf(
        VaccineDoseSchedule(
            "bcg", "BCG", null, "Al nacer", 0,
            "Protege contra las formas graves de tuberculosis. Dosis única, se aplica en la maternidad.",
        ),
        VaccineDoseSchedule(
            "hepb-1", "Hepatitis B", "1ª dosis", "Al nacer", 0,
            "Protege contra el virus de la hepatitis B. Primera dosis del esquema, se aplica en la maternidad.",
        ),
        VaccineDoseSchedule(
            "penta-1", "Pentavalente", "1ª dosis", "2 meses", 60,
            "Protege contra difteria, tétanos, tos convulsa, hepatitis B y Haemophilus influenzae tipo b (5 en 1). Primera dosis del esquema primario.",
        ),
        VaccineDoseSchedule(
            "penta-2", "Pentavalente", "2ª dosis", "4 meses", 120,
            "Protege contra difteria, tétanos, tos convulsa, hepatitis B y Haemophilus influenzae tipo b (5 en 1). Segunda dosis del esquema primario.",
        ),
        VaccineDoseSchedule(
            "penta-3", "Pentavalente", "3ª dosis", "6 meses", 180,
            "Protege contra difteria, tétanos, tos convulsa, hepatitis B y Haemophilus influenzae tipo b (5 en 1). Tercera dosis del esquema primario.",
        ),
        VaccineDoseSchedule(
            "neumo-2", "Neumococo", "2ª dosis", "6 meses", 180,
            "Protege contra las formas graves de las enfermedades causadas por el neumococo (neumonía, meningitis, otitis). Segunda dosis del esquema.",
        ),
        VaccineDoseSchedule(
            "flu", "Antigripal", null, "6 meses", 180,
            "Protege contra la gripe estacional. Se aplica una vez por año a partir de los 6 meses.",
        ),
    )

    private val _appliedDoses = MutableStateFlow<List<AppliedDose>>(emptyList())
    override val appliedDoses: StateFlow<List<AppliedDose>> = _appliedDoses.asStateFlow()

    override suspend fun markApplied(childId: String, doseScheduleId: String, appliedDate: LocalDate, lot: String?): Result<Unit> {
        delay(500)
        _appliedDoses.update { current ->
            current.filterNot { it.childId == childId && it.doseScheduleId == doseScheduleId } +
                AppliedDose(childId, doseScheduleId, appliedDate, lot)
        }
        return Result.success(Unit)
    }
}
