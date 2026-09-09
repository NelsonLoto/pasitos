package com.ninosano.app.data.vaccination.model

enum class DoseStatus { Applied, Overdue, Pending }

/**
 * Referencia de una dosis del calendario. `recommendedAgeInDays` se usa para derivar el
 * estado (Aplicada/Pendiente/Atrasada) comparando contra la edad real del hijo.
 *
 * El set de dosis acá es el mismo subconjunto de ejemplo ya validado en el mockup de Figma
 * (BCG, Hepatitis B, Pentavalente x3, Neumococo, Antigripal) — no el Calendario Nacional de
 * Vacunación completo. RN04 pide que el calendario sea actualizable sin nueva versión de la
 * app (vía backend), así que no corresponde expandirlo a mano acá; se reemplaza por una fuente
 * real cuando exista el backend.
 */
data class VaccineDoseSchedule(
    val id: String,
    val vaccineName: String,
    val doseLabel: String?,
    val recommendedAgeLabel: String,
    val recommendedAgeInDays: Int,
)

data class AppliedDose(
    val childId: String,
    val doseScheduleId: String,
    val appliedDate: java.time.LocalDate,
)
