package com.pasitos.app.data.child

import com.pasitos.app.core.sync.SyncEngine
import com.pasitos.app.core.sync.SyncedEntity
import com.pasitos.app.data.child.local.ChildDao
import com.pasitos.app.data.child.remote.ChildRow
import com.pasitos.app.data.child.remote.GuardianshipRow
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val rowJson = Json { ignoreUnknownKeys = true }

/** Hijos en la sincronización. Llegan por acá y se escriben por REST (ADR-28). */
class ChildSyncedEntity @Inject constructor(private val dao: ChildDao) : SyncedEntity {

    override val name: String = SyncEngine.CHILD

    override suspend fun apply(row: JsonObject) {
        val child = rowJson.decodeFromJsonElement(ChildRow.serializer(), row)
        if (child.deleted) dao.delete(child.id) else dao.upsert(child.toEntity())
    }

    override suspend fun deleteChild(childId: String) = dao.delete(childId)
}

/** Tutores de cada niño en la sincronización. Se escriben por REST (ADR-28). */
class GuardianshipSyncedEntity @Inject constructor(private val dao: ChildDao) : SyncedEntity {

    override val name: String = "GUARDIANSHIP"

    override suspend fun apply(row: JsonObject) {
        val guardianship = rowJson.decodeFromJsonElement(GuardianshipRow.serializer(), row)
        if (guardianship.deleted) {
            dao.deleteGuardianship(guardianship.childId, guardianship.accountId)
        } else {
            dao.upsertGuardianship(guardianship.toEntity())
        }
    }

    override suspend fun deleteChild(childId: String) = dao.deleteGuardianships(childId)
}
