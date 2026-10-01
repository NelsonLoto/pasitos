package com.pasitos.backend.auth

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {

    fun hash(plain: String): String =
        BCrypt.withDefaults().hashToString(12, plain.toCharArray())

    fun verify(plain: String, hash: String): Boolean =
        BCrypt.verifyer().verify(plain.toCharArray(), hash).verified
}
