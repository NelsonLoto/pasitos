package com.pasitos.backend.mail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simplejavamail.api.email.Email
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import org.slf4j.LoggerFactory
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

/**
 * Emails transaccionales de cuentas (RF-CTA-02, RF-CTA-05; ADR-07, 17 · Notificaciones y emails).
 *
 * El envío se hace fuera de la transacción de base de datos que crea el token (los servicios la
 * llaman después de que esa transacción cierra) y un fallo no interrumpe la operación que lo
 * disparó: se registra en el log y el pedido que la originó igual responde bien (17 · §3).
 */
class MailService(private val config: MailConfig, private val templateEngine: TemplateEngine) {

    private val logger = LoggerFactory.getLogger(MailService::class.java)

    private val mailer: Mailer = MailerBuilder
        .withSMTPServer(
            config.smtpHost,
            config.smtpPort,
            config.smtpUser.ifBlank { null },
            config.smtpPassword.ifBlank { null },
        )
        .withTransportStrategy(TransportStrategy.SMTP)
        .buildMailer()

    /** RF-CTA-01 CA1, RF-CTA-02: email con el link para verificar la cuenta (vence a las 24 h, RN-CTA-06). */
    suspend fun sendVerificationEmail(to: String, firstName: String, token: String) {
        send(
            to = to,
            subject = "Confirmá tu email de Pasitos",
            template = "email/verify-email",
            context = linkContext(firstName, "/web/verify-email?token=$token"),
        )
    }

    /** RF-CTA-05: email con el link para definir una contraseña nueva (vence a los 60 min, RN-CTA-06). */
    suspend fun sendPasswordResetEmail(to: String, firstName: String, token: String) {
        send(
            to = to,
            subject = "Recuperá tu contraseña de Pasitos",
            template = "email/reset-password",
            context = linkContext(firstName, "/web/reset-password?token=$token"),
        )
    }

    /** RF-CTA-11, CA1: verificación del email nuevo, antes de reemplazar al anterior (vence a las 24 h). */
    suspend fun sendEmailChangeVerification(to: String, firstName: String, token: String) {
        send(
            to = to,
            subject = "Confirmá tu nuevo email de Pasitos",
            template = "email/confirm-email-change",
            context = linkContext(firstName, "/web/confirm-email-change?token=$token"),
        )
    }

    /** RF-CTA-11, CA1: aviso al email anterior, una vez confirmado el cambio. */
    suspend fun sendEmailChangeNotice(to: String, firstName: String, newEmail: String) {
        send(
            to = to,
            subject = "Tu email de Pasitos cambió",
            template = "email/email-changed-notice",
            context = Context().apply {
                setVariable("firstName", firstName)
                setVariable("newEmail", newEmail)
            },
        )
    }

    private fun linkContext(firstName: String, path: String) = Context().apply {
        setVariable("firstName", firstName)
        setVariable("link", "${config.publicBaseUrl}$path")
    }

    private suspend fun send(to: String, subject: String, template: String, context: Context) {
        try {
            val html = templateEngine.process("$template.html", context)
            val text = templateEngine.process("$template.txt", context)
            val email: Email = EmailBuilder.startingBlank()
                .from(config.from)
                .to(to)
                .withSubject(subject)
                .withPlainText(text)
                .withHTMLText(html)
                .buildEmail()
            withContext(Dispatchers.IO) { mailer.sendMail(email) }
        } catch (e: Exception) {
            // Un email que no sale no tiene que tirar abajo el pedido que lo disparó (17 · §3).
            logger.warn("No se pudo enviar el email '{}': {}", template, e.message)
        }
    }
}
