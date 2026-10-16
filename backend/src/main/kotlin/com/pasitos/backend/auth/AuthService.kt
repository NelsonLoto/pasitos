package com.pasitos.backend.auth

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ConflictException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.UnauthorizedException
import com.pasitos.backend.common.tx
import com.pasitos.shared.api.ErrorCode
import java.util.UUID

class InvalidCredentialsException : UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, "Email o contraseña incorrectos")

class EmailAlreadyInUseException : ConflictException(ErrorCode.EMAIL_ALREADY_IN_USE, "Ya existe una cuenta con este email")

class AuthService(
    private val accounts: AccountRepository,
    private val sessions: SessionRepository,
    private val jwt: JwtService,
    private val clock: ArgentinaClock,
) {

    suspend fun register(request: RegisterRequest): AuthResponse {
        val name = request.name.trim()
        val email = request.email.trim().lowercase()
        if (name.isBlank() || email.isBlank() || request.password.length < 6) {
            throw InvalidInputException("Completá nombre, email y una contraseña de al menos 6 caracteres")
        }
        // bcrypt tarda unos 250 ms: se calcula antes de tomar una conexión de la base.
        val passwordHash = PasswordHasher.hash(request.password)

        return tx {
            if (accounts.findByEmail(email) != null) throw EmailAlreadyInUseException()
            // La app actual pide un solo campo de nombre: va entero a first_name hasta que el
            // registro de I1 pida nombre y apellido por separado.
            val account = accounts.insertGuardian(email, firstName = name, lastName = "", passwordHash)
            openSession(account)
        }
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        return tx {
            val account = accounts.findByEmail(email) ?: throw InvalidCredentialsException()
            val hash = account.passwordHash ?: throw InvalidCredentialsException()
            if (!PasswordHasher.verify(request.password, hash)) throw InvalidCredentialsException()
            openSession(account)
        }
    }

    suspend fun logout(caller: Caller) {
        tx { sessions.delete(caller.sessionId) }
    }

    /** Se llama en cada request autenticado: revocar una sesión invalida su token al instante. */
    suspend fun isSessionActive(sessionId: UUID): Boolean =
        tx { sessions.isActive(sessionId, clock.now()) }

    private fun openSession(account: Account): AuthResponse {
        val now = clock.now()
        val sessionId = UUID.randomUUID()
        val issued = jwt.generateToken(account.id, sessionId, now)
        // El token de renovación se entrega a la app recién en I0-05. Hasta entonces la sesión
        // vence junto con el token de acceso, como antes de esta línea base.
        sessions.insert(sessionId, account.id, OpaqueToken.hash(OpaqueToken.generate()), issued.expiresAt, now)
        return AuthResponse(
            userId = account.id.toString(),
            email = account.email,
            name = account.fullName,
            token = issued.token,
        )
    }
}
