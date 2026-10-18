package com.pasitos.backend.mail

import io.ktor.server.config.ApplicationConfig

/** Variables de entorno del envío de emails (11 · Backend §7, ADR-07). */
data class MailConfig(
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUser: String,
    val smtpPassword: String,
    val from: String,
    /** Para armar los links de los emails hacia las páginas `/web/...` (ADR-27). */
    val publicBaseUrl: String,
) {
    companion object {
        fun from(config: ApplicationConfig): MailConfig = MailConfig(
            smtpHost = config.property("mail.smtpHost").getString(),
            smtpPort = config.property("mail.smtpPort").getString().toInt(),
            smtpUser = config.property("mail.smtpUser").getString(),
            smtpPassword = config.property("mail.smtpPassword").getString(),
            from = config.property("mail.from").getString(),
            publicBaseUrl = config.property("public.baseUrl").getString(),
        )
    }
}
