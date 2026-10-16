package com.pasitos.backend.auth

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ConflictException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.LockedException
import com.pasitos.backend.common.UnauthorizedException
import com.pasitos.backend.common.tx
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.auth.AccountDto
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.MeResponse
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.RegisterRequest
import com.pasitos.shared.api.auth.TokenPair
import java.time.Duration
import java.time.Instant
import java.util.UUID

class InvalidCredentialsException : UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, "Email o contraseña incorrectos")

class EmailAlreadyInUseException : ConflictException(ErrorCode.EMAIL_ALREADY_IN_USE, "Ya existe una cuenta con este email")

class SessionExpiredException : UnauthorizedException(ErrorCode.UNAUTHORIZED, "Tu sesión venció. Iniciá sesión de nuevo")

class AccountLockedException(minutes: Long) : LockedException(
    ErrorCode.ACCOUNT_LOCKED,
    "Hubo demasiados intentos fallidos. Probá de nuevo en $minutes ${if (minutes == 1L) "minuto" else "minutos"}",
)

/** Autenticación de las cuentas de la app (11 · Backend §3, ADR-11). */
class AuthService(
    private val accounts: AccountRepository,
    private val sessions: SessionRepository,
    private val consents: ConsentRepository,
    private val jwt: JwtService,
    private val clock: ArgentinaClock,
    private val termsVersion: String,
) {

    suspend fun register(request: RegisterRequest): AuthResponse {
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()
        val email = request.email.trim().lowercase()
        if (firstName.isBlank() || lastName.isBlank()) throw InvalidInputException("Completá nombre y apellido")
        if (!EMAIL_FORMAT.matches(email)) throw InvalidInputException("Ingresá un email válido")
        validatePassword(request.password)
        if (!request.acceptTerms) {
            throw InvalidInputException("Para crear la cuenta tenés que aceptar los términos y la política de privacidad")
        }
        // bcrypt tarda unos 250 ms: se calcula antes de tomar una conexión de la base.
        val passwordHash = PasswordHasher.hash(request.password)

        return tx {
            if (accounts.findByEmail(email) != null) throw EmailAlreadyInUseException()
            val account = accounts.insertGuardian(email, firstName, lastName, passwordHash)
            consents.insertTerms(account.id, termsVersion, clock.now())
            AuthResponse(account.toDto(), openSession(account.id))
        }
    }

    suspend fun login(request: LoginRequest): AuthResponse =
        when (val result = checkCredentials(request.email, request.password)) {
            is LoginResult.Success -> AuthResponse(result.account.toDto(), result.tokens)
            LoginResult.InvalidCredentials -> throw InvalidCredentialsException()
            is LoginResult.Locked -> throw AccountLockedException(result.minutesLeft)
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
                sessions.findByPreviousRefreshToken(tokenHash)?.let { reused -> sessions.delete(reused.id) }
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
    }

    /**
     * Verifica email y contraseña con el bloqueo de RN-CTA-05: el quinto intento fallido seguido
     * bloquea el inicio con contraseña por 15 minutos. Los intentos fallidos se guardan aunque el
     * login falle, por eso el resultado se devuelve en vez de lanzar la excepción dentro de la
     * transacción. Con las credenciales correctas abre la sesión.
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

    private fun validatePassword(password: String) {
        // RN-CTA-01
        if (password.length < 8 || password.none { it.isLetter() } || password.none { it.isDigit() }) {
            throw InvalidInputException("La contraseña tiene que tener al menos 8 caracteres, con letras y números")
        }
    }

    private fun Account.toDto() = AccountDto(
        id = id.toString(),
        email = email,
        firstName = firstName,
        lastName = lastName,
        emailVerified = emailVerifiedAt != null,
        isGuardian = isGuardian,
        lastMode = lastMode,
    )

    private fun Duration.toMinutesRoundedUp(): Long = (toSeconds() + 59) / 60

    companion object {
        /** Token de acceso (ADR-11). */
        val ACCESS_TOKEN_LIFETIME: Duration = Duration.ofMinutes(15)

        /** La sesión vence tras 30 días sin uso; cada renovación extiende el plazo (RN-CTA-07). */
        val SESSION_LIFETIME: Duration = Duration.ofDays(30)

        /** RN-CTA-05 (valores propuestos en SUP-05). */
        const val MAX_FAILED_LOGINS = 5
        val LOCK_DURATION: Duration = Duration.ofMinutes(15)

        /** Solo descarta lo que claramente no es un email; la verificación real llega por correo (I1-B1). */
        private val EMAIL_FORMAT = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
