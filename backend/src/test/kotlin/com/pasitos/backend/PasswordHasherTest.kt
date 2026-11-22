package com.pasitos.backend

import com.pasitos.backend.auth.PasswordHasher
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test fun `I0-05 contrasenas largas conservan los caracteres posteriores al byte 72`() {
        val passwords = PasswordHasher()
        val prefix = "á".repeat(40)
        val hash = passwords.hash(prefix + "Final1")
        assertTrue(passwords.matches(prefix + "Final1", hash))
        assertFalse(passwords.matches(prefix + "Otro2", hash))
        assertFalse(passwords.matches(prefix + "Final1", null))
    }
}
