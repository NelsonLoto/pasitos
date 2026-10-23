package com.pasitos.backend.auth

import com.pasitos.backend.auth.google.GoogleTokenVerifier
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ConflictException
import com.pasitos.backend.common.ForbiddenException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.LockedException
import com.pasitos.backend.common.UnauthorizedException
import com.pasitos.backend.common.tx
import com.pasitos.backend.mail.MailService
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.EmailVerificationRequest
import com.pasitos.shared.api.auth.GoogleAuthRequest
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.MeResponse
import com.pasitos.shared.api.auth.PasswordResetRequest
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.RegisterRequest
import com.pasitos.shared.api.auth.TokenPair
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory

class InvalidCredentialsException : UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, "Email o contraseña incorrectos")

class EmailAlreadyInUseException : ConflictException(ErrorCode.EMAIL_ALREADY_IN_USE, "Ya existe una cuenta con este email")

class SessionExpiredException : UnauthorizedException(ErrorCode.UNAUTHORIZED, "Tu sesión venció. Iniciá sesión de nuevo")

class AccountLockedException(minutes: Long) : LockedException(
    ErrorCode.ACCOUNT_LOCKED,
    "Hubo demasiados intentos fallidos. Probá de nuevo en $minutes ${if (minutes == 1L) "minuto" else "minutos"}",
)

/** RF-CTA-02, CA1: credenciales correctas, pero hay que verificar el email antes de entrar. */
class EmailNotVerifiedException : ForbiddenException(ErrorCode.EMAIL_NOT_VERIFIED, "Tenés que verificar tu email antes de continuar")

/** RF-CTA-03, CA1: primer ingreso con Google. No se guardó nada hasta que acepte los términos. */
class TermsRequiredException : ConflictException(
    ErrorCode.TERMS_REQUIRED,
    "Para crear la cuenta tenés que aceptar los términos y la política de privacidad",
)

/** `POST /web/reset-password` (RF-CTA-05). */
sealed interface ResetPasswordResult {
    data object Success : ResetPasswordResult
    data object WeakPassword : ResetPasswordResult
    data object InvalidToken : ResetPasswordResult
}

