package com.pasitos.app.feature.childonboarding

import com.pasitos.app.data.child.model.ChildSex
import java.time.LocalDate

/** El perfil se guardó. Después de un alta, [offerPreviousDoses] dice si ofrecer la carga retroactiva (RF-HIJ-01, CA2). */
data class SavedChild(val childId: String, val offerPreviousDoses: Boolean)

/** Formulario de alta y edición de un hijo (RF-HIJ-01 a 03). Los números se editan como texto. */
data class ChildFormUiState(
    /** Nulo en el alta; en la edición, el niño que se edita. */
    val childId: String? = null,
    val professionalPatient: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val sex: ChildSex? = null,
    val birthDate: LocalDate? = null,
    val gestationalWeeks: String = "",
    /** "No lo sé": se considera nacido a término (RN-HIJ-03). */
    val gestationalWeeksUnknown: Boolean = false,
    val birthWeightKg: String = "",
    val birthLengthCm: String = "",
    val birthHeadCircumferenceCm: String = "",
    val avatarKey: String? = null,
    val allergies: String = "",
    val relevantHistory: String = "",
    /** Solo en el alta (RF-HIJ-01). */
    val representationDeclared: Boolean = false,
    val errors: ChildFormErrors = ChildFormErrors(),
    /** Medidas fuera de rango que el tutor tiene que revisar o confirmar (RF-HIJ-01, CA4). */
    val implausibleMeasurements: List<String> = emptyList(),
    /** Pide confirmar que cambiarán percentiles, estados y vacunas (RF-HIJ-02, CA1). */
    val confirmRecalculation: Boolean = false,
    /** El alta y la edición solo funcionan en línea. */
    val isOnline: Boolean = true,
    /** En la edición, hasta que llegan los datos del niño desde la base local. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val generalError: String? = null,
    /** Para el selector de fecha: hoy, y el último día en que el niño tendría menos de 15 años. */
    val today: LocalDate = LocalDate.MIN,
    val minBirthDate: LocalDate = LocalDate.MIN,
) {
    val isEdit: Boolean get() = childId != null
}

/** Un mensaje por campo inválido; nulo si el campo está bien. */
data class ChildFormErrors(
    val firstName: String? = null,
    val lastName: String? = null,
    val sex: String? = null,
    val birthDate: String? = null,
    val gestationalWeeks: String? = null,
    val birthWeightKg: String? = null,
    val birthLengthCm: String? = null,
    val birthHeadCircumferenceCm: String? = null,
    val allergies: String? = null,
    val relevantHistory: String? = null,
    val representation: String? = null,
) {
    val any: Boolean
        get() = listOf(
            firstName, lastName, sex, birthDate, gestationalWeeks, birthWeightKg, birthLengthCm,
            birthHeadCircumferenceCm, allergies, relevantHistory, representation,
        ).any { it != null }
}
