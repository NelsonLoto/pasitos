package com.pasitos.app.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.SyncStateDao
import com.pasitos.app.core.sync.local.SyncStateEntity
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.shared.api.sync.SyncChangesResponse
import com.pasitos.shared.api.sync.SyncSnapshotResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Filas recibidas en una sincronización y filas que no se pudieron leer. */
data class SyncResult(val received: Int, val skipped: Int)

/**
 * Trae los cambios del servidor a la base local (14 · Sincronización §2 y §6). Cada entidad se
 * guarda con su [SyncedEntity]; el motor solo maneja el cursor, las fotos de los niños nuevos y
 * las revocaciones. Enviar la bandeja de salida llega con I1-A6.
 *
 * Una sola sincronización a la vez: un [Mutex] hace esperar a la segunda. Las entidades que se
 * escriben por REST guardan la respuesta con [withLock], así una sincronización en curso no las
 * pisa con una versión vieja.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val httpClient: HttpClient,
    private val database: PasitosDatabase,
    private val syncStateDao: SyncStateDao,
    private val pendingMutationDao: PendingMutationDao,
    entities: Set<@JvmSuppressWildcards SyncedEntity>,
    private val clock: ArgentinaClock,
) {

    private val mutex = Mutex()
    private val entities = entities.associateBy { it.name }

    /** Trae todos los cambios pendientes. Lanza `NoConnectionException` o `ApiException` sin guardar la página que falló. */
    suspend fun sync(): SyncResult = mutex.withLock { pull() }

    /** Ejecuta [block] sin ninguna sincronización en curso, por ejemplo un pedido REST y el guardado de su respuesta. */
    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }

    /**
     * Borra todos los datos locales de un niño y sus cambios pendientes, por ejemplo al eliminarlo.
     * Se llama dentro de [withLock].
     */
    suspend fun forgetChild(childId: String) = database.withTransaction { deleteChild(childId) }

    private suspend fun pull(): SyncResult {
        val state = syncStateDao.get() ?: SyncStateEntity()
        var cursor = state.cursor
        var received = 0
        var skipped = 0
        do {
            val page = changes(cursor)
            // Las fotos se piden antes de guardar: si una falla, el cursor no avanza y la próxima
            // sincronización vuelve a recibir esos niños en newChildIds
            val snapshots = page.newChildIds.mapNotNull { snapshot(it) }
            database.withTransaction {
                // Primero las revocaciones: un niño que se perdió y se recuperó vuelve con su foto
                page.revokedChildIds.forEach { deleteChild(it) }
                for (changes in listOf(page.changes) + snapshots.map { it.changes }) {
                    val result = apply(changes)
                    received += result.received
                    skipped += result.skipped
                }
                syncStateDao.save(state.copy(cursor = page.cursor))
            }
            cursor = page.cursor
        } while (page.hasMore)
        syncStateDao.save(SyncStateEntity(cursor = cursor, lastSyncedAt = clock.now()))
        return SyncResult(received, skipped)
    }

    private suspend fun apply(changes: Map<String, List<JsonElement>>): SyncResult {
        var received = 0
        var skipped = 0
        for ((name, rows) in changes) {
            // Una entidad sin manejador llega con otra tarea, que sube la versión de la base: hasta
            // el piloto la base se recrea y se vuelve a bajar todo desde el cursor 0
            val entity = entities[name] ?: continue
            var unreadable = 0
            for (row in rows) {
                try {
                    val fields = row.jsonObject
                    entity.apply(fields)
                    // Un niño eliminado se lleva sus datos de todas las entidades (RN-HIJ-08)
                    if (name == CHILD && fields["deleted"]?.jsonPrimitive?.booleanOrNull == true) {
                        fields["id"]?.jsonPrimitive?.contentOrNull?.let { deleteChild(it) }
                    }
                    received++
                } catch (_: IllegalArgumentException) {
                    // Incluye SerializationException. Sin el contenido: puede tener datos personales
                    unreadable++
                }
            }
            if (unreadable > 0) Log.w(TAG, "$unreadable filas de $name no se pudieron leer")
            skipped += unreadable
        }
        return SyncResult(received, skipped)
    }

    private suspend fun deleteChild(childId: String) {
        entities.values.forEach { it.deleteChild(childId) }
        pendingMutationDao.deleteForChild(childId)
    }

    private suspend fun changes(cursor: Long): SyncChangesResponse = withConnection {
        val response = httpClient.get("/api/v1/sync/changes") { parameter("cursor", cursor) }
        if (response.status != HttpStatusCode.OK) throw response.toApiException()
        response.body()
    }

    private suspend fun snapshot(childId: String): SyncSnapshotResponse? = withConnection {
        val response = httpClient.get("/api/v1/sync/snapshot/$childId")
        when (response.status) {
            HttpStatusCode.OK -> response.body()
            // Perdió el acceso entre los dos pedidos: lo va a avisar revokedChildIds
            HttpStatusCode.NotFound -> null
            else -> throw response.toApiException()
        }
    }

    companion object {
        /** El niño es la unidad del alcance: su lápida borra los datos de todas las entidades. */
        const val CHILD = "CHILD"

        private const val TAG = "SyncEngine"
    }
}
