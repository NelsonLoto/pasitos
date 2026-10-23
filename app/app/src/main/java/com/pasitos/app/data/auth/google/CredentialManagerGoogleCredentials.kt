package com.pasitos.app.data.auth.google

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential Manager con `GetSignInWithGoogleOption`, la opción para un botón explícito: deja
 * agregar una cuenta si el teléfono no tiene ninguna y no deja de mostrarse si la persona lo
 * cierra varias veces (15 · Android §4).
 */
@Singleton
class CredentialManagerGoogleCredentials @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : GoogleCredentials {

    override suspend fun request(activityContext: Context): GoogleCredentialResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(GoogleConfig.WEB_CLIENT_ID).build())
            .build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential !is CustomCredential || credential.type !in GOOGLE_ID_TOKEN_TYPES) {
                return GoogleCredentialResult.Failure(GENERIC_ERROR)
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleCredentialResult.Success(
                idToken = google.idToken,
                email = google.email,
                givenName = google.givenName,
                familyName = google.familyName,
            )
        } catch (_: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelled
        } catch (e: GetCredentialException) {
            // Solo el tipo: el mensaje puede traer la cuenta (RNF-PRI-03)
            Log.w(TAG, "Credential Manager no devolvió una cuenta de Google: ${e.type}")
            GoogleCredentialResult.Failure(GENERIC_ERROR)
        } catch (_: GoogleIdTokenParsingException) {
            GoogleCredentialResult.Failure(GENERIC_ERROR)
        }
    }

    override suspend fun clear() {
        try {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            // No impide cerrar sesión: solo hace que el próximo ingreso proponga la misma cuenta
            Log.w(TAG, "No se pudo limpiar el estado de Credential Manager: ${e.type}")
        }
    }

    private companion object {
        const val TAG = "GoogleCredentials"
        const val GENERIC_ERROR = "No pudimos conectar con Google. Probá de nuevo."

        /** "Acceder con Google" devuelve el tipo SIWG; se acepta también el del ID token común. */
        val GOOGLE_ID_TOKEN_TYPES = setOf(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        )
    }
}
