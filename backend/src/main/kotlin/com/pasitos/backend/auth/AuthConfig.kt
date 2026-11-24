package com.pasitos.backend.auth

/** Configuración explícita: nunca generar una clave de firma nueva al reiniciar el servidor. */
data class AuthConfig(val secret: String, val termsVersion: String = "1", val requestsPerMinute: Int = 10) {
    init {
        require(secret.length >= 64) { "JWT_SECRET debe tener al menos 64 caracteres" }
        require(termsVersion.isNotBlank()) { "TERMS_VERSION no puede estar vacía" }
        require(requestsPerMinute > 0) { "AUTH_RATE_LIMIT debe ser positivo" }
    }
    override fun toString() = "AuthConfig(secret=[redacted], termsVersion=$termsVersion, requestsPerMinute=$requestsPerMinute)"
}
