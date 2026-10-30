package com.pasitos.backend.auth

/** RN-CTA-01: al menos 8 caracteres, con al menos una letra y un número. */
object PasswordPolicy {
    fun isValid(password: String): Boolean =
        password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }
}
