package com.pasitos.shared.vaccination

import java.time.LocalDate

/**
 * Estados de las dosis de [calendar] (la versión vigente) para un niño nacido el [birthDate], a la
 * fecha [today] (RN-VAC-01). `shared` no conoce la zona horaria: la app y el backend pasan
 * `ArgentinaClock.today()`.
 *
 * - Solo entran las dosis que corresponden a la fecha de nacimiento (RN-VAC-10), en el orden del
 *   calendario.
 * - La fecha indicada es el nacimiento más la edad de la dosis en meses, con edad cronológica
 *   también en prematuros (RN-VAC-08).
 * - Una dosis es Aplicada si hay un registro con su código, tenga fecha o no (RN-VAC-04). Si hay más
 *   de uno, cuenta el primero (RN-VAC-09).
 * - Las dosis no aplicadas se calculan con esta versión; las registradas que no corresponden a
 *   ninguna dosis vigente para el niño van a [VaccinationStatus.otherApplied] (RN-VAC-03).
 */
fun doseStatuses(
    birthDate: LocalDate,
    calendar: List<CalendarDose>,
    applied: List<AppliedDose>,
    today: LocalDate,
): VaccinationStatus {
    val firstApplied = applied.distinctBy { it.doseCode }
    val appliedByCode = firstApplied.associateBy { it.doseCode }
    val doses = calendar
        .filter { it.appliesTo(birthDate) }
        .distinctBy { it.doseCode }
        .sortedBy { it.sortOrder }
        .map { dose ->
            val indicatedDate = birthDate.plusMonths(dose.ageMonths.toLong())
            val record = appliedByCode[dose.doseCode]
            DoseWithStatus(
                dose = dose,
                status = if (record != null) DoseStatus.APPLIED else pendingStatus(dose, indicatedDate, today),
                indicatedDate = indicatedDate,
                applied = record,
            )
        }
    val currentCodes = doses.mapTo(mutableSetOf()) { it.dose.doseCode }
    return VaccinationStatus(doses = doses, otherApplied = firstApplied.filterNot { it.doseCode in currentCodes })
}

/** El estado de una dosis no aplicada, según la anticipación y la tolerancia de la dosis. */
private fun pendingStatus(dose: CalendarDose, indicatedDate: LocalDate, today: LocalDate): DoseStatus = when {
    today.isBefore(indicatedDate.minusDays(dose.leadDays.toLong())) -> DoseStatus.SCHEDULED
    today.isBefore(indicatedDate) -> DoseStatus.UPCOMING
    !today.isAfter(indicatedDate.plusDays(dose.toleranceDays.toLong())) -> DoseStatus.DUE
    else -> DoseStatus.OVERDUE
}
