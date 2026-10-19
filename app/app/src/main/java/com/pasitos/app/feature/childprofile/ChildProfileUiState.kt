package com.pasitos.app.feature.childprofile

import com.pasitos.app.data.child.model.Child
import java.text.Normalizer
import java.util.Locale

/** Perfil de un hijo, de solo lectura, con la edición y la eliminación (RF-HIJ-02, 03, 09). */
data class ChildProfileUiState(
    /** Nulo mientras se lee la base local, o si el niño ya no está (se eliminó o se perdió el acceso). */
    val child: Child? = null,
    val isLoaded: Boolean = false,
    val ageLabel: String = "",
    /** Si el usuario es el tutor principal, para explicar qué pasa con el rol al dejar de ser tutor (RF-HIJ-09, CA2). */
    val isPrincipal: Boolean = false,
    /** Editar y eliminar solo funcionan en línea. */
    val isOnline: Boolean = true,
    val deletion: DeletionStep = DeletionStep.None,
    /** El nombre que escribe el tutor para confirmar la eliminación (RF-HIJ-09, CA1). */
    val typedName: String = "",
    val isWorking: Boolean = false,
    val error: String? = null,
) {
    val canConfirmDeletion: Boolean
        get() = !isWorking && when (deletion) {
            DeletionStep.None -> false
            DeletionStep.ConfirmByName -> child != null && matchesFirstName(typedName, child.firstName)
            DeletionStep.ConfirmLeave -> true
        }
}

/** Qué confirmación muestra la eliminación, según lo que responde el servidor (RF-HIJ-09). */
enum class DeletionStep {
    None,

    /** Es el único tutor: se borra todo el historial y se confirma escribiendo el nombre (CA1). */
    ConfirmByName,

    /** Hay otros tutores: el perfil sigue para ellos y el usuario deja de ser tutor (CA2). */
    ConfirmLeave,
}

/**
 * Para confirmar alcanza con el nombre de pila, sin distinguir mayúsculas, tildes ni espacios
 * sobrantes: escribir "lucia" confirma a "Lucía". Busca que la acción sea intencional, no exigir
 * tildes en el teclado del teléfono.
 */
internal fun matchesFirstName(typed: String, firstName: String): Boolean {
    val expected = normalizeName(firstName)
    return expected.isNotEmpty() && normalizeName(typed) == expected
}

private val Diacritics = Regex("\\p{Mn}+")
private val Spaces = Regex("\\s+")

private fun normalizeName(text: String): String =
    Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replace(Diacritics, "")
        .replace(Spaces, " ")
        .lowercase(Locale.ROOT)
