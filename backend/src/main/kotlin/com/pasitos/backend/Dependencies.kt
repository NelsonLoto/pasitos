package com.pasitos.backend

import com.pasitos.backend.sync.SyncRegistry

/** I0 no publica datos de dominio. I1 registra sus SyncEntity al adaptar cada dominio. */
fun productionSyncRegistry(): SyncRegistry = SyncRegistry()
