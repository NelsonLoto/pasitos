package com.pasitos.app.core.sync

import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.SyncStateDao
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** RNF-OFF-04: cuándo se sincronizó por última vez y cuántos cambios esperan para enviarse. */
interface SyncStatus {
    /** Nulo si el teléfono todavía no sincronizó nunca. */
    val lastSyncedAt: Flow<Instant?>
    val pendingChanges: Flow<Int>
}

class RoomSyncStatus @Inject constructor(
    syncStateDao: SyncStateDao,
    pendingMutationDao: PendingMutationDao,
) : SyncStatus {
    override val lastSyncedAt: Flow<Instant?> = syncStateDao.observe().map { it?.lastSyncedAt }
    override val pendingChanges: Flow<Int> = pendingMutationDao.observeCount()
}
