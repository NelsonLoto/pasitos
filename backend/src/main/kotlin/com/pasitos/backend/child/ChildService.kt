package com.pasitos.backend.child

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.NotFoundException
import com.pasitos.backend.common.toLocalDateOrNull
import com.pasitos.backend.common.tx
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.guardianship.GuardianshipRole

class ChildNotFoundException : NotFoundException("child_not_found", "No se encontró ese hijo/a")

class ChildService(
    private val children: ChildRepository,
    private val guardianships: GuardianshipRepository,
    private val clock: ArgentinaClock,
) {

    suspend fun create(caller: Caller, request: CreateChildRequest): ChildResponse {
        val name = request.name.trim()
        if (name.isBlank()) throw InvalidInputException("Ingresá el nombre")
        val birthDate = request.birthDate.toLocalDateOrNull()
            ?: throw InvalidInputException("Fecha de nacimiento inválida")
        if (birthDate.isAfter(clock.today())) throw InvalidInputException("La fecha no puede ser futura")
        val sex = sexFromLegacyCode(request.sex) ?: throw InvalidInputException("Sexo inválido")

        return tx {
            // La app actual no pide apellido ni semanas de gestación: el nombre va entero a
            // first_name y las semanas quedan como "No lo sé" hasta el alta completa de I1.
            val child = children.insert(
                firstName = name,
                lastName = "",
                sex = sex,
                birthDate = birthDate,
                gestationalWeeks = null,
                createdBy = caller.accountId,
            )
            // Quien da de alta al niño queda como su tutor principal (RF-HIJ-01).
            guardianships.insert(child.id, caller.accountId, GuardianshipRole.PRINCIPAL)
            child.toResponse()
        }
    }

    suspend fun list(caller: Caller): List<ChildResponse> =
        tx { children.listForGuardian(caller.accountId).map { it.toResponse() } }
}
