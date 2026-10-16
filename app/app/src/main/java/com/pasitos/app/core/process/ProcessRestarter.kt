package com.pasitos.app.core.process

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reinicia la app en un proceso nuevo. */
interface ProcessRestarter {

    /**
     * Termina el proceso y vuelve a abrir la app desde su pantalla inicial. Lo usa el cierre de
     * sesión: así no queda nada de la sesión anterior en memoria (15 · App Android §3). No vuelve.
     */
    fun restart()
}

@Singleton
class AndroidProcessRestarter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ProcessRestarter {

    override fun restart() {
        val launchIntent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        // El sistema registra el pedido antes de que el proceso termine y abre la app en uno nuevo
        context.startActivity(Intent.makeRestartActivityTask(launchIntent.component))
        Runtime.getRuntime().exit(0)
    }
}
