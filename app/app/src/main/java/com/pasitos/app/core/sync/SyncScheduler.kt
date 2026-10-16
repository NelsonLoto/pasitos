package com.pasitos.app.core.sync

/** Por qué se pidió una sincronización (14 · Sincronización, sección 6). */
enum class SyncTrigger {
    /** Al abrir la app o volver a ella. */
    APP_FOREGROUND,

    /** Después de guardar un cambio en la base local. */
    LOCAL_CHANGE,

    /** "Sincronizar ahora" (RNF-OFF-03). */
    MANUAL,

    /** Cada 15 minutos con red. */
    PERIODIC,
}

/** Programa el [SyncWorker]. Los repositorios lo llaman después de cada escritura local. */
interface SyncScheduler {

    /** Registra la sincronización periódica. Llamarlo de nuevo no la duplica ni la reinicia. */
    fun schedulePeriodic()

    /** Sincroniza apenas haya red. Si ya había una pendiente, la reemplaza. */
    fun requestSync(trigger: SyncTrigger)
}
