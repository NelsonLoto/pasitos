package com.pasitos.app.core.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.pasitos.app.data.auth.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** En I0 observa la sesión falsa; I0-06 reemplaza su persistencia, no estos disparadores. */
@Singleton
class SyncLifecycle @Inject constructor(private val auth: AuthRepository, private val scheduler: SyncScheduler) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            auth.currentSession.collect { session ->
                if (session == null) scheduler.cancelSync() else {
                    scheduler.schedulePeriodic()
                    scheduler.requestSync(SyncReason.SESSION_STARTED)
                }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (auth.currentSession.value != null) scheduler.requestSync(SyncReason.APP_OPEN)
    }
}
