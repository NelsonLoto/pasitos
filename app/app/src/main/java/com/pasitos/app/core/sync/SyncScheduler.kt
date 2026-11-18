package com.pasitos.app.core.sync

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncReason { APP_OPEN, SESSION_STARTED, LOCAL_CHANGE, MANUAL, NOTIFICATION }

@Singleton
class SyncScheduler @Inject constructor(@ApplicationContext context: Context) {
    private val manager = WorkManager.getInstance(context)

    fun requestSync(reason: SyncReason) = manager.enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, immediate(reason))
    fun schedulePeriodic() = manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic())
    fun cancelSync() {
        manager.cancelUniqueWork(NOW)
        manager.cancelUniqueWork(PERIODIC)
    }

    companion object {
        const val NOW = "sync-now"
        const val PERIODIC = "sync-periodic"
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        internal fun immediate(reason: SyncReason): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(network)
            .setInputData(workDataOf(SyncWorker.REASON to reason.name))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }
            }.build()

        internal fun periodic(): PeriodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(network).build()
    }
}
