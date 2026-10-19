package com.pasitos.backend.guardianship

import com.pasitos.backend.auth.AccountRepository
import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.child.ChildService
import com.pasitos.backend.common.AccessPolicy
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ChildAccess
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.ForbiddenException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.tx
import com.pasitos.backend.mail.MailService
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.guardianship.GuardianResponse
import com.pasitos.shared.api.guardianship.GuardianshipRole as GuardianshipRoleDto
import com.pasitos.shared.api.guardianship.TransferPrincipalRequest
import java.util.UUID

/** RF-HIJ-07, RF-HIJ-08, RF-HIJ-09 (11 · Backend §4). */
class GuardianshipService(
    private val guardianships: GuardianshipRepository,
    private val children: ChildRepository,
    private val accounts: AccountRepository,
    private val accessPolicy: AccessPolicy,
    private val childService: ChildService,
    private val mail: MailService,
    private val clock: ArgentinaClock,
) {

    /** RF-HIJ-07: cualquier tutor vigente puede ver la lista. */
    suspend fun list(caller: Caller, childId: UUID): List<GuardianResponse> = tx {
        accessPolicy.require(caller, childId)
        guardianships.listForChild(childId).map { guardianship ->
            val account = accounts.findById(guardianship.accountId) ?: throw ChildNotFoundException()
            GuardianResponse(
                accountId = account.id.toString(),
                firstName = account.firstName,
                lastName = account.lastName,
                email = account.email,
                role = guardianship.role.toDto(),
            )
        }
    }

    /**
     * RF-HIJ-09: si [accountId] es quien llama, abandona (reusa [ChildService.delete], que ya
     * resuelve la baja del único tutor vs. la cesión del rol principal, RN-HIJ-08 y CA2). Si es
     * otro tutor, solo el principal puede sacarlo, y solo a un tutor secundario.
     */
    suspend fun remove(caller: Caller, childId: UUID, accountId: UUID) {
        if (accountId == caller.accountId) {
            val result = tx {
                val guardiansBefore = guardianships.listForChild(childId)
                val principal = guardiansBefore.firstOrNull { it.role == GuardianshipRole.PRINCIPAL && it.accountId != caller.accountId }
                val leaver = accounts.findById(caller.accountId) ?: throw ChildNotFoundException()
                val child = children.findById(childId) ?: throw ChildNotFoundException()
                Triple(principal, leaver.firstName, child.fullName)
            }
            childService.delete(caller, childId)
            val (principal, leaverFirstName, childName) = result
            if (principal != null) {
                val principalAccount = tx { accounts.findById(principal.accountId) } ?: return
                mail.sendGuardianSelfAbandoned(principalAccount.email, principalAccount.firstName, leaverFirstName, childName)
            }
            return
        }

        val result = tx {
            if (accessPolicy.require(caller, childId) != ChildAccess.PRINCIPAL_GUARDIAN) {
                throw ForbiddenException(ErrorCode.FORBIDDEN, "Solo el tutor principal puede sacar a otro tutor")
            }
            if (guardianships.findRole(childId, accountId) != GuardianshipRole.SECONDARY) throw ChildNotFoundException()
            val removed = accounts.findById(accountId) ?: throw ChildNotFoundException()
            val child = children.findById(childId) ?: throw ChildNotFoundException()
            guardianships.softDelete(childId, accountId, clock.now())
            Pair(removed, child.fullName)
        }
        val (removed, childName) = result
        mail.sendGuardianRemoved(removed.email, removed.firstName, childName)
    }

    /**
     * RF-HIJ-08: cede el rol de principal a otro tutor vigente. Primero degrada a quien llama y
     * después asciende al destino, en ese orden (ver [GuardianshipRepository.demoteToSecondary]).
     */
    suspend fun transferPrincipal(caller: Caller, childId: UUID, request: TransferPrincipalRequest) {
        val newPrincipalId = request.accountId.toUuidOrThrow()
        if (newPrincipalId == caller.accountId) throw InvalidInputException("Elegí otro tutor para cederle el rol principal")

        val result = tx {
            if (accessPolicy.require(caller, childId) != ChildAccess.PRINCIPAL_GUARDIAN) {
                throw ForbiddenException(ErrorCode.FORBIDDEN, "Solo el tutor principal puede ceder su rol")
            }
            if (guardianships.findRole(childId, newPrincipalId) != GuardianshipRole.SECONDARY) {
                throw InvalidInputException("Ese tutor no está a cargo de este hijo/a")
            }
            guardianships.demoteToSecondary(childId, caller.accountId)
            guardianships.promoteToPrincipal(childId, newPrincipalId)
            val oldPrincipal = accounts.findById(caller.accountId) ?: throw ChildNotFoundException()
            val newPrincipal = accounts.findById(newPrincipalId) ?: throw ChildNotFoundException()
            val child = children.findById(childId) ?: throw ChildNotFoundException()
            Triple(oldPrincipal, newPrincipal, child.fullName)
        }
        val (oldPrincipal, newPrincipal, childName) = result
        mail.sendPrincipalTransferred(oldPrincipal.email, oldPrincipal.firstName, childName, becamePrincipal = false)
        mail.sendPrincipalTransferred(newPrincipal.email, newPrincipal.firstName, childName, becamePrincipal = true)
    }

    private fun String.toUuidOrThrow(): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw InvalidInputException("Ese tutor no existe")
        }

    private fun GuardianshipRole.toDto() = when (this) {
        GuardianshipRole.PRINCIPAL -> GuardianshipRoleDto.PRINCIPAL
        GuardianshipRole.SECONDARY -> GuardianshipRoleDto.SECONDARY
    }
}
