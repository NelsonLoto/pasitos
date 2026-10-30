package com.pasitos.backend.invitation

import com.pasitos.backend.auth.Account
import com.pasitos.backend.auth.AccountRepository
import com.pasitos.backend.auth.ConsentRepository
import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.common.AccessPolicy
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ChildAccess
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.ConflictException
import com.pasitos.backend.common.ForbiddenException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.NotFoundException
import com.pasitos.backend.common.tx
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.guardianship.GuardianshipRole
import com.pasitos.backend.mail.MailService
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.guardianship.AcceptInvitationRequest
import com.pasitos.shared.api.guardianship.InvitationResponse
import com.pasitos.shared.api.guardianship.InviteGuardianRequest
import com.pasitos.shared.api.guardianship.SentInvitationResponse
import java.time.Duration
import java.util.UUID

class AlreadyGuardianException : ConflictException(ErrorCode.ALREADY_GUARDIAN, "Ese email ya es tutor de este hijo/a")

class InvitationAlreadyPendingException : ConflictException(ErrorCode.INVITATION_ALREADY_PENDING, "Ya hay una invitación vigente para ese email")

class InvitationNotFoundException : NotFoundException(ErrorCode.INVITATION_NOT_FOUND, "No se encontró esa invitación")

class InvitationExpiredException : NotFoundException(ErrorCode.INVITATION_NOT_FOUND, "Esa invitación venció. Pedí que te inviten de nuevo")