/** Autenticación de las cuentas de la app (11 · Backend §3, ADR-11). */
class AuthService(
    private val accounts: AccountRepository,
    private val sessions: SessionRepository,
    private val consents: ConsentRepository,
    private val emailTokens: EmailTokenRepository,
    private val jwt: JwtService,
    private val google: GoogleTokenVerifier,
    private val mail: MailService,
    private val clock: ArgentinaClock,
    private val termsVersion: String,
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun register(request: RegisterRequest): AuthResponse {
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()
        val email = request.email.trim().lowercase()
        if (firstName.isBlank() || lastName.isBlank()) throw InvalidInputException("Completá nombre y apellido")
        if (!EMAIL_FORMAT.matches(email)) throw InvalidInputException("Ingresá un email válido")
        requireValidPassword(request.password)
        if (!request.acceptTerms) {
            throw InvalidInputException("Para crear la cuenta tenés que aceptar los términos y la política de privacidad")
        }
        // bcrypt tarda unos 250 ms: se calcula antes de tomar una conexión de la base.
        val passwordHash = PasswordHasher.hash(request.password)
        val verificationToken = OpaqueToken.generate()

        val result = tx {
            if (accounts.findByEmail(email) != null) throw EmailAlreadyInUseException()
            val account = accounts.insertGuardian(email, firstName, lastName, passwordHash)
            consents.insertTerms(account.id, termsVersion, clock.now())
            emailTokens.insert(account.id, EmailTokenPurpose.VERIFY_EMAIL, OpaqueToken.hash(verificationToken), clock.now().plus(VERIFY_EMAIL_LIFETIME))
            AuthResponse(account.toDto(), openSession(account.id))
        }
        // RF-CTA-01, CA1: el envío va fuera de la transacción (17 · Notificaciones y emails §3).
        mail.sendVerificationEmail(email, firstName, verificationToken)
        return result
    }

    suspend fun login(request: LoginRequest): AuthResponse =
        when (val result = checkCredentials(request.email, request.password)) {
            is LoginResult.Success -> AuthResponse(result.account.toDto(), result.tokens)
            LoginResult.InvalidCredentials -> throw InvalidCredentialsException()
            is LoginResult.Locked -> throw AccountLockedException(result.minutesLeft)
            LoginResult.EmailNotVerified -> throw EmailNotVerifiedException()
        }

    /**
     * RF-CTA-03 y RN-CTA-04. El token se verifica antes de abrir la transacción, porque puede hacer
     * falta bajar las claves de Google. La cuenta se busca primero por la cuenta de Google, que no
     * cambia aunque cambie el email, y después por el email. Como el inicio con contraseña no
     * interviene, el bloqueo por intentos fallidos no aplica (RN-CTA-05).
     *
     * Si dos primeros ingresos con la misma cuenta de Google llegan juntos, el segundo choca con el
     * índice único de `accounts`. Exposed reintenta la transacción, y en el reintento encuentra la
     * cuenta que creó el primero.
     */
    suspend fun loginWithGoogle(request: GoogleAuthRequest): AuthResponse {
        val identity = google.verify(request.idToken)
        return tx {
            val now = clock.now()
            accounts.findByGoogleSubject(identity.subject)?.let { account ->
                return@tx AuthResponse(account.toDto(), openSession(account.id))
            }

            val existing = accounts.findByEmailForUpdate(identity.email)
            if (existing != null) {
                // Otra cuenta de Google ya usó este email, por ejemplo una dirección de una
                // organización que se reasignó a otra persona: no se entra a la cuenta de la anterior.
                if (existing.googleSubject != null) throw EmailAlreadyInUseException()
                if (existing.emailVerifiedAt == null) {
                    // La contraseña y las sesiones pudo crearlas otra persona que registró este
                    // email antes que su dueño (secuestro previo de la cuenta): no sobreviven.
                    sessions.deleteAllForAccount(existing.id)
                    accounts.linkGoogleReplacingPassword(existing.id, identity.subject, now)
                } else {
                    accounts.linkGoogle(existing.id, identity.subject)
                }
                val linked = accounts.findById(existing.id) ?: error("La cuenta ${existing.id} desapareció dentro de la transacción")
                return@tx AuthResponse(linked.toDto(), openSession(linked.id))
            }

            // Primer ingreso: nada se guarda hasta que acepte los términos (RN-CTA-02).
            if (!request.acceptTerms) throw TermsRequiredException()
            val firstName = request.firstName?.trim().orEmpty()
            val lastName = request.lastName?.trim().orEmpty()
            if (firstName.isBlank() || lastName.isBlank()) throw InvalidInputException("Completá nombre y apellido")
            val account = accounts.insertGoogleGuardian(identity.email, firstName, lastName, identity.subject, now)
            consents.insertTerms(account.id, termsVersion, now)
            AuthResponse(account.toDto(), openSession(account.id))
        }
    }

    /** RF-CTA-02, CA1: reenvía el link de verificación si la cuenta existe y no está verificada. */
    suspend fun resendEmailVerification(request: EmailVerificationRequest) {
        val email = request.email.trim().lowercase()
        val token = OpaqueToken.generate()
        val toSend = tx {
            val account = accounts.findByEmail(email) ?: return@tx null
            if (account.emailVerifiedAt != null) return@tx null
            emailTokens.insert(account.id, EmailTokenPurpose.VERIFY_EMAIL, OpaqueToken.hash(token), clock.now().plus(VERIFY_EMAIL_LIFETIME))
            account
        } ?: return
        mail.sendVerificationEmail(toSend.email, toSend.firstName, token)
    }

    /** RF-CTA-05, CA1: misma respuesta exista o no la cuenta, para no revelar qué emails están registrados. */
    suspend fun requestPasswordReset(request: PasswordResetRequest) {
        val email = request.email.trim().lowercase()
        val token = OpaqueToken.generate()
        val toSend = tx {
            val account = accounts.findByEmail(email) ?: return@tx null
            emailTokens.insert(account.id, EmailTokenPurpose.RESET_PASSWORD, OpaqueToken.hash(token), clock.now().plus(RESET_PASSWORD_LIFETIME))
            account
        } ?: return
        mail.sendPasswordResetEmail(toSend.email, toSend.firstName, token)
    }

    /** `GET /web/verify-email` (RF-CTA-02, CA2 y CA3). */
    suspend fun verifyEmail(token: String): Boolean = tx {
        val now = clock.now()
        val row = emailTokens.findActive(OpaqueToken.hash(token), EmailTokenPurpose.VERIFY_EMAIL, now) ?: return@tx false
        emailTokens.markUsed(row.id, now)
        accounts.markEmailVerified(row.accountId, now)
        true
    }

    /** `GET /web/reset-password`: si el link sigue vigente, antes de mostrar el formulario. */
    suspend fun isPasswordResetTokenValid(token: String): Boolean = tx {
        emailTokens.findActive(OpaqueToken.hash(token), EmailTokenPurpose.RESET_PASSWORD, clock.now()) != null
    }

    /** `POST /web/reset-password` (RF-CTA-05, CA2 y CA3). */
    suspend fun resetPassword(token: String, newPassword: String): ResetPasswordResult {
        if (!PasswordPolicy.isValid(newPassword)) return ResetPasswordResult.WeakPassword
        // bcrypt tarda unos 250 ms: se calcula antes de tomar una conexión de la base.
        val passwordHash = PasswordHasher.hash(newPassword)
        val now = clock.now()
        return tx {
            val row = emailTokens.findActive(OpaqueToken.hash(token), EmailTokenPurpose.RESET_PASSWORD, now)
                ?: return@tx ResetPasswordResult.InvalidToken
            emailTokens.markUsed(row.id, now)
            accounts.updatePassword(row.accountId, passwordHash)
            sessions.deleteAllForAccount(row.accountId)
            ResetPasswordResult.Success
        }
    }

    /**
     * Entrega un par de tokens nuevo e invalida el token de renovación recibido (rotación). Si llega
     * un token que ya se usó, alguien más lo tiene: se revoca la sesión completa (ADR-11).
     */
    suspend fun refresh(request: RefreshRequest): TokenPair {
        val tokenHash = OpaqueToken.hash(request.refreshToken)
        val now = clock.now()
        // Las revocaciones se confirman antes de responder 401: por eso se devuelve null en vez de
        // lanzar la excepción dentro de la transacción, que la desharía.
        val tokens = tx {
            val session = sessions.findByRefreshTokenForUpdate(tokenHash)
            if (session == null) {
                sessions.findByPreviousRefreshToken(tokenHash)?.let { reused ->
                    sessions.delete(reused.id)
                    // Puede ser un robo o una renovación cuya respuesta se perdió: se registra para
                    // medir cuánto pasa (15 · App Android §4). Solo el ID, sin datos personales.
                    logger.warn("Sesión {} revocada: llegó un token de renovación ya rotado", reused.id)
                }
                return@tx null
            }
            if (!session.expiresAt.isAfter(now)) {
                sessions.delete(session.id)
                return@tx null
            }
            val refreshToken = OpaqueToken.generate()
            sessions.rotate(session.id, OpaqueToken.hash(refreshToken), tokenHash, now.plus(SESSION_LIFETIME), now)
            tokenPair(session.accountId, session.id, refreshToken, now)
        }
        return tokens ?: throw SessionExpiredException()
    }

    suspend fun logout(caller: Caller) {
        tx { sessions.delete(caller.sessionId) }
    }

    suspend fun me(caller: Caller): MeResponse = tx {
        val account = accounts.findById(caller.accountId) ?: throw SessionExpiredException()
        val acceptedVersion = consents.latestTermsVersion(account.id)
        MeResponse(
            account = account.toDto(),
            pendingTermsVersion = termsVersion.takeIf { it != acceptedVersion },
        )
    }

    /** Se llama en cada request autenticado: revocar una sesión invalida su token al instante. */
    suspend fun isSessionActive(sessionId: UUID): Boolean =
        tx { sessions.isActive(sessionId, clock.now()) }

    private sealed interface LoginResult {
        data class Success(val account: Account, val tokens: TokenPair) : LoginResult
        data object InvalidCredentials : LoginResult
        data class Locked(val minutesLeft: Long) : LoginResult
        data object EmailNotVerified : LoginResult
    }

    /**
     * Verifica email y contraseña con el bloqueo de RN-CTA-05: el quinto intento fallido seguido
     * bloquea el inicio con contraseña por 15 minutos. Los intentos fallidos se guardan aunque el
     * login falle, por eso el resultado se devuelve en vez de lanzar la excepción dentro de la
     * transacción. Con las credenciales correctas y el email verificado abre la sesión; sin
     * verificar (RF-CTA-02, CA1) no abre sesión, pero tampoco cuenta como intento fallido.
     */
    private suspend fun checkCredentials(rawEmail: String, password: String): LoginResult {
        val email = rawEmail.trim().lowercase()
        val now = clock.now()
        return tx {
            val account = accounts.findByEmailForUpdate(email) ?: return@tx LoginResult.InvalidCredentials
            val lockedUntil = account.lockedUntil
            if (lockedUntil != null && lockedUntil.isAfter(now)) {
                return@tx LoginResult.Locked(minutesLeft = Duration.between(now, lockedUntil).toMinutesRoundedUp())
            }
            val hash = account.passwordHash ?: return@tx LoginResult.InvalidCredentials
            if (PasswordHasher.verify(password, hash)) {
                if (account.failedLoginCount > 0 || lockedUntil != null) {
                    accounts.updateLoginFailures(account.id, failedLoginCount = 0, lockedUntil = null)
                }
                if (account.emailVerifiedAt == null) return@tx LoginResult.EmailNotVerified
                return@tx LoginResult.Success(account, openSession(account.id))
            }
            val failures = account.failedLoginCount + 1
            if (failures >= MAX_FAILED_LOGINS) {
                accounts.updateLoginFailures(account.id, failedLoginCount = 0, lockedUntil = now.plus(LOCK_DURATION))
            } else {
                accounts.updateLoginFailures(account.id, failedLoginCount = failures, lockedUntil = null)
            }
            LoginResult.InvalidCredentials
        }
    }

    /** Asume una transacción abierta. */
    private fun openSession(accountId: UUID): TokenPair {
        val now = clock.now()
        val sessionId = UUID.randomUUID()
        val refreshToken = OpaqueToken.generate()
        sessions.insert(sessionId, accountId, OpaqueToken.hash(refreshToken), now.plus(SESSION_LIFETIME), now)
        return tokenPair(accountId, sessionId, refreshToken, now)
    }

    private fun tokenPair(accountId: UUID, sessionId: UUID, refreshToken: String, now: Instant): TokenPair {
        val access = jwt.generateToken(accountId, sessionId, now, ACCESS_TOKEN_LIFETIME)
        return TokenPair(access.token, access.expiresAt, refreshToken)
    }

    private fun requireValidPassword(password: String) {
        if (!PasswordPolicy.isValid(password)) {
            throw InvalidInputException("La contraseña tiene que tener al menos 8 caracteres, con letras y números")
        }
    }

    private fun Duration.toMinutesRoundedUp(): Long = (toSeconds() + 59) / 60

    companion object {
        /** Token de acceso (ADR-11). */
        val ACCESS_TOKEN_LIFETIME: Duration = Duration.ofMinutes(15)

        /** La sesión vence tras 30 días sin uso; cada renovación extiende el plazo (RN-CTA-07). */
        val SESSION_LIFETIME: Duration = Duration.ofDays(30)

        /** RN-CTA-05 (valores propuestos en SUP-05). */
        const val MAX_FAILED_LOGINS = 5
        val LOCK_DURATION: Duration = Duration.ofMinutes(15)

        /** RN-CTA-06 (valores propuestos en SUP-04). */
        val VERIFY_EMAIL_LIFETIME: Duration = Duration.ofHours(24)
        val RESET_PASSWORD_LIFETIME: Duration = Duration.ofMinutes(60)

        /** Solo descarta lo que claramente no es un email; la verificación real llega por correo. */
        private val EMAIL_FORMAT = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
