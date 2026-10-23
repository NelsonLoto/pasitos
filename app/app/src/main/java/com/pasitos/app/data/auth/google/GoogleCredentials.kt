package com.pasitos.app.data.auth.google

import android.content.Context

/** Cuenta de Google del teléfono para el inicio con Google (RF-CTA-03, I1-A2). */
interface GoogleCredentials {

    /**
     * Abre el diálogo "Acceder con Google" y devuelve el ID token de la cuenta elegida.
     * [activityContext] tiene que ser la Activity: Credential Manager muestra su interfaz sobre
     * ella. No se retiene después de la llamada.
     */
    suspend fun request(activityContext: Context): GoogleCredentialResult

    /** Al cerrar sesión: el próximo inicio con Google vuelve a preguntar qué cuenta usar. */
    suspend fun clear()
}

sealed interface GoogleCredentialResult {

    /**
     * [idToken] permite iniciar sesión como esa persona durante una hora: solo vive en memoria y
     * nunca va a logs ni a disco. [email] (la cuenta elegida), [givenName] y [familyName] se
     * muestran en el paso de términos.
     */
    class Success(
        val idToken: String,
        val email: String?,
        val givenName: String?,
        val familyName: String?,
    ) : GoogleCredentialResult {
        override fun toString() = "Success(idToken=<oculto>)"
    }

    /** La persona cerró el diálogo: se queda en el login sin error (RF-CTA-03, CA3). */
    data object Cancelled : GoogleCredentialResult

    data class Failure(val message: String) : GoogleCredentialResult
}
