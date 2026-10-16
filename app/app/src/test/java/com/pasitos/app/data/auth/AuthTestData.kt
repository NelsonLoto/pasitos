package com.pasitos.app.data.auth

import com.pasitos.shared.api.auth.AccountDto
import com.pasitos.shared.api.auth.TokenPair
import java.time.Instant

/** Datos ficticios para las pruebas de sesión (D-65). */
object AuthTestData {

    fun account(id: String = "11111111-1111-1111-1111-111111111111", firstName: String = "Ana") = AccountDto(
        id = id,
        email = "ana@pasitos.test",
        firstName = firstName,
        lastName = "Pérez",
        emailVerified = false,
        isGuardian = true,
        lastMode = null,
    )

    /** Par de tokens número [n]: cada renovación devuelve el siguiente. */
    fun tokens(n: Int) = TokenPair(
        accessToken = "access-$n",
        accessTokenExpiresAt = Instant.parse("2026-10-16T15:15:00Z").plusSeconds(900L * n),
        refreshToken = "refresh-$n",
    )
}
