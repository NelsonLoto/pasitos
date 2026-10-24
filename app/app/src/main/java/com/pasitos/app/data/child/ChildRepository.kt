package com.pasitos.app.data.child

import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildInput
import com.pasitos.app.data.child.model.GuardianRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Hijos del tutor (RF-HIJ-01 a 04, 09). Lee de la base local, que se alimenta de la sincronización.
 * El alta, la edición y la eliminación van por REST y solo funcionan en línea (12 · API): sin red
 * devuelven `NoConnectionException`.
 */
interface ChildRepository {

    /** Del mayor al menor (RF-HIJ-04). */
    val children: StateFlow<List<Child>>

    /**
     * El último hijo que eligió el tutor, también después de cerrar la app (RF-HIJ-04, CA3). Si ese
     * hijo ya no está, el primero de [children]; nulo si no hay hijos.
     */
    val activeChildId: StateFlow<String?>

    /** Rol del usuario sobre el niño (RN-HIJ-06), o nulo si todavía no llegó por la sincronización. */
    fun observeRole(childId: String): Flow<GuardianRole?>

    /** Alta (RF-HIJ-01): el usuario queda como tutor principal y el niño como hijo activo (CA1). */
    suspend fun addChild(input: ChildInput, representationDeclared: Boolean): Result<Child>

    /** Alta de paciente propio por un profesional, sin crear tutoría. */
    suspend fun addPatient(input: ChildInput): Result<Child>

    /** Edición del perfil, alergias y antecedentes (RF-HIJ-02, RF-HIJ-03). */
    suspend fun updateChild(childId: String, input: ChildInput): Result<Child>

    /** `true` si el usuario es el único tutor y eliminar borra todo el historial (RF-HIJ-09, RN-HIJ-08). */
    suspend fun isOnlyGuardian(childId: String): Result<Boolean>

    /**
     * Elimina el perfil o, si hay otros tutores, deja de ser tutor (RF-HIJ-09). En los dos casos el
     * niño y sus datos desaparecen del teléfono.
     */
    suspend fun deleteChild(childId: String): Result<Unit>

    /** Elige el hijo activo (RF-HIJ-04). Funciona sin conexión. */
    suspend fun selectChild(childId: String)
}
