package com.pasitos.app.data.child

import android.util.Log
import com.pasitos.app.core.sync.SyncEngine
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.child.local.ActiveChildEntity
import com.pasitos.app.data.child.local.ChildDao
import com.pasitos.app.data.child.local.GuardianshipEntity
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildInput
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.app.data.child.remote.ChildRemoteDataSource
import com.pasitos.app.data.child.remote.toEntity
import com.pasitos.app.data.child.remote.toSex
import com.pasitos.shared.api.child.CreateChildRequest
import com.pasitos.shared.api.child.CreatePatientRequest
import com.pasitos.shared.api.child.PatchChildRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * [ChildRepository] sobre Room (15 · App Android §1). Las respuestas del alta y la edición se
 * guardan con el `Mutex` del [SyncEngine], para que una sincronización en curso no las pise con una
 * versión vieja (14 · Sincronización §6).
 */
@Singleton
class RoomChildRepository @Inject constructor(
    private val dao: ChildDao,
    private val remote: ChildRemoteDataSource,
    private val syncEngine: SyncEngine,
    private val syncScheduler: SyncScheduler,
    private val authRepository: AuthRepository,
) : ChildRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val children: StateFlow<List<Child>> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        // Al cerrar sesión la base se borra mientras se la observa, y la app se reinicia enseguida
        .catch { e ->
            Log.w(TAG, "Se dejó de leer la base local de hijos", e)
            emit(emptyList())
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val activeChildId: StateFlow<String?> =
        combine(children, dao.observeActiveChildId().catch { emit(null) }) { children, chosen ->
            chosen?.takeIf { id -> children.any { it.id == id } } ?: children.firstOrNull()?.id
        }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun observeRole(childId: String): Flow<GuardianRole?> {
        val accountId = authRepository.currentSession.value?.userId ?: return emptyFlow()
        return dao.observeRole(childId, accountId)
    }

    override suspend fun addChild(input: ChildInput, representationDeclared: Boolean): Result<Child> = runCatching {
        val accountId = checkNotNull(authRepository.currentSession.value) { "No hay sesión activa" }.userId
        val child = syncEngine.withLock {
            val created = remote.create(input.toCreateRequest(representationDeclared)).toEntity()
            dao.insertCreated(created, GuardianshipEntity(created.id, accountId, GuardianRole.PRINCIPAL))
            created.toDomain()
        }
        // Trae la tutoría y deja el cursor al día; los otros teléfonos lo reciben en su próxima sincronización
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
        child
    }

    override suspend fun addPatient(input: ChildInput): Result<Child> = runCatching {
        val child = syncEngine.withLock {
            val created = remote.createPatient(input.toCreatePatientRequest()).toEntity()
            dao.insertPatient(created)
            created.toDomain()
        }
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
        child
    }

    override suspend fun updateChild(childId: String, input: ChildInput): Result<Child> = runCatching {
        val child = syncEngine.withLock {
            val updated = remote.update(childId, input.toPatchRequest()).toEntity()
            dao.upsert(updated)
            updated.toDomain()
        }
        syncScheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
        child
    }

    override suspend fun isOnlyGuardian(childId: String): Result<Boolean> = runCatching {
        remote.deletionPreview(childId).onlyGuardian
    }

    override suspend fun deleteChild(childId: String): Result<Unit> = runCatching {
        syncEngine.withLock {
            remote.delete(childId)
            // Sin esperar la sincronización. Si la app se cierra antes, la borra revokedChildIds
            syncEngine.forgetChild(childId)
        }
    }

    override suspend fun selectChild(childId: String) {
        dao.saveActiveChild(ActiveChildEntity(childId = childId))
    }

    private fun ChildInput.toCreateRequest(representationDeclared: Boolean) = CreateChildRequest(
        firstName = firstName.trim(),
        lastName = lastName.trim(),
        sex = sex.toSex(),
        birthDate = birthDate.toString(),
        gestationalWeeks = gestationalWeeks,
        birthWeightKg = birthWeightKg,
        birthLengthCm = birthLengthCm,
        birthHeadCircumferenceCm = birthHeadCircumferenceCm,
        avatarKey = avatarKey,
        representationDeclared = representationDeclared,
    )

    private fun ChildInput.toCreatePatientRequest() = CreatePatientRequest(
        firstName = firstName.trim(),
        lastName = lastName.trim(),
        sex = sex.toSex(),
        birthDate = birthDate.toString(),
        gestationalWeeks = gestationalWeeks,
        birthWeightKg = birthWeightKg,
        birthLengthCm = birthLengthCm,
        birthHeadCircumferenceCm = birthHeadCircumferenceCm,
        avatarKey = avatarKey,
    )

    private fun ChildInput.toPatchRequest() = PatchChildRequest(
        firstName = firstName.trim(),
        lastName = lastName.trim(),
        sex = sex.toSex(),
        birthDate = birthDate.toString(),
        gestationalWeeks = gestationalWeeks,
        birthWeightKg = birthWeightKg,
        birthLengthCm = birthLengthCm,
        birthHeadCircumferenceCm = birthHeadCircumferenceCm,
        allergies = allergies?.trim()?.ifBlank { null },
        relevantHistory = relevantHistory?.trim()?.ifBlank { null },
        avatarKey = avatarKey,
    )

    private companion object {
        const val TAG = "RoomChildRepository"
    }
}
