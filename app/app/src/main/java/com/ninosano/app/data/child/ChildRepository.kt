package com.ninosano.app.data.child

import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.child.model.ChildSex
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de perfiles de hijos (RF02). La implementación actual (ver [FakeChildRepository])
 * es mock/local: el backend todavía no existe. Mismo patrón que [com.ninosano.app.data.auth.AuthRepository]
 * — el día que exista el backend, se agrega una implementación real y solo cambia el binding
 * en el módulo de Hilt correspondiente.
 */
interface ChildRepository {
    val children: StateFlow<List<Child>>

    suspend fun addChild(name: String, birthDate: LocalDate, sex: ChildSex): Result<Child>
}
