package com.ninosano.app.data.child

import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.child.model.ChildSex
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de perfiles de hijos (RF02). Implementación real: [ChildApiRepository], contra el
 * backend Ktor (`POST /children`, `GET /children`, protegidas por JWT — un hijo pertenece al
 * tutor autenticado). `selectChild` es puro estado local, no hay concepto de "hijo activo" en
 * el servidor.
 */
interface ChildRepository {
    val children: StateFlow<List<Child>>

    /** Hijo activo (RF03) — el que se muestra en Home y en el resto de las pantallas. */
    val activeChildId: StateFlow<String?>

    suspend fun addChild(name: String, birthDate: LocalDate, sex: ChildSex): Result<Child>

    fun selectChild(childId: String)
}
