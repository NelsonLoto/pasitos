package com.pasitos.backend.web

import com.pasitos.backend.account.AccountService
import com.pasitos.backend.auth.AuthService
import com.pasitos.backend.auth.ResetPasswordResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.thymeleaf.ThymeleafContent

/**
 * Páginas públicas que abren los links de los emails de cuenta (16 · Panel de administración
 * §Páginas públicas, ADR-27). Sin autenticación: el token de un solo uso de la URL es el permiso.
 * Un link vencido, ya usado o inexistente responde 400: no hay nada que el visitante pueda
 * corregir en el pedido salvo pedir uno nuevo.
 */
fun Route.webRoutes(authService: AuthService, accountService: AccountService) {
    get("/web/verify-email") {
        val token = call.request.queryParameters["token"]
        val success = token != null && authService.verifyEmail(token)
        call.respondTemplate("web/verify-email.html", mapOf("success" to success), ok = success)
    }

    get("/web/reset-password") {
        val token = call.request.queryParameters["token"].orEmpty()
        val tokenValid = token.isNotBlank() && authService.isPasswordResetTokenValid(token)
        call.respondTemplate(
            "web/reset-password.html",
            mapOf("tokenValid" to tokenValid, "token" to token, "success" to false, "error" to ""),
            ok = tokenValid,
        )
    }

    post("/web/reset-password") {
        val params = call.receiveParameters()
        val token = params["token"].orEmpty()
        val newPassword = params["newPassword"].orEmpty()
        when (authService.resetPassword(token, newPassword)) {
            ResetPasswordResult.Success ->
                call.respondTemplate("web/reset-password.html", mapOf("tokenValid" to true, "success" to true), ok = true)

            ResetPasswordResult.WeakPassword ->
                call.respondTemplate(
                    "web/reset-password.html",
                    mapOf(
                        "tokenValid" to true,
                        "token" to token,
                        "success" to false,
                        "error" to "La contraseña tiene que tener al menos 8 caracteres, con letras y números",
                    ),
                    // No es un problema del link, sino de la contraseña que se puede corregir y reintentar.
                    ok = true,
                )

            ResetPasswordResult.InvalidToken ->
                call.respondTemplate("web/reset-password.html", mapOf("tokenValid" to false, "success" to false), ok = false)
        }
    }

    get("/web/confirm-email-change") {
        val token = call.request.queryParameters["token"]
        val success = token != null && accountService.confirmEmailChange(token)
        call.respondTemplate("web/confirm-email-change.html", mapOf("success" to success), ok = success)
    }
}

private suspend fun ApplicationCall.respondTemplate(template: String, model: Map<String, Any>, ok: Boolean) {
    val status = if (ok) HttpStatusCode.OK else HttpStatusCode.BadRequest
    respond(status, ThymeleafContent(template, model))
}
