package com.pasitos.app.feature.othervaccine

import java.time.LocalDate

/** Las vacunas que D-68 dejó fuera del calendario: completan el nombre de un toque. */
val OtherVaccineSuggestions = listOf("Antigripal", "Fiebre amarilla", "Fiebre hemorrágica argentina")

/** Otra vacuna (RF-VAC-05): alta, o una existente para ver, editar o eliminar. */
data class OtherVaccineUiState(
    val isLoaded: Boolean = false,
    /** La vacuna o el niño ya no están, por ejemplo porque se eliminó en otro teléfono. */
    val notFound: Boolean = false,
    val isNew: Boolean = true,
    /** Solo el autor edita y elimina; los demás tutores la ven en modo lectura. */
    val isOwn: Boolean = true,
    val authorText: String? = null,
    val name: String = "",
    val date: LocalDate? = null,
    val lot: String = "",
    val note: String = "",
    val nameError: String? = null,
    val dateError: String? = null,
    val lotError: String? = null,
    val noteError: String? = null,
    /** El selector de fecha va del nacimiento a hoy (RN-VAC-02). */
    val minDate: LocalDate? = null,
    val maxDate: LocalDate = LocalDate.MAX,
    val isOnline: Boolean = true,
    val isSaving: Boolean = false,
    val confirmDelete: Boolean = false,
    val error: String? = null,
) {
    val isReadOnly: Boolean get() = !isNew && !isOwn
}
