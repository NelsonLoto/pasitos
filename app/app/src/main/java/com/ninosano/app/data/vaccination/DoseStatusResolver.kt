package com.ninosano.app.data.vaccination

import com.ninosano.app.data.vaccination.model.DoseStatus
import com.ninosano.app.data.vaccination.model.VaccineDoseSchedule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Días de gracia antes de marcar una dosis no aplicada como "Atrasada" en vez de "Pendiente". */
private const val OVERDUE_GRACE_DAYS = 30

/** Regla de negocio compartida entre el Calendario de vacunación y el Detalle de vacuna. */
fun resolveDoseStatus(dose: VaccineDoseSchedule, childBirthDate: LocalDate?, isApplied: Boolean): DoseStatus = when {
    isApplied -> DoseStatus.Applied
    childBirthDate == null -> DoseStatus.Pending
    else -> {
        val ageInDays = ChronoUnit.DAYS.between(childBirthDate, LocalDate.now())
        if (ageInDays > dose.recommendedAgeInDays + OVERDUE_GRACE_DAYS) DoseStatus.Overdue else DoseStatus.Pending
    }
}
