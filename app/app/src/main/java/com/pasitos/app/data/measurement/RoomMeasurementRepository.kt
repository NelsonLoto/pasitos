package com.pasitos.app.data.measurement

import android.util.Log
import androidx.room.withTransaction
import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.core.sync.local.MutationEntity
import com.pasitos.app.core.sync.local.MutationOp
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.measurement.local.MeasurementDao
import com.pasitos.app.data.measurement.local.MeasurementEntity
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.measurement.model.MeasurementInput
import com.pasitos.app.data.measurement.remote.MeasurementDeleteData
import com.pasitos.app.data.measurement.remote.MeasurementPushData
import com.pasitos.app.data.measurement.remote.MeasurementRow
import com.pasitos.app.data.measurement.remote.measurementJson
import com.pasitos.app.data.measurement.remote.toPushData
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [MeasurementRepository] sobre Room y la bandeja de salida (14 · Sincronización §6). Cada escritura
 * guarda la fila y su cambio en la misma transacción y pide una sincronización inmediata.
 */
@Singleton
class RoomMeasurementRepository @Inject constructor(
    private val database: PasitosDatabase,
    private val dao: MeasurementDao,
    private val pendingMutationDao: PendingMutationDao,
    private val syncScheduler: SyncScheduler,
    private val authRepository: AuthRepository,
    private val clock: ArgentinaClock,
) : MeasurementRepository {

    override fun observeMeasurements(childId: String): Flow<List<Measurement>> =
        dao.observeByChild(childId)
            .map { rows -> rows.map { it.toDomain() } }
            // Al cerrar sesión la base se borra mientras se la observa, y la app se reinicia enseguida
            .catch { e ->
                Log.w(TAG, "Se dejó de leer la base local de mediciones", e)
                emit(emptyList())
            }

    override fun observeMeasurement(id: String): Flow<Measurement?> =
        dao.observe(id).map { it?.toDomain() }.catch { emit(null) }

    override suspend fun findOwnOn(childId: String, date: LocalDate, excludeId: String?): Measurement? {
        val accountId = authRepository.currentSession.value?.userId ?: return null
        return dao.findByAuthorOn(childId, accountId, date, excludeId.orEmpty())?.toDomain()
    }

    override suspend fun addMeasurement(childId: String, input: MeasurementInput): Result<Measurement> = runCatching {
        val accountId = checkNotNull(authRepository.currentSession.value) { "No hay sesión activa" }.userId
        val measurement = MeasurementEntity(
            id = UUID.randomUUID().toString(),
            childId = childId,
            measuredOn = input.date,
            weightKg = input.weightKg,
            lengthCm = input.lengthCm,
            lengthPosition = input.lengthPosition.takeIf { input.lengthCm != null },
            headCircumferenceCm = input.headCircumferenceCm,
            outlierConfirmed = false,
            authorAccountId = accountId,
            // En I1 la app solo tiene el perfil de tutor; el de pediatra llega en I3
            authorRole = AuthorRole.GUARDIAN,
            syncState = SyncState.PENDING,
        )
        database.withTransaction {
            dao.findByAuthorOn(childId, accountId, input.date, excludeId = "")?.let { throw DuplicateMeasurementException(it.id) }
            dao.upsert(measurement)
            enqueue(measurement, MutationOp.UPSERT, encode(measurement.toPushData()))
        }
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
        measurement.toDomain()
    }

    override suspend fun updateMeasurement(id: String, input: MeasurementInput): Result<Measurement> = runCatching {
        val updated = database.withTransaction {
            val existing = requireOwn(id)
            dao.findByAuthorOn(existing.childId, existing.authorAccountId, input.date, excludeId = id)
                ?.let { throw DuplicateMeasurementException(it.id) }
            val updated = existing.copy(
                measuredOn = input.date,
                weightKg = input.weightKg,
                lengthCm = input.lengthCm,
                lengthPosition = input.lengthPosition.takeIf { input.lengthCm != null },
                headCircumferenceCm = input.headCircumferenceCm,
                syncState = SyncState.PENDING,
                syncError = null,
            )
            dao.upsert(updated)
            enqueue(updated, MutationOp.UPSERT, encode(updated.toPushData()))
            updated
        }
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
        updated.toDomain()
    }

    override suspend fun deleteMeasurement(id: String): Result<Unit> = runCatching {
        val sent = database.withTransaction {
            val existing = requireOwn(id)
            dao.delete(id)
            // Una medición que el servidor nunca tuvo ni va a recibir (rechazada) se borra solo acá
            val serverKnowsIt = existing.serverCopy != null || pendingMutationDao.hasPending(MutationEntity.MEASUREMENT, id)
            if (serverKnowsIt) {
                enqueue(existing, MutationOp.DELETE, measurementJson.encodeToString(MeasurementDeleteData.serializer(), MeasurementDeleteData(id)))
            }
            serverKnowsIt
        }
        if (sent) syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
    }

    override suspend fun discardChange(id: String): Result<Unit> = runCatching {
        database.withTransaction {
            val existing = requireOwn(id)
            check(existing.syncState == SyncState.ERROR) { "Esta medición no tiene un cambio rechazado" }
            val serverCopy = existing.serverCopy
            if (serverCopy == null) {
                dao.delete(id)
            } else {
                dao.upsert(measurementJson.decodeFromString(MeasurementRow.serializer(), serverCopy).toEntity())
            }
        }
    }

    /** RF-CRE-02: solo el autor edita o borra. */
    private suspend fun requireOwn(id: String): MeasurementEntity {
        val accountId = checkNotNull(authRepository.currentSession.value) { "No hay sesión activa" }.userId
        val existing = checkNotNull(dao.find(id)) { "La medición ya no existe" }
        check(existing.authorAccountId == accountId) { "Solo quien cargó la medición puede editarla" }
        return existing
    }

    private suspend fun enqueue(measurement: MeasurementEntity, op: MutationOp, payload: String) {
        pendingMutationDao.insert(
            PendingMutationEntity(
                mutationId = UUID.randomUUID().toString(),
                entity = MutationEntity.MEASUREMENT,
                entityId = measurement.id,
                childId = measurement.childId,
                op = op,
                payload = payload,
                createdAt = clock.now(),
            ),
        )
    }

    private fun encode(data: MeasurementPushData): String = measurementJson.encodeToString(MeasurementPushData.serializer(), data)

    private companion object {
        const val TAG = "RoomMeasurementRepo"
    }
}
