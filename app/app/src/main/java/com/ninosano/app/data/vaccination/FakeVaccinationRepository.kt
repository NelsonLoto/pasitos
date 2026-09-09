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
        VaccineDoseSchedule("bcg", "BCG", null, "Al nacer", 0),
        VaccineDoseSchedule("hepb-1", "Hepatitis B", "1ª dosis", "Al nacer", 0),
        VaccineDoseSchedule("penta-1", "Pentavalente", "1ª dosis", "2 meses", 60),
        VaccineDoseSchedule("penta-2", "Pentavalente", "2ª dosis", "4 meses", 120),
        VaccineDoseSchedule("penta-3", "Pentavalente", "3ª dosis", "6 meses", 180),
        VaccineDoseSchedule("neumo-2", "Neumococo", "2ª dosis", "6 meses", 180),
        VaccineDoseSchedule("flu", "Antigripal", null, "6 meses", 180),
    )

    private val _appliedDoses = MutableStateFlow<List<AppliedDose>>(emptyList())
    override val appliedDoses: StateFlow<List<AppliedDose>> = _appliedDoses.asStateFlow()

    override suspend fun markApplied(childId: String, doseScheduleId: String, appliedDate: LocalDate): Result<Unit> {
        delay(500)
        _appliedDoses.update { current ->
            current.filterNot { it.childId == childId && it.doseScheduleId == doseScheduleId } +
                AppliedDose(childId, doseScheduleId, appliedDate)
        }
        return Result.success(Unit)
    }
}
