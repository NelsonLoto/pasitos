package com.pasitos.backend.child

import java.util.UUID

/**
 * Lo que cada dominio borra cuando se elimina el perfil de un niño (RN-HIJ-08). Se registra en
 * `Dependencies.kt` y [ChildService.delete] lo llama dentro de su transacción, así el dominio de
 * hijos no conoce a los demás.
 *
 * El borrado es físico: el niño ya no está en el alcance de nadie y los teléfonos de quien lo
 * eliminó borran todo por `revokedChildIds`, así que nadie necesita lápidas.
 */
fun interface ChildDataPurger {
    /** Asume una transacción abierta. */
    fun purge(childId: UUID)
}
