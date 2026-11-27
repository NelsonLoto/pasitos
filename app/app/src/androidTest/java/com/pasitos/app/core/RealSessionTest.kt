package com.pasitos.app.core

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pasitos.app.core.database.LocalDatabase
import com.pasitos.app.core.database.SyncState
import com.pasitos.app.core.network.backendConfiguration
import com.pasitos.app.core.security.*
import com.pasitos.app.data.auth.*
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.app.data.auth.remote.HttpAuthApi
import com.pasitos.shared.api.auth.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Requiere el backend local y adb reverse tcp:8080 tcp:8080; nunca usa cuentas reales. */
@RunWith(AndroidJUnit4::class)
class RealSessionTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val prefix = "real-session-test-${UUID.randomUUID()}"
    private val directory = File(app.cacheDir, prefix).apply { mkdirs() }
    private val context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("$prefix-$name", mode)
        override fun getDatabasePath(name: String) = File(directory, name)
        override fun deleteDatabase(name: String) = android.database.sqlite.SQLiteDatabase.deleteDatabase(getDatabasePath(name))
    }
    private val dataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionScopes = mutableListOf<CoroutineScope>()
    private val preferencesFile = File(directory, "secrets.preferences_pb")
    private val preferences = PreferenceDataStoreFactory.create(scope = dataScope) { preferencesFile }
    private val cipher = LocalCipher(context, "keyset", "${prefix}_master")
    private val store = SessionStore(preferences, cipher)
    private val local = LocalDatabase(context, DatabaseKeyStore(preferences, cipher))
    private val publicClient = HttpClient(OkHttp) { backendConfiguration("http://localhost:8080/") }
    private val api = HttpAuthApi(publicClient)
    private val effects = object : SessionEffects {
        var restarted = false
        override suspend fun stopSync() {}
        override suspend fun wipeDatabase() = local.wipe()
        override fun restart() { restarted = true }
        override fun scheduleRevocations() {}
    }
    private fun repository(): SessionRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(sessionScopes::add)
        return SessionRepository(store, api, effects, scope)
    }

    @After fun cleanup() = runBlocking(Dispatchers.IO) {
        sessionScopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        local.wipe(); publicClient.close(); dataScope.coroutineContext[Job]!!.cancelAndJoin()
        preferencesFile.delete(); directory.delete(); cipher.reset()
    }

    @Test fun RF_CTA_06_y_07_restaurarRenovarContraBackendRealYCerrarBorraBaseYTokens() = runBlocking(Dispatchers.IO) {
        val first = repository()
        val account = first.register(RegisterRequest("Prueba", "Dispositivo", "$prefix@example.test", "Synthetic12345", true))
        val tokens = first.tokens()!!
        local.get().syncState().save(SyncState(cursor = 160))
        assertTrue(context.getDatabasePath(LocalDatabase.NAME).exists())
        sessionScopes.last().coroutineContext[Job]!!.cancelAndJoin()

        // Simula el proceso cerrado: una instancia nueva solo conoce el DataStore cifrado.
        store.save(StoredSession(account, expiredToken(tokens.first), tokens.second))
        val restored = repository()
        assertEquals(account, (restored.state.first { it !is SessionState.Restoring } as SessionState.SignedIn).account)
        val authenticated = HttpClient(OkHttp) { backendConfiguration("http://localhost:8080/", restored) }
        try {
            assertEquals(HttpStatusCode.OK, authenticated.get("api/v1/me").status)
            assertNotEquals(tokens.second, store.read()!!.refreshToken)
            assertEquals(160L, local.get().syncState().read()!!.cursor)
            restored.close()
            assertNull(store.read()); assertTrue(effects.restarted)
            assertFalse(context.getDatabasePath(LocalDatabase.NAME).exists())
            assertEquals(HttpStatusCode.Unauthorized, authenticated.get("api/v1/me").status)
        } finally { authenticated.close() }
    }

    private fun expiredToken(original: String): String {
        val key = InstrumentationRegistry.getArguments().getString("authTestSigningKey")
            ?: "synthetic-app160-backend-signing-key-".padEnd(96, '0')
        val parts = original.split('.')
        fun sign(value: String): String = Base64.encodeToString(Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        }.doFinal(value.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        assertEquals("Usar solo la clave ficticia del backend de prueba", parts[2], sign(parts[0] + "." + parts[1]))
        val payload = Json.parseToJsonElement(Base64.decode(parts[1], Base64.URL_SAFE).toString(Charsets.UTF_8)).jsonObject
        val expired = payload["iat"]!!.jsonPrimitive.long - 1
        val modified = JsonObject(payload + mapOf("exp" to JsonPrimitive(expired), "iat" to JsonPrimitive(expired - 900)))
        val encoded = Base64.encodeToString(modified.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val unsigned = parts[0] + "." + encoded
        return unsigned + "." + sign(unsigned)
    }
}
