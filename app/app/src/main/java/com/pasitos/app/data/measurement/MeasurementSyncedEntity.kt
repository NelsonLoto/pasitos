package com.pasitos.app.data.measurement

import android.util.Log
import com.pasitos.app.core.sync.OutboxEntity
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.core.sync.SyncedEntity
import com.pasitos.app.core.sync.local.MutationEntity
import com.pasitos.app.core.sync.local.MutationOp
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.data.measurement.local.MeasurementDao
import com.pasitos.app.data.measurement.remote.MeasurementPushData
import com.pasitos.app.data.measurement.remote.MeasurementRow
import com.pasitos.app.data.measurement.remote.measurementJson
import com.pasitos.shared.api.ErrorCode
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

/**
 * Mediciones en la sincronización: llegan por `/sync/changes` y se envían por `/sync/push` (ADR-28).
 * El motor no llama a [apply] con una medición que tiene cambios en la bandeja.
 */
class MeasurementSyncedEntity @Inject constructor(
    private val dao: MeasurementDao,
    private val pendingMutationDao: PendingMutationDao,
) : SyncedEntity, OutboxEntity {

    override val name: String = "MEASUREMENT"

    override val entity: MutationEntity = MutationEntity.MEASUREMENT

    override suspend fun apply(row: JsonObject) {
        val measurement = measurementJson.decodeFromJsonElement(MeasurementRow.serializer(), row)
        if (measurement.deleted) dao.delete(measurement.id) else dao.upsert(measurement.toEntity())
    }

    override suspend fun deleteChild(childId: String) = dao.deleteForChild(childId)

    /** Lo enviado queda como la copia del servidor; sincronizada, salvo que haya otro cambio detrás. */
    override suspend fun onApplied(mutation: PendingMutationEntity) {
        if (mutation.op != MutationOp.UPSERT) return
        val local = dao.find(mutation.entityId) ?: return
        val sent = measurementJson.decodeFromString(MeasurementPushData.serializer(), mutation.payload)
        val serverCopy = measurementJson.encodeToString(MeasurementRow.serializer(), sent.toRow(local.authorAccountId, local.authorRole))
        dao.upsert(local.copy(syncState = stateAfterAnswer(mutation, SyncState.SYNCED), syncError = null, serverCopy = serverCopy))
    }

    /**
     * Decisión de I1-A6: la medición queda con el motivo para corregirla o descartarla. Si ya no
     * existe en el servidor (se borró desde otro teléfono) o el niño ya no está, no hay nada que
     * corregir y se borra del teléfono.
     */
    override suspend fun onRejected(mutation: PendingMutationEntity, code: ErrorCode?, message: String?) {
        if (mutation.op != MutationOp.UPSERT) {
            // El borrado ya se hizo en el teléfono. Sin el motivo: puede tener datos personales
            Log.w(TAG, "El servidor rechazó el borrado de una medición: $code")
            return
        }
        if (code == ErrorCode.MEASUREMENT_NOT_FOUND || code == ErrorCode.CHILD_NOT_FOUND) {
            dao.delete(mutation.entityId)
            return
        }
        val local = dao.find(mutation.entityId) ?: return
        val state = stateAfterAnswer(mutation, SyncState.ERROR)
        dao.upsert(
            local.copy(
                syncState = state,
                syncError = if (state == SyncState.ERROR) message ?: "No se pudo guardar la medición" else null,
            ),
        )
    }

    /** Si el tutor volvió a editarla mientras se enviaba, sigue pendiente: el próximo envío decide. */
    private suspend fun stateAfterAnswer(mutation: PendingMutationEntity, answered: SyncState): SyncState =
        if (pendingMutationDao.hasPending(entity, mutation.entityId)) SyncState.PENDING else answered

    private companion object {
        const val TAG = "MeasurementSync"
    }
}
