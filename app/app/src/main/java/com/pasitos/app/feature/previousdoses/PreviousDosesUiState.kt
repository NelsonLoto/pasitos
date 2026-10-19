package com.pasitos.app.feature.previousdoses

/** Los pasos de la carga retroactiva (RF-VAC-04). */
enum class PreviousDosesStep {
    /** "¿Tiene las vacunas al día para su edad?" */
    Question,

    /** La lista para marcar: con "Sí" viene toda marcada; con "No" o "No sé", sin marcar (CA1 y CA2). */
    Checklist,

    /** Guardadas: se pueden completar fecha, lote y lugar desde cada dosis (CA3). */
    Done,
}

data class PreviousDoseUi(val doseCode: String, val vaccineName: String, val doseLabel: String)

data class PreviousDoseGroupUi(val label: String, val doses: List<PreviousDoseUi>)

data class PreviousDosesUiState(
    val isLoaded: Boolean = false,
    val childName: String = "",
    /** Las dosis anteriores a hoy sin registrar, agrupadas por edad como en el calendario. */
    val groups: List<PreviousDoseGroupUi> = emptyList(),
    val step: PreviousDosesStep = PreviousDosesStep.Question,
    val checked: Set<String> = emptySet(),
    /** Después del alta de un hijo se puede saltear (RF-HIJ-01, CA2). */
    val afterChildCreated: Boolean = false,
    val isOnline: Boolean = true,
    val isSaving: Boolean = false,
    val savedCount: Int = 0,
    val error: String? = null,
) {
    val total: Int get() = groups.sumOf { it.doses.size }

    val canSave: Boolean get() = checked.isNotEmpty() && isOnline && !isSaving
}
