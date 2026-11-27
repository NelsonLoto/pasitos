package com.pasitos.backend.auth

import com.auth0.jwt.interfaces.Payload
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.auth.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.util.Locale
import java.util.UUID

data class Caller(val accountId: UUID, val sessionId: UUID)

class AuthService(
    private val database: Database,
    private val config: AuthConfig,
    val tokens: AuthTokens,
    private val passwords: PasswordHasher,
    private val clock: Clock,
    private val repository: AuthRepository = AuthRepository(),
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun register(request: RegisterRequest): AuthResponse {
        val fields = mutableMapOf<String, String>()
        val email = normalize(request.email)
        if (!email.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) fields["email"] = "Ingresá un email válido"
        if (request.firstName.isBlank()) fields["firstName"] = "Ingresá tu nombre"
        if (request.lastName.isBlank()) fields["lastName"] = "Ingresá tu apellido"
        if (request.password.codePointCount(0, request.password.length) < 8 ||
            !request.password.any(Char::isLetter) || !request.password.any(Char::isDigit)) {
            fields["password"] = "La contraseña debe tener al menos 8 caracteres, una letra y un número"
        }
        if (fields.isNotEmpty()) throw AuthFailure(ErrorCode.INVALID_INPUT, "Revisá los datos ingresados", fields)
        if (!request.acceptTerms || request.termsVersion != config.termsVersion) throw AuthFailure(ErrorCode.TERMS_REQUIRED, "Debés aceptar los términos vigentes y la política de privacidad")
        val hash = passwords.hash(request.password)
        try {
            return transaction(database) {
                if (repository.accountByEmail(email) != null) throw duplicateEmail()
                val id = UUID.randomUUID()
                repository.createAccount(id, request.firstName.trim(), request.lastName.trim(), email, hash)
                repository.consent(id, config.termsVersion, clock.instant())
                openSession(id, request.deviceLabel)
            }
        } catch (failure: ExposedSQLException) {
            if (failure.sqlState == "23505") throw duplicateEmail()
            throw failure
        }
    }

    fun login(request: LoginRequest): AuthResponse = transaction(database) {
        val account = repository.accountByEmail(normalize(request.email))
        val now = clock.instant()
        if (account?.lockedUntil?.isAfter(now) == true) return@transaction Result.failure(locked())
        if (!passwords.matches(request.password, account?.passwordHash)) {
            if (account != null) {
                val failures = if (account.lockedUntil != null) 1 else account.failed + 1
                repository.loginAttempt(account.id, failures,
                    if (failures >= 5) now.plus(Duration.ofMinutes(15)) else null, now)
            }
            // La transacción se confirma antes de lanzar el error: los intentos fallidos se conservan.
            return@transaction Result.failure(invalidCredentials())
        }
        repository.loginAttempt(account!!.id, 0, null, now)
        Result.success(openSession(account.id, request.deviceLabel))
    }.getOrThrow()

    fun refresh(request: RefreshRequest): AuthResponse = transaction(database) {
        val candidate = tokens.hash(request.refreshToken)
        val session = repository.sessionByToken(candidate)
            ?: return@transaction Result.failure(unauthorized())
        val now = clock.instant()
        if (!session.expiresAt.isAfter(now)) {
            repository.deleteSession(session.id)
            return@transaction Result.failure(unauthorized())
        }
        if (!MessageDigest.isEqual(candidate, session.hash)) {
            repository.deleteSession(session.id)
            logger.warn("Sesión revocada por reutilización: {}", session.id)
            return@transaction Result.failure(unauthorized())
        }
        val account = repository.account(session.accountId, config.termsVersion)
            ?: return@transaction Result.failure(unauthorized())
        val replacement = tokens.refresh()
        repository.rotate(session.id, session.hash, tokens.hash(replacement), now)
        Result.success(response(account, session.id, replacement))
    }.getOrThrow()

    fun validate(payload: Payload): Caller? {
        val subject = payload.subject ?: return null
        val sessionId = payload.getClaim("sid").asString() ?: return null
        val caller = try {
            Caller(UUID.fromString(subject), UUID.fromString(sessionId))
        } catch (_: IllegalArgumentException) { return null }
        return transaction(database) {
            val session = repository.sessionById(caller.sessionId) ?: return@transaction null
            if (session.accountId != caller.accountId || !session.expiresAt.isAfter(clock.instant())) return@transaction null
            repository.touch(session.id, clock.instant())
            caller
        }
    }

    fun me(caller: Caller): AccountResponse = transaction(database) {
        repository.account(caller.accountId, config.termsVersion) ?: throw unauthorized()
    }

    fun logout(caller: Caller) { transaction(database) { repository.deleteSession(caller.sessionId) } }

    private fun openSession(accountId: UUID, deviceLabel: String?): AuthResponse {
        val id = UUID.randomUUID()
        val refresh = tokens.refresh()
        repository.insertSession(id, accountId, tokens.hash(refresh), deviceLabel, clock.instant())
        return response(repository.account(accountId, config.termsVersion)!!, id, refresh)
    }

    private fun response(account: AccountResponse, sid: UUID, refresh: String): AuthResponse {
        val (access, expires) = tokens.access(UUID.fromString(account.id), sid)
        return AuthResponse(account, access, refresh, expires.toString())
    }

    private fun normalize(email: String) = email.trim().lowercase(Locale.ROOT)
    private fun duplicateEmail() = AuthFailure(ErrorCode.EMAIL_ALREADY_IN_USE, "El email ya está en uso")
    private fun locked() = AuthFailure(ErrorCode.ACCOUNT_LOCKED, "Demasiados intentos. Volvé a intentar en 15 minutos")
    private fun invalidCredentials() = AuthFailure(ErrorCode.INVALID_CREDENTIALS, "Email o contraseña incorrectos")
}

fun unauthorized() = AuthFailure(ErrorCode.UNAUTHORIZED, "La sesión no está vigente")
