package com.pasitos.app.data.auth

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.pasitos.app.core.network.withoutSession
import com.pasitos.app.data.auth.local.SessionStore
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.TokenPair
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Revoca en el servidor las sesiones que se cerraron sin conexión (RF-CTA-07, CA3). */
interface SessionRevocation {

    /** Guarda [refreshToken] cifrado y programa su revocación para cuando haya red. */
    suspend fun schedule(refreshToken: String)
}

@Singleton
class WorkManagerSessionRevocation @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: SessionStore,
) : SessionRevocation {

    override suspend fun schedule(refreshToken: String) {
        store.addPendingRevocation(refreshToken)
        val request = OneTimeWorkRequestBuilder<RevokeSessionsWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // Si ya hay una revocación corriendo, esta va después y lee la lista actualizada. Se espera
        // a que WorkManager la registre porque a continuación la app se reinicia.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            .await()
    }

    companion object {
        const val WORK_NAME = "revoke-sessions"
    }
}

/**
 * Revoca cada sesión pendiente: renueva con el token guardado y cierra la sesión con el token de
 * acceso nuevo. Si alguna no se pudo revocar, lo vuelve a intentar más tarde.
 */
@HiltWorker
class RevokeSessionsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val httpClient: HttpClient,
    private val store: SessionStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = store.pendingRevocations()
        val revoked = pending.filter { revoke(it) }
        store.removePendingRevocations(revoked)
        return if (revoked.size == pending.size) Result.success() else Result.retry()
    }

    /** Devuelve falso si hay que volver a intentar. */
    private suspend fun revoke(refreshToken: String): Boolean = try {
        val refresh = httpClient.post("/api/v1/auth/refresh") {
            withoutSession()
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
        when (refresh.status) {
            // La sesión ya no existe: venció o el servidor la revocó
            HttpStatusCode.Unauthorized -> true
            HttpStatusCode.OK -> {
                val logout = httpClient.post("/api/v1/auth/logout") {
                    withoutSession()
                    bearerAuth(refresh.body<TokenPair>().accessToken)
                }
                // Si el cierre falla, el reintento usa el token guardado, que ya rotó: el servidor
                // lo toma como un reuso y revoca la sesión igual
                logout.status == HttpStatusCode.NoContent || logout.status == HttpStatusCode.Unauthorized
            }
            else -> false
        }
    } catch (_: IOException) {
        false
    }
}
