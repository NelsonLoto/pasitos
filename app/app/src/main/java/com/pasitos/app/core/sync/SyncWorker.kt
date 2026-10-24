package com.pasitos.app.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.NoConnectionException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sincroniza la base local con el servidor (14 · Sincronización): envía la bandeja de salida y trae
 * los cambios. Después refresca los datos de referencia, como el calendario de vacunación: si alguno
 * falla, la sincronización no falla y se vuelve a pedir la próxima vez.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val referenceData: ReferenceDataRefresher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(TRIGGER)?.let(SyncTrigger::valueOf) ?: SyncTrigger.PERIODIC
        return try {
            val result = syncEngine.sync()
            Log.i(
                TAG,
                "Sincronización ($trigger): ${result.applied} cambios enviados, ${result.rejected} rechazados, " +
                    "${result.received} recibidos, ${result.skipped} salteados",
            )
            referenceData.refreshAll()
            Result.success()
        } catch (_: NoConnectionException) {
            // WorkManager reintenta con espera creciente
            Result.retry()
        } catch (e: ApiException) {
            // Un error del servidor no se arregla reintentando enseguida: vuelve a probar la
            // periódica o el próximo disparador
            Log.w(TAG, "Sincronización ($trigger) rechazada: ${e.code}")
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val TRIGGER = "trigger"

        fun inputData(trigger: SyncTrigger): Data = workDataOf(TRIGGER to trigger.name)
    }
}
