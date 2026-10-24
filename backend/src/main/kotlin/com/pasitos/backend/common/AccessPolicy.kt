package com.pasitos.backend.common

import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.guardianship.GuardianshipRole
import com.pasitos.backend.child.PatientOwnershipRepository
import com.pasitos.shared.api.ErrorCode
import java.util.UUID

class ChildNotFoundException : NotFoundException(ErrorCode.CHILD_NOT_FOUND, "No se encontró ese hijo/a")

/**
 * Acceso de [Caller] a un niño (11 · Backend §4), incluidos pacientes propios del profesional.
 */
enum class ChildAccess { NONE, GUARDIAN, PRINCIPAL_GUARDIAN, CURRENT_PEDIATRICIAN }

/**
 * Matriz de permisos sobre datos de un niño (01 · Actores y permisos). Si el usuario no tiene
 * ningún acceso, [require] responde 404 en vez de 403, para no revelar que el niño existe.
 */
class AccessPolicy(
    private val guardianships: GuardianshipRepository,
    private val patientOwnership: PatientOwnershipRepository? = null,
) {

    fun accessTo(caller: Caller, childId: UUID): ChildAccess =
        when (guardianships.findRole(childId, caller.accountId)) {
            GuardianshipRole.PRINCIPAL -> ChildAccess.PRINCIPAL_GUARDIAN
            GuardianshipRole.SECONDARY -> ChildAccess.GUARDIAN
            null -> if (patientOwnership?.owns(childId, caller.accountId) == true) ChildAccess.CURRENT_PEDIATRICIAN else ChildAccess.NONE
        }

    /** Exige cualquier acceso (tutor principal o secundario); lanza [ChildNotFoundException] si no hay ninguno. */
    fun require(caller: Caller, childId: UUID): ChildAccess {
        val access = accessTo(caller, childId)
        if (access == ChildAccess.NONE) throw ChildNotFoundException()
        return access
    }
}
