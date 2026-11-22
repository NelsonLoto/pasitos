package com.pasitos.backend.common

import io.github.cdimascio.dotenv.dotenv
import com.pasitos.backend.auth.AuthConfig

enum class AppEnvironment { LOCAL, CI, STAGING }

data class DatabaseConfig(val host: String, val port: Int, val name: String, val user: String, val password: String) {
    init {
        require(host.matches(Regex("[A-Za-z0-9_.-]+"))) { "DB_HOST inválido" }
        require(name.matches(Regex("[A-Za-z0-9_-]+"))) { "DB_NAME inválido" }
    }
    val jdbcUrl: String get() = "jdbc:postgresql://$host:$port/$name"
    override fun toString() = "DatabaseConfig([redacted])"
}

data class AppConfig(val environment: AppEnvironment, val host: String, val port: Int, val database: DatabaseConfig, val auth: AuthConfig) {
    companion object {
        fun from(values: Map<String, String>): AppConfig {
            fun required(key: String) = values[key]?.takeIf { it.isNotBlank() }
                ?: error("Falta la variable $key")
            fun port(key: String, default: String) = (values[key] ?: default).toIntOrNull()
                ?.takeIf { it in 1..65535 } ?: error("$key debe ser un puerto válido")
            val environment = when (values["APP_ENV"] ?: "local") {
                "local" -> AppEnvironment.LOCAL
                "ci" -> AppEnvironment.CI
                "staging" -> AppEnvironment.STAGING
                else -> error("APP_ENV debe ser local, ci o staging")
            }
            return AppConfig(environment, values["HOST"] ?: "0.0.0.0", port("PORT", "8080"),
                DatabaseConfig(required("DB_HOST"), port("DB_PORT", "5432"), required("DB_NAME"),
                    required("DB_USER"), required("DB_PASSWORD")),
                AuthConfig(required("JWT_SECRET"), values["TERMS_VERSION"] ?: "1",
                    values["AUTH_RATE_LIMIT"]?.let { it.toIntOrNull() ?: error("AUTH_RATE_LIMIT debe ser un entero positivo") } ?: 10))
        }

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): AppConfig {
            val mode = environment["APP_ENV"] ?: "local"
            val local = if (mode == "local") dotenv { ignoreIfMissing = true }
                .entries().associate { it.key to it.value } else emptyMap()
            return from(local + environment + ("APP_ENV" to mode))
        }
    }
}
