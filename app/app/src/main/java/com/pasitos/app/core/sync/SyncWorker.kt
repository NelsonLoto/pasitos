package com.pasitos.app.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pasitos.app.core.sync.local.PendingMutationDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sincroniza la base local con el servidor (14 · Sincronización). En I0 solo abre la base y
 * registra la ejecución; enviar la bandeja de salida y traer cambios llega con I0-08 e I1.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingMutationDao: PendingMutationDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(TRIGGER)?.let(SyncTrigger::valueOf) ?: SyncTrigger.PERIODIC
        val pending = pendingMutationDao.count()
        Log.i(TAG, "Sincronización ($trigger, intento ${runAttemptCount + 1}): $pending cambios pendientes")
        return Result.success()
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val TRIGGER = "trigger"

        fun inputData(trigger: SyncTrigger): Data = workDataOf(TRIGGER to trigger.name)
    }
}
