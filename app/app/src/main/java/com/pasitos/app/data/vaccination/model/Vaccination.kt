package com.pasitos.app.data.vaccination.model

import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.vaccination.AppliedDose
import com.pasitos.shared.vaccination.CalendarDose
import com.pasitos.shared.vaccination.VaccinationStatus
import com.pasitos.shared.vaccination.doseStatuses
import java.time.LocalDate

/** Una dosis del calendario vigente, con lo que muestra su detalle (RF-VAC-07). */
data class CalendarDoseInfo(
    val dose: CalendarDose,
    val prevents: String?,
    val source: String?,
)

/** Una dosis que el calendario vigente ya no tiene, para mostrar las aplicadas (RN-VAC-03). */
data class FormerDose(
    val doseCode: String,
    val vaccineName: String,
    val doseLabel: String,
)

/**
 * El Calendario Nacional vigente guardado en el teléfono. Vacío mientras no se bajó, o si el
 * servidor todavía no tiene uno publicado.
 */
data class VaccinationCalendar(
    val effectiveFrom: LocalDate?,
    val doses: List<CalendarDoseInfo>,
    val formerDoses: List<FormerDose>,
) {
    val isEmpty: Boolean get() = doses.isEmpty()

    /** Los datos de una dosis para un niño: con el código hay una fila por cohorte (RN-VAC-10). */
    fun info(doseCode: String, birthDate: LocalDate): CalendarDoseInfo? =
        doses.firstOrNull { it.dose.doseCode == doseCode && it.dose.appliesTo(birthDate) }

    fun former(doseCode: String): FormerDose? = formerDoses.firstOrNull { it.doseCode == doseCode }

    /** Los estados de cada dosis del calendario para un niño (RN-VAC-01), con las fechas en hora argentina. */
    fun statusFor(birthDate: LocalDate, applied: List<AppliedDoseRecord>, today: LocalDate): VaccinationStatus =
        doseStatuses(
            birthDate = birthDate,
            calendar = doses.map { it.dose },
            applied = applied.map { AppliedDose(it.doseCode, it.appliedOn) },
            today = today,
        )

    /**
     * Las dosis que ofrece la carga retroactiva (RF-VAC-04): las del calendario vigente que
     * corresponden al niño, con fecha indicada anterior a hoy y todavía sin registrar, en el orden
     * del calendario.
     */
    fun previousDoses(birthDate: LocalDate, appliedCodes: Set<String>, today: LocalDate): List<CalendarDose> =
        doses.map { it.dose }
            .filter { it.appliesTo(birthDate) && it.doseCode !in appliedCodes }
            .filter { birthDate.plusMonths(it.ageMonths.toLong()).isBefore(today) }
            .distinctBy { it.doseCode }
            .sortedBy { it.sortOrder }

    companion object {
        val EMPTY = VaccinationCalendar(effectiveFrom = null, doses = emptyList(), formerDoses = emptyList())
    }
}

/** Una dosis del calendario registrada para un niño (RF-VAC-02). [appliedOn] nulo: fecha no registrada (RN-VAC-04). */
data class AppliedDoseRecord(
    val id: String,
    val childId: String,
    val doseCode: String,
    val appliedOn: LocalDate?,
    val lot: String?,
    val place: String?,
    val authorAccountId: String,
    val authorRole: AuthorRole,
)

/** Una vacuna fuera del Calendario Nacional, sin estado (RF-VAC-05, RN-VAC-06). */
data class OtherVaccine(
    val id: String,
    val childId: String,
    val name: String,
    val appliedOn: LocalDate,
    val lot: String?,
    val note: String?,
    val authorAccountId: String,
    val authorRole: AuthorRole,
)

/** Lo que se carga al registrar o editar una dosis. [appliedOn] solo es nulo en una de carga retroactiva. */
data class DoseApplicationInput(
    val appliedOn: LocalDate?,
    val lot: String?,
    val place: String?,
)

data class OtherVaccineInput(
    val name: String,
    val appliedOn: LocalDate,
    val lot: String?,
    val note: String?,
)

/**
 * La edad indicada como la escribe el calendario oficial: "Al nacer", "2 meses", "15 meses",
 * "5 años". Hasta los 23 meses va en meses; desde los 24, en años si son años justos.
 */
fun ageMonthsLabel(ageMonths: Int): String = when {
    ageMonths == 0 -> "Al nacer"
    ageMonths == 1 -> "1 mes"
    ageMonths >= 24 && ageMonths % 12 == 0 -> "${ageMonths / 12} años"
    else -> "$ageMonths meses"
}
