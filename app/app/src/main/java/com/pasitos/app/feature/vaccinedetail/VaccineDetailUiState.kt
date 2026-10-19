package com.pasitos.app.feature.vaccinedetail

import com.pasitos.shared.vaccination.DoseStatus
import java.time.LocalDate

/** El registro de aplicación, si la dosis está aplicada (RF-VAC-07). */
data class AppliedInfoUi(
    /** La fecha, o "Fecha no registrada" en la carga retroactiva (RN-VAC-04). */
    val dateText: String,
    val lot: String?,
    val place: String?,
    /** "La registraste vos" o "La registró la familia" (40 · Modelo conceptual, Autoría). */
    val authorText: String,
    /** Solo el autor edita y elimina (RF-VAC-03). */
    val isOwn: Boolean,
)

/** El formulario para registrar o editar la aplicación (RF-VAC-02). */
data class DoseFormUi(
    val date: LocalDate? = null,
    val lot: String = "",
    val place: String = "",
    val dateError: String? = null,
    val lotError: String? = null,
    val placeError: String? = null,
)

/**
 * Detalle de una dosis (RF-VAC-07) con su registro (RF-VAC-02 y 03). Una dosis sin aplicar muestra
 * el formulario; una aplicada, sus datos, y si es propia se puede editar o eliminar.
 */
data class VaccineDetailUiState(
    val isLoaded: Boolean = false,
    /** El niño o la dosis ya no están, por ejemplo porque se perdió el acceso. */
    val notFound: Boolean = false,
    val vaccineName: String = "",
    val doseLabel: String = "",
    val status: DoseStatus? = null,
    /** "2 meses · 16/12/2025". Nulo en una dosis de un calendario anterior. */
    val indicatedText: String? = null,
    val prevents: String? = null,
    val source: String? = null,
    val applied: AppliedInfoUi? = null,
    val isEditing: Boolean = false,
    val form: DoseFormUi = DoseFormUi(),
    /** Falso al editar una dosis de carga retroactiva que todavía no tiene fecha (RF-VAC-04, CA3). */
    val dateRequired: Boolean = true,
    /** El selector de fecha va del nacimiento a hoy (RN-VAC-02). */
    val minDate: LocalDate? = null,
    val maxDate: LocalDate = LocalDate.MAX,
    /** Registrar, editar y eliminar solo funcionan en línea. */
    val isOnline: Boolean = true,
    val isSaving: Boolean = false,
    val confirmDelete: Boolean = false,
    val error: String? = null,
) {
    val showForm: Boolean get() = !notFound && (applied == null || isEditing)

    val isOverdue: Boolean get() = status == DoseStatus.OVERDUE
}
