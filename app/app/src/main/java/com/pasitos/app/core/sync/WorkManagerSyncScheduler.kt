package com.pasitos.app.core.sync

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SyncScheduler {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    private val withNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL)
            .setConstraints(withNetwork)
            .setInputData(SyncWorker.inputData(SyncTrigger.PERIODIC))
            .build()
        // UPDATE conserva la próxima ejecución programada y aplica cambios de configuración
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun requestSync(trigger: SyncTrigger) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(withNetwork)
            .setInputData(SyncWorker.inputData(trigger))
            .apply {
                // Antes de Android 12, un trabajo acelerado corre como servicio en primer plano y
                // obliga a mostrar una notificación: ahí se encola como trabajo normal.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }
            }
            .build()
        // Reemplazar es seguro: reenviar cambios no los duplica (RNF-OFF-03)
        workManager.enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val PERIODIC_WORK = "sync-periodic"
        const val IMMEDIATE_WORK = "sync-now"

        /** El mínimo que permite WorkManager. */
        val PERIODIC_INTERVAL: Duration = Duration.ofMinutes(15)
    }
}
