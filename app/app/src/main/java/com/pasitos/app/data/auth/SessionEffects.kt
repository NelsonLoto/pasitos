package com.pasitos.app.data.auth

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.work.*
import com.pasitos.app.core.security.SessionRestartActivity
import com.pasitos.app.core.database.LocalDatabase
import com.pasitos.app.core.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionEffects @Inject constructor(
    @ApplicationContext private val context: Context,
    private val local: LocalDatabase,
    private val sync: SyncScheduler,
) : SessionEffects {
    override suspend fun stopSync() = sync.cancelSyncAndAwait()
    override suspend fun wipeDatabase() = local.wipe()
    override fun restart() {
        context.startActivity(Intent(context, SessionRestartActivity::class.java)
            .putExtra(SessionRestartActivity.PREVIOUS_PID, Process.myPid())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
    override fun scheduleRevocations() {
        WorkManager.getInstance(context).enqueueUniqueWork("revoke-sessions", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RevokeSessionsWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
}
