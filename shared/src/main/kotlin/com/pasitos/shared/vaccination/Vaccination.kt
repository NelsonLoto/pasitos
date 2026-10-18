package com.pasitos.shared.vaccination

import java.time.LocalDate

/** Estado de una dosis del calendario para un niño (RN-VAC-01). */
enum class DoseStatus {
    /** Programada: no aplicada y su fecha indicada está a más días que la anticipación (SUP-19). */
    SCHEDULED,

    /** Próxima: no aplicada y faltan tantos días como la anticipación, o menos. */
    UPCOMING,

    /** Pendiente: no aplicada, desde la fecha indicada hasta el fin de la tolerancia. */
    DUE,

    /** Atrasada: no aplicada y pasó la tolerancia. */
    OVERDUE,

    /** Aplicada: tiene registro de aplicación, con fecha o sin ella (RN-VAC-04). */
    APPLIED,
}

/**
 * Una dosis de una versión del Calendario Nacional.
 *
 * [doseCode] identifica la dosis y no cambia entre versiones (RN-VAC-03): con él se reconoce una
 * dosis aplicada. La fecha indicada es el nacimiento más [ageMonths] (RN-VAC-08, edad
 * cronológica). [leadDays] y [toleranceDays] son la anticipación y la tolerancia de RN-VAC-01.
 *
 * [bornFrom] y [bornUntil] limitan la dosis a los nacidos en ese rango, ambos inclusive (RN-VAC-10).
 * Un mismo [doseCode] puede tener varias filas con rangos que no se superpongan, cada una con su
 * edad: así cambia la edad de una dosis según la cohorte.
 */
data class CalendarDose(
    val doseCode: String,
    val vaccineName: String,
    val doseLabel: String,
    val ageMonths: Int,
    val leadDays: Int,
    val toleranceDays: Int,
    val sortOrder: Int,
    val bornFrom: LocalDate? = null,
    val bornUntil: LocalDate? = null,
) {
    /** Si la dosis corresponde a un niño nacido el [birthDate]. */
    fun appliesTo(birthDate: LocalDate): Boolean =
        (bornFrom == null || !birthDate.isBefore(bornFrom)) && (bornUntil == null || !birthDate.isAfter(bornUntil))
}

/** Una dosis registrada. [appliedOn] nulo es la carga retroactiva sin fecha (RN-VAC-04). */
data class AppliedDose(
    val doseCode: String,
    val appliedOn: LocalDate?,
)

/** Una dosis del calendario vigente con su estado para un niño. */
data class DoseWithStatus(
    val dose: CalendarDose,
    val status: DoseStatus,
    val indicatedDate: LocalDate,
    /** El registro de aplicación, si [status] es [DoseStatus.APPLIED]. */
    val applied: AppliedDose? = null,
)

/**
 * El calendario de un niño. [otherApplied] son las dosis registradas que no corresponden a ninguna
 * dosis vigente para ese niño, por ejemplo porque una versión nueva del calendario la sacó: se
 * conservan tal como se registraron (RN-VAC-03).
 */
data class VaccinationStatus(
    val doses: List<DoseWithStatus>,
    val otherApplied: List<AppliedDose>,
)
