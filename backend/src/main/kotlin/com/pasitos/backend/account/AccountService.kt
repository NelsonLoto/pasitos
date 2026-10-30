package com.pasitos.backend.account

import com.pasitos.backend.auth.Account
import com.pasitos.backend.auth.AccountRepository
import com.pasitos.backend.auth.ConsentRepository
import com.pasitos.backend.auth.EmailAlreadyInUseException
import com.pasitos.backend.auth.EmailTokenPurpose
import com.pasitos.backend.auth.EmailTokenRepository
import com.pasitos.backend.auth.OpaqueToken
import com.pasitos.backend.auth.PasswordHasher
import com.pasitos.backend.auth.PasswordPolicy
import com.pasitos.backend.auth.SessionExpiredException
import com.pasitos.backend.auth.SessionRepository
import com.pasitos.backend.auth.toDto
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.UnauthorizedException
import com.pasitos.backend.common.tx
import com.pasitos.backend.mail.MailService
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.account.ChangePasswordRequest
import com.pasitos.shared.api.account.EmailChangeRequest
import com.pasitos.shared.api.account.PatchMeRequest
import com.pasitos.shared.api.auth.AccountDto
import java.time.Duration

/** RF-CTA-11, CA2: la contraseña actual no coincide. */
class InvalidCurrentPasswordException : UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, "La contraseña actual no es correcta")

/**
 * Edición de la cuenta propia: nombre, contraseña y email (RF-CTA-09 CA1, RF-CTA-11, RN-CTA-02;
 * 11 · Backend §3). `GET /me` queda en `AuthService` (I0-05): no cambió con esta tarea.
 */
class AccountService(
    private val accounts: AccountRepository,
    private val sessions: SessionRepository,
    private val consents: ConsentRepository,
    private val emailTokens: EmailTokenRepository,
    private val mail: MailService,
    private val clock: ArgentinaClock,
    private val termsVersion: String,
) {

    /** RF-CTA-11, CA1 (nombre y apellido; el email tiene su propio camino, con verificación). */
    suspend fun patchMe(caller: Caller, request: PatchMeRequest): AccountDto {
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()
        if (firstName.isBlank() || lastName.isBlank()) throw InvalidInputException("Completá nombre y apellido")
        return tx {
            accounts.updateName(caller.accountId, firstName, lastName)
            (accounts.findById(caller.accountId) ?: throw SessionExpiredException()).toDto()
        }
    }

    /** RF-CTA-11, CA2: pide la contraseña actual y cierra las demás sesiones, no la que la pidió. */
    suspend fun changePassword(caller: Caller, request: ChangePasswordRequest) {
        if (!PasswordPolicy.isValid(request.newPassword)) {
            throw InvalidInputException("La contraseña tiene que tener al menos 8 caracteres, con letras y números")
        }
        // bcrypt tarda unos 250 ms: se calcula antes de tomar una conexión de la base.
        val newPasswordHash = PasswordHasher.hash(request.newPassword)
        tx {
            val account = accounts.findById(caller.accountId) ?: throw SessionExpiredException()
            val hash = account.passwordHash
            if (hash == null || !PasswordHasher.verify(request.currentPassword, hash)) {
                throw InvalidCurrentPasswordException()
            }
            accounts.updatePassword(caller.accountId, newPasswordHash)
            sessions.deleteAllForAccountExcept(caller.accountId, caller.sessionId)
        }
    }

    /**
     * RF-CTA-11, CA1: pide la contraseña actual y manda la verificación al email nuevo. El cambio
     * recién se aplica cuando se confirma ([confirmEmailChange]); hasta entonces el email actual
     * sigue siendo el vigente.
     */
    suspend fun requestEmailChange(caller: Caller, request: EmailChangeRequest) {
        val newEmail = request.newEmail.trim().lowercase()
        if (!EMAIL_FORMAT.matches(newEmail)) throw InvalidInputException("Ingresá un email válido")
        val token = OpaqueToken.generate()
        val account = tx {
            val account = accounts.findById(caller.accountId) ?: throw SessionExpiredException()
            val hash = account.passwordHash
            if (hash == null || !PasswordHasher.verify(request.password, hash)) {
                throw InvalidCurrentPasswordException()
            }
            if (newEmail == account.email) throw InvalidInputException("Ese ya es tu email actual")
            if (accounts.findByEmail(newEmail) != null) throw EmailAlreadyInUseException()
            emailTokens.insert(caller.accountId, EmailTokenPurpose.CHANGE_EMAIL, OpaqueToken.hash(token), clock.now().plus(EMAIL_CHANGE_LIFETIME), newEmail)
            account
        }
        mail.sendEmailChangeVerification(newEmail, account.firstName, token)
    }

    /** `GET /web/confirm-email-change` (RF-CTA-11, CA1): recién acá se reemplaza el email anterior. */
    suspend fun confirmEmailChange(token: String): Boolean {
        val now = clock.now()
        val result = tx {
            val row = emailTokens.findActive(OpaqueToken.hash(token), EmailTokenPurpose.CHANGE_EMAIL, now) ?: return@tx null
            val newEmail = row.newEmail ?: return@tx null
            val account = accounts.findById(row.accountId) ?: return@tx null
            emailTokens.markUsed(row.id, now)
            accounts.updateEmail(row.accountId, newEmail)
            Pair(account, newEmail)
        } ?: return false
        val (account, newEmail) = result
        mail.sendEmailChangeNotice(account.email, account.firstName, newEmail)
        return true
    }

    /** RN-CTA-02: acepta la versión vigente de los términos. */
    suspend fun acceptConsents(caller: Caller) {
        tx { consents.insertTerms(caller.accountId, termsVersion, clock.now()) }
    }

    companion object {
        /** Sin RN explícita para este caso: mismo plazo que verificar un email recién creado (RN-CTA-06). */
        val EMAIL_CHANGE_LIFETIME: Duration = Duration.ofHours(24)

        private val EMAIL_FORMAT = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
