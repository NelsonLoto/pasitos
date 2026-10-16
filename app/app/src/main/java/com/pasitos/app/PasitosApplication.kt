package com.pasitos.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.SessionState
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltAndroidApp
class PasitosApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var authRepository: AuthRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // WorkManager se inicia acá y no con su inicializador automático (quitado en el manifest),
    // para que cree los workers con Hilt
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Solo se sincroniza con sesión. Al restaurarla o iniciarla se programa la periódica y se
        // sincroniza enseguida (al abrir la app todavía se está restaurando y onStart no lo hace).
        // Al vencer o cerrarla se cancela todo.
        authRepository.sessionState
            .filterNot { it == SessionState.Restoring }
            .map { it is SessionState.SignedIn }
            .distinctUntilChanged()
            .onEach { signedIn ->
                if (signedIn) {
                    syncScheduler.schedulePeriodic()
                    syncScheduler.requestSync(SyncTrigger.APP_FOREGROUND)
                } else {
                    syncScheduler.cancelAll()
                }
            }
            .launchIn(applicationScope)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (authRepository.sessionState.value is SessionState.SignedIn) {
                    syncScheduler.requestSync(SyncTrigger.APP_FOREGROUND)
                }
            }
        })
    }
}
