package com.pasitos.app.data.vaccination

import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.vaccination.model.AppliedDoseRecord
import com.pasitos.app.data.vaccination.model.CalendarDoseInfo
import com.pasitos.app.data.vaccination.model.DoseApplicationInput
import com.pasitos.app.data.vaccination.model.FormerDose
import com.pasitos.app.data.vaccination.model.OtherVaccine
import com.pasitos.app.data.vaccination.model.OtherVaccineInput
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.vaccination.CalendarDose
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Datos ficticios para las pruebas de vacunación (D-65). Hoy es el 19/10/2026 ([ChildTestData.clock])
 * y Lucía nació el 16/10/2025: tiene un año y tres días.
 */
object VaccinationTestData {

    const val OTHER_ACCOUNT_ID = "22222222-2222-2222-2222-222222222222"
    val LUCIA_BIRTH: LocalDate = LocalDate.of(2025, 10, 16)
    const val SOURCE = "Calendario Nacional de Vacunación 2026, Ministerio de Salud de la Nación"

    /** Un extracto del Calendario Nacional 2026, con las dos filas por cohorte de la 2ª de triple viral. */
    val calendar = VaccinationCalendar(
        effectiveFrom = LocalDate.of(2026, 1, 1),
        doses = listOf(
            dose("BCG", "BCG", "Única dosis", 0, 1, prevents = "Formas graves de tuberculosis"),
            dose("HB-NEO", "Hepatitis B", "Dosis neonatal", 0, 2),
            dose("NEUMO-1", "Neumococo conjugada", "1ª dosis", 2, 3, prevents = "Meningitis, neumonía y sepsis por neumococo"),
            dose("PENTA-1", "Quíntuple (pentavalente)", "1ª dosis", 2, 4),
            dose("HA", "Hepatitis A", "Única dosis", 12, 16),
            dose("SRP-2", "Triple viral (SRP)", "2ª dosis", 15, 20, tolerance = 120, bornFrom = LocalDate.of(2024, 7, 1)),
            dose("VAR-1", "Varicela", "1ª dosis", 15, 21),
            dose("SRP-2", "Triple viral (SRP)", "2ª dosis", 60, 23, tolerance = 365, bornUntil = LocalDate.of(2024, 6, 30)),
        ),
        formerDoses = listOf(FormerDose("FA-1", "Fiebre amarilla", "1ª dosis")),
    )

    fun applied(
        doseCode: String,
        appliedOn: LocalDate? = LocalDate.of(2025, 10, 17),
        author: String = ChildTestData.ACCOUNT_ID,
        id: String = "dose-$doseCode",
        childId: String = ChildTestData.SOFIA,
    ) = AppliedDoseRecord(
        id = id,
        childId = childId,
        doseCode = doseCode,
        appliedOn = appliedOn,
        lot = null,
        place = null,
        authorAccountId = author,
        authorRole = AuthorRole.GUARDIAN,
    )

    fun otherVaccine(id: String = "other-1", author: String = ChildTestData.ACCOUNT_ID) = OtherVaccine(
        id = id,
        childId = ChildTestData.SOFIA,
        name = "Antigripal",
        appliedOn = LocalDate.of(2026, 5, 1),
        lot = "X1",
        note = "Campaña de otoño",
        authorAccountId = author,
        authorRole = AuthorRole.GUARDIAN,
    )

    private fun dose(
        code: String,
        vaccine: String,
        label: String,
        ageMonths: Int,
        sortOrder: Int,
        tolerance: Int = 30,
        bornFrom: LocalDate? = null,
        bornUntil: LocalDate? = null,
        prevents: String? = vaccine,
    ) = CalendarDoseInfo(
        dose = CalendarDose(code, vaccine, label, ageMonths, 7, tolerance, sortOrder, bornFrom, bornUntil),
        prevents = prevents,
        source = SOURCE,
    )
}

/** Repositorio en memoria para las pruebas de las pantallas. */
class FakeVaccinationRepository(calendar: VaccinationCalendar = VaccinationTestData.calendar) : VaccinationRepository {

    override val calendar = MutableStateFlow(calendar)
    val appliedDoses = MutableStateFlow<List<AppliedDoseRecord>>(emptyList())
    val otherVaccines = MutableStateFlow<List<OtherVaccine>>(emptyList())

    var result: Result<Unit> = Result.success(Unit)
    var refreshCount = 0
    val registered = mutableListOf<Triple<String, String, DoseApplicationInput>>()
    val updated = mutableListOf<Pair<String, DoseApplicationInput>>()
    val deletedDoses = mutableListOf<String>()
    val previousDoses = mutableListOf<Pair<String, List<String>>>()
    val savedOtherVaccines = mutableListOf<Triple<String, String?, OtherVaccineInput>>()
    val deletedOtherVaccines = mutableListOf<String>()

    override fun observeAppliedDoses(childId: String): Flow<List<AppliedDoseRecord>> =
        appliedDoses.map { doses -> doses.filter { it.childId == childId } }

    override fun observeOtherVaccines(childId: String): Flow<List<OtherVaccine>> =
        otherVaccines.map { vaccines -> vaccines.filter { it.childId == childId } }

    override suspend fun refreshCalendar(): Result<Unit> {
        refreshCount++
        return result
    }

    override suspend fun registerDose(childId: String, doseCode: String, input: DoseApplicationInput) = result.map {
        registered += Triple(childId, doseCode, input)
        VaccinationTestData.applied(doseCode, input.appliedOn).also { appliedDoses.value += it }
    }

    override suspend fun updateDose(doseId: String, input: DoseApplicationInput) = result.map {
        updated += doseId to input
        val current = appliedDoses.value.first { it.id == doseId }
        current.copy(appliedOn = input.appliedOn, lot = input.lot, place = input.place).also { changed ->
            appliedDoses.value = appliedDoses.value.map { if (it.id == doseId) changed else it }
        }
    }

    override suspend fun deleteDose(doseId: String) = result.map {
        deletedDoses += doseId
        appliedDoses.value = appliedDoses.value.filterNot { it.id == doseId }
    }

    override suspend fun registerPreviousDoses(childId: String, doseCodes: List<String>) = result.map {
        previousDoses += childId to doseCodes
        doseCodes.map { VaccinationTestData.applied(it, appliedOn = null, childId = childId) }.also { appliedDoses.value += it }
    }

    override suspend fun saveOtherVaccine(childId: String, vaccineId: String?, input: OtherVaccineInput) = result.map {
        savedOtherVaccines += Triple(childId, vaccineId, input)
        VaccinationTestData.otherVaccine(id = vaccineId ?: "other-new").copy(name = input.name, appliedOn = input.appliedOn)
    }

    override suspend fun deleteOtherVaccine(vaccineId: String) = result.map {
        deletedOtherVaccines += vaccineId
        otherVaccines.value = otherVaccines.value.filterNot { it.id == vaccineId }
    }
}
