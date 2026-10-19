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

    /** RF-HIJ-05: invitación a ser tutor/a, a una cuenta que ya existe (se acepta/rechaza en la app). */
    suspend fun sendGuardianInvitation(to: String, recipientFirstName: String, inviterFirstName: String, childName: String) {
        send(
            to = to,
            subject = "$inviterFirstName te invitó a Pasitos",
            template = "email/guardian-invitation",
            context = Context().apply {
                setVariable("firstName", recipientFirstName)
                setVariable("inviterFirstName", inviterFirstName)
                setVariable("childName", childName)
            },
        )
    }

    /** RF-HIJ-05: invitación a ser tutor/a, a un email sin cuenta todavía. */
    suspend fun sendGuardianInvitationNewAccount(to: String, inviterFirstName: String, childName: String) {
        send(
            to = to,
            subject = "$inviterFirstName te invitó a Pasitos",
            template = "email/guardian-invitation-new-account",
            context = Context().apply {
                setVariable("inviterFirstName", inviterFirstName)
                setVariable("childName", childName)
            },
        )
    }

    /** RF-HIJ-06, CA1: aviso al tutor principal de que aceptaron su invitación. */
    suspend fun sendGuardianInvitationAccepted(to: String, principalFirstName: String, responderFirstName: String, childName: String) {
        send(
            to = to,
            subject = "$responderFirstName aceptó tu invitación",
            template = "email/guardian-invitation-accepted",
            context = Context().apply {
                setVariable("firstName", principalFirstName)
                setVariable("responderFirstName", responderFirstName)
                setVariable("childName", childName)
            },
        )
    }

    /** RF-HIJ-06, CA2: aviso al tutor principal de que rechazaron su invitación. */
    suspend fun sendGuardianInvitationRejected(to: String, principalFirstName: String, responderFirstName: String, childName: String) {
        send(
            to = to,
            subject = "$responderFirstName rechazó tu invitación",
            template = "email/guardian-invitation-rejected",
            context = Context().apply {
                setVariable("firstName", principalFirstName)
                setVariable("responderFirstName", responderFirstName)
                setVariable("childName", childName)
            },
        )
    }

    /** RF-HIJ-09: aviso a un tutor de que el tutor principal lo sacó de un niño. */
    suspend fun sendGuardianRemoved(to: String, firstName: String, childName: String) {
        send(
            to = to,
            subject = "Dejaste de ser tutor/a en Pasitos",
            template = "email/guardian-removed",
            context = Context().apply {
                setVariable("firstName", firstName)
                setVariable("childName", childName)
            },
        )
    }

    /** RF-HIJ-09: aviso al tutor principal de que otro tutor se fue por su cuenta. */
    suspend fun sendGuardianSelfAbandoned(to: String, principalFirstName: String, formerGuardianFirstName: String, childName: String) {
        send(
            to = to,
            subject = "Un tutor dejó de ver a $childName",
            template = "email/guardian-self-abandoned",
            context = Context().apply {
                setVariable("firstName", principalFirstName)
                setVariable("formerGuardianFirstName", formerGuardianFirstName)
                setVariable("childName", childName)
            },
        )
    }

    /** RF-HIJ-08: aviso del cambio de tutor principal, al que lo cede y al que lo recibe. */
    suspend fun sendPrincipalTransferred(to: String, firstName: String, childName: String, becamePrincipal: Boolean) {
        send(
            to = to,
            subject = "Cambió el tutor principal de $childName",
            template = "email/principal-transferred",
            context = Context().apply {
                setVariable("firstName", firstName)
                setVariable("childName", childName)
                setVariable("becamePrincipal", becamePrincipal)
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
