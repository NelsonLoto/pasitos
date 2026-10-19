package com.pasitos.app.data.child

import com.pasitos.app.core.network.SessionTokens
import com.pasitos.app.core.network.RefreshResult
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildInput
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.shared.api.auth.TokenPair
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Datos ficticios para las pruebas de hijos (D-65). */
object ChildTestData {

    const val ACCOUNT_ID = "11111111-1111-1111-1111-111111111111"
    const val SOFIA = "aaaaaaaa-0000-0000-0000-000000000001"
    const val TOMAS = "aaaaaaaa-0000-0000-0000-000000000002"

    /** 19/10/2026 al mediodía en Argentina. */
    val clock = ArgentinaClock(Clock.fixed(Instant.parse("2026-10-19T15:00:00Z"), ZoneOffset.UTC))

    val session = AuthSession(userId = ACCOUNT_ID, email = "ana@pasitos.test", firstName = "Ana", lastName = "Pérez")

    /** Fila `CHILD` como la arma el backend en `/sync/changes`. */
    fun childRow(id: String = SOFIA, firstName: String = "Sofía", birthDate: String = "2026-03-15", deleted: Boolean = false): JsonObject =
        buildJsonObject {
            put("id", id)
            put("firstName", firstName)
            put("lastName", "Pérez")
            put("sex", "FEMALE")
            put("birthDate", birthDate)
            put("gestationalWeeks", 38)
            put("gestationalWeeksUnknown", false)
            put("birthWeightKg", 3.25)
            put("avatarKey", "rabbit")
            put("status", "ACTIVE")
            put("deleted", deleted)
        }

    fun guardianshipRow(childId: String = SOFIA, accountId: String = ACCOUNT_ID, role: String = "PRINCIPAL", deleted: Boolean = false): JsonObject =
        buildJsonObject {
            put("childId", childId)
            put("accountId", accountId)
            put("role", role)
            put("deleted", deleted)
        }
}

/** Una sesión con un token de acceso que el [com.pasitos.app.core.network.FakeServer] acepta. */
class FixedSessionTokens(private val accessToken: String = "access-1") : SessionTokens {
    override suspend fun current() = TokenPair(accessToken, Instant.parse("2026-10-19T16:00:00Z"), "refresh-1")

    override suspend fun refresh(rejectedAccessToken: String?, renew: suspend (refreshToken: String) -> RefreshResult): TokenPair? = null
}

class RecordingSyncScheduler : SyncScheduler {
    val requests = mutableListOf<SyncTrigger>()

    override fun schedulePeriodic() = Unit

    override fun requestSync(trigger: SyncTrigger) {
        requests += trigger
    }

    override fun cancelAll() = Unit
}

class SignedInAuthRepository(session: AuthSession = ChildTestData.session) : AuthRepository {
    override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedIn(session))
    override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(session)

    override suspend fun login(email: String, password: String) = Result.success(currentSession.value!!)
    override suspend fun loginWithGoogle() = Result.success(currentSession.value!!)
    override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean) =
        Result.success(currentSession.value!!)

    override suspend fun resendEmailVerification(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun requestPasswordReset(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun isEmailVerified(): Result<Boolean> = Result.success(true)

    override suspend fun pendingChanges(): Int = 0
    override suspend fun logout(): Result<Unit> = Result.success(Unit)
}

/** Repositorio en memoria para las pruebas de las pantallas. */
class FakeChildRepository(children: List<Child> = emptyList()) : ChildRepository {

    val state = MutableStateFlow(children)
    override val children: StateFlow<List<Child>> = state
    override val activeChildId = MutableStateFlow(children.firstOrNull()?.id)

    var role: GuardianRole? = GuardianRole.PRINCIPAL
    var onlyGuardian: Result<Boolean> = Result.success(true)
    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    val added = mutableListOf<Pair<ChildInput, Boolean>>()
    val updated = mutableListOf<Pair<String, ChildInput>>()
    val deleted = mutableListOf<String>()

    override fun observeRole(childId: String): Flow<GuardianRole?> = state.map { role }

    override suspend fun addChild(input: ChildInput, representationDeclared: Boolean): Result<Child> =
        saveResult.map {
            added += input to representationDeclared
            input.toChild("new-child")
        }

    override suspend fun updateChild(childId: String, input: ChildInput): Result<Child> =
        saveResult.map {
            updated += childId to input
            input.toChild(childId)
        }

    override suspend fun isOnlyGuardian(childId: String): Result<Boolean> = onlyGuardian

    override suspend fun deleteChild(childId: String): Result<Unit> =
        deleteResult.onSuccess { deleted += childId }

    override suspend fun selectChild(childId: String) {
        activeChildId.value = childId
    }

    private fun ChildInput.toChild(id: String) = Child(
        id = id,
        firstName = firstName,
        lastName = lastName,
        sex = sex,
        birthDate = birthDate,
        gestationalWeeks = gestationalWeeks,
    )
}
