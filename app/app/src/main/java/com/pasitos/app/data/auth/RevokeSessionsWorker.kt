package com.pasitos.app.data.auth

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pasitos.app.core.security.SessionStorage
import com.pasitos.app.data.auth.remote.AuthApi
import com.pasitos.app.data.auth.remote.AuthApiException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class RevokeSessionsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val store: SessionStorage,
    private val api: AuthApi,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        for (token in store.pendingRevocations()) {
            try {
                val fresh = api.refresh(token)
                api.logout(fresh.accessToken)
                store.removeRevocation(token)
            } catch (failure: AuthApiException) {
                if (failure.status == 401) store.removeRevocation(token) else return Result.retry()
            }
        }
        Result.success()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { Result.retry() }
}
