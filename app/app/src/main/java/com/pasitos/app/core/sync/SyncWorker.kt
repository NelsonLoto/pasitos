package com.pasitos.app.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        // I0-07 solo comprueba el disparador. No adelantar el cursor ni marcar datos como enviados.
        Log.i(TAG, "Ejecutado el esqueleto de sincronización (${inputData.getString(REASON) ?: "PERIODIC"})")
        return Result.success(workDataOf("skeletonExecuted" to true))
    }

    companion object {
        const val TAG = "PasitosSync"
        const val REASON = "reason"
    }
}