/** RF-HIJ-05, RF-HIJ-06; RN-HIJ-07 (11 · Backend §2). */
class InvitationService(
    private val invitations: InvitationRepository,
    private val children: ChildRepository,
    private val guardianships: GuardianshipRepository,
    private val accounts: AccountRepository,
    private val consents: ConsentRepository,
    private val accessPolicy: AccessPolicy,
    private val mail: MailService,
    private val clock: ArgentinaClock,
    private val declarationVersion: String,
) {

    /** RF-HIJ-05: solo el tutor principal invita. */
    suspend fun invite(caller: Caller, childId: UUID, request: InviteGuardianRequest) {
        val email = request.email.trim().lowercase()
        if (!EMAIL_FORMAT.matches(email)) throw InvalidInputException("Ingresá un email válido")
        val now = clock.now()

        val result = tx {
            if (accessPolicy.require(caller, childId) != ChildAccess.PRINCIPAL_GUARDIAN) {
                throw ForbiddenException(ErrorCode.FORBIDDEN, "Solo el tutor principal puede invitar a otro tutor")
            }
            val child = children.findById(childId) ?: throw ChildNotFoundException()
            val invitedAccount = accounts.findByEmail(email)
            if (invitedAccount != null && guardianships.findRole(childId, invitedAccount.id) != null) {
                throw AlreadyGuardianException()
            }
            if (invitations.findPending(childId, email, InvitationType.GUARDIAN, now) != null) {
                throw InvitationAlreadyPendingException()
            }
            // La vencida deja de figurar en la lista del principal: la reemplaza la nueva (RF-HIJ-05, CA3)
            invitations.cancelExpired(childId, email, InvitationType.GUARDIAN, now)
            val inviter = accounts.findById(caller.accountId) ?: throw ChildNotFoundException()
            invitations.insert(InvitationType.GUARDIAN, childId, caller.accountId, email, now.plus(INVITATION_LIFETIME))
            Triple(child.fullName, inviter, invitedAccount)
        }
        val (childName, inviter, invitedAccount) = result
        if (invitedAccount != null) {
            mail.sendGuardianInvitation(email, invitedAccount.firstName, inviter.firstName, childName)
        } else {
            mail.sendGuardianInvitationNewAccount(email, inviter.firstName, childName)
        }
    }

    /** RF-HIJ-05, CA3: las invitaciones de tutor pendientes del niño, vigentes y vencidas. Solo el tutor principal las ve. */
    suspend fun listSent(caller: Caller, childId: UUID): List<SentInvitationResponse> = tx {
        if (accessPolicy.require(caller, childId) != ChildAccess.PRINCIPAL_GUARDIAN) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Solo el tutor principal puede ver las invitaciones enviadas")
        }
        val now = clock.now()
        invitations.listPendingForChild(childId, InvitationType.GUARDIAN).mapNotNull { invitation ->
            val email = invitation.email ?: return@mapNotNull null
            SentInvitationResponse(
                id = invitation.id.toString(),
                email = email,
                sentAt = invitation.sentAt,
                expiresAt = invitation.expiresAt,
                // El mismo corte que findPending: vigente mientras expires_at sea posterior a ahora
                expired = !invitation.expiresAt.isAfter(now),
            )
        }
    }

    /** RF-HIJ-06: invitaciones de tutor vigentes para la cuenta que pregunta. */
    suspend fun listMine(caller: Caller): List<InvitationResponse> = tx {
        val account = accounts.findById(caller.accountId) ?: return@tx emptyList()
        invitations.listPendingGuardianInvitations(account.email, clock.now()).map { it.toResponse() }
    }

    /** RF-HIJ-06, CA1: acepta y queda como tutor secundario. */
    suspend fun accept(caller: Caller, invitationId: UUID, request: AcceptInvitationRequest) {
        if (!request.representationDeclared) {
            throw InvalidInputException("Tenés que declarar que sos madre, padre o representante legal de este niño o niña")
        }
        val now = clock.now()
        val result = tx {
            val account = accounts.findById(caller.accountId) ?: throw InvitationNotFoundException()
            val invitation = loadMine(invitationId, account.email)
            if (invitation.expiresAt.isBefore(now)) throw InvitationExpiredException()
            invitations.markStatus(invitation.id, InvitationStatus.ACCEPTED, now)
            guardianships.insert(invitation.childId, caller.accountId, GuardianshipRole.SECONDARY)
            consents.insertGuardianDeclaration(caller.accountId, invitation.childId, declarationVersion, now)
            Pair(invitation, account)
        }
        notifyInviter(result.first) { inviter, childName -> mail.sendGuardianInvitationAccepted(inviter.email, inviter.firstName, result.second.firstName, childName) }
    }

    /** RF-HIJ-06, CA2. */
    suspend fun reject(caller: Caller, invitationId: UUID) {
        val now = clock.now()
        val result = tx {
            val account = accounts.findById(caller.accountId) ?: throw InvitationNotFoundException()
            val invitation = loadMine(invitationId, account.email)
            if (invitation.expiresAt.isBefore(now)) throw InvitationExpiredException()
            invitations.markStatus(invitation.id, InvitationStatus.REJECTED, now)
            Pair(invitation, account)
        }
        notifyInviter(result.first) { inviter, childName -> mail.sendGuardianInvitationRejected(inviter.email, inviter.firstName, result.second.firstName, childName) }
    }

    /** RF-HIJ-05, CA3; RN-HIJ-07: reenviar genera una invitación nueva con un plazo nuevo. */
    suspend fun resend(caller: Caller, invitationId: UUID) {
        val now = clock.now()
        val result = tx {
            val invitation = invitations.findById(invitationId) ?: throw InvitationNotFoundException()
            if (invitation.inviterAccountId != caller.accountId) throw InvitationNotFoundException()
            // Una aceptada, rechazada o cancelada ya no se reenvía: no se le escribe a quien ya es tutor
            if (invitation.status != InvitationStatus.PENDING) throw InvitationNotFoundException()
            if (accessPolicy.require(caller, invitation.childId) != ChildAccess.PRINCIPAL_GUARDIAN) {
                throw ForbiddenException(ErrorCode.FORBIDDEN, "Solo el tutor principal puede reenviar la invitación")
            }
            val email = invitation.email ?: throw InvitationNotFoundException()
            invitations.markStatus(invitation.id, InvitationStatus.CANCELLED, now)
            invitations.insert(InvitationType.GUARDIAN, invitation.childId, caller.accountId, email, now.plus(INVITATION_LIFETIME))
            val child = children.findById(invitation.childId) ?: throw ChildNotFoundException()
            val inviter = accounts.findById(caller.accountId) ?: throw ChildNotFoundException()
            Triple(email, child.fullName, Pair(inviter, accounts.findByEmail(email)))
        }
        val (email, childName, inviterAndInvitedAccount) = result
        val (inviter, invitedAccount) = inviterAndInvitedAccount
        if (invitedAccount != null) {
            mail.sendGuardianInvitation(email, invitedAccount.firstName, inviter.firstName, childName)
        } else {
            mail.sendGuardianInvitationNewAccount(email, inviter.firstName, childName)
        }
    }

    /** La invitación [id], solo si es de tutor y fue mandada al email de quien pregunta. */
    private fun loadMine(id: UUID, email: String): Invitation {
        val invitation = invitations.findById(id) ?: throw InvitationNotFoundException()
        if (invitation.type != InvitationType.GUARDIAN || invitation.email != email || invitation.status != InvitationStatus.PENDING) {
            throw InvitationNotFoundException()
        }
        return invitation
    }

    private suspend fun notifyInviter(invitation: Invitation, send: suspend (Account, String) -> Unit) {
        val (inviter, childName) = tx {
            val inviter = accounts.findById(invitation.inviterAccountId) ?: return@tx null
            val child = children.findById(invitation.childId) ?: return@tx null
            inviter to child.fullName
        } ?: return
        send(inviter, childName)
    }

    private fun Invitation.toResponse(): InvitationResponse {
        val child = children.findById(childId)
        val inviter = accounts.findById(inviterAccountId)
        return InvitationResponse(
            id = id.toString(),
            childId = childId.toString(),
            childFirstName = child?.firstName.orEmpty(),
            childLastName = child?.lastName.orEmpty(),
            inviterFirstName = inviter?.firstName.orEmpty(),
            inviterLastName = inviter?.lastName.orEmpty(),
            sentAt = sentAt,
            expiresAt = expiresAt,
        )
    }

    companion object {
        /** RN-HIJ-07. */
        val INVITATION_LIFETIME: Duration = Duration.ofDays(15)

        private val EMAIL_FORMAT = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
