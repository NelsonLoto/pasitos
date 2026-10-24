package com.pasitos.app.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.app.core.sync.local.MutationEntity
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.core.sync.local.SyncStateDao
import com.pasitos.app.core.sync.local.SyncStateEntity
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.shared.api.sync.SyncChangesResponse
import com.pasitos.shared.api.sync.SyncMutation
import com.pasitos.shared.api.sync.SyncMutationStatus
import com.pasitos.shared.api.sync.SyncOp
import com.pasitos.shared.api.sync.SyncPushRequest
import com.pasitos.shared.api.sync.SyncPushResponse
import com.pasitos.shared.api.sync.SyncSnapshotResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Filas recibidas en una sincronización, filas que no se pudieron leer, y cambios de la bandeja de
 * salida que el servidor aplicó o rechazó.
 */
data class SyncResult(val received: Int, val skipped: Int, val applied: Int = 0, val rejected: Int = 0)

/**
 * Envía la bandeja de salida y trae los cambios del servidor a la base local (14 · Sincronización
 * §2, §3 y §6). Cada entidad se guarda con su [SyncedEntity] y recibe la respuesta a sus cambios
 * con su [OutboxEntity]; el motor solo maneja la bandeja, el cursor, las fotos de los niños nuevos
 * y las revocaciones.
 *
 * Una sola sincronización a la vez: un [Mutex] hace esperar a la segunda. Las entidades que se
 * escriben por REST guardan la respuesta con [withLock], así una sincronización en curso no las
 * pisa con una versión vieja. Las que se escriben en el teléfono no esperan: al traer cambios, el
 * motor no pisa un registro que todavía tiene cambios en la bandeja (decisión de I1-A6).
 */
@Singleton
class SyncEngine @Inject constructor(
    private val httpClient: HttpClient,
    private val database: PasitosDatabase,
    private val syncStateDao: SyncStateDao,
    private val pendingMutationDao: PendingMutationDao,
    entities: Set<@JvmSuppressWildcards SyncedEntity>,
    outboxEntities: Set<@JvmSuppressWildcards OutboxEntity>,
    private val clock: ArgentinaClock,
) {

    private val mutex = Mutex()
    private val entities = entities.associateBy { it.name }
    private val outboxEntities = outboxEntities.associateBy { it.entity }

    /**
     * Primero envía la bandeja y después trae los cambios, así lo que baja ya incluye lo enviado.
     * Lanza `NoConnectionException` o `ApiException` sin guardar la página que falló; si falla el
     * envío, no trae cambios.
     */
    suspend fun sync(): SyncResult = mutex.withLock {
        val (applied, rejected) = push()
        pull().copy(applied = applied, rejected = rejected)
    }

    /** Ejecuta [block] sin ninguna sincronización en curso, por ejemplo un pedido REST y el guardado de su respuesta. */
    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }

    /**
     * Borra todos los datos locales de un niño y sus cambios pendientes, por ejemplo al eliminarlo.
     * Se llama dentro de [withLock].
     */
    suspend fun forgetChild(childId: String) = database.withTransaction { deleteChild(childId) }

    /**
     * Envía la bandeja de a [PUSH_BATCH] cambios, del más viejo al más nuevo. Cada cambio
     * respondido sale de la bandeja y su entidad recibe el resultado, en una transacción por lote.
     */
    private suspend fun push(): Pair<Int, Int> {
        var applied = 0
        var rejected = 0
        while (true) {
            val batch = pendingMutationDao.oldest(PUSH_BATCH)
            if (batch.isEmpty()) break
            val results = send(batch).results.associateBy { it.mutationId }
            val answered = batch.filter { it.mutationId in results }
            database.withTransaction {
                pendingMutationDao.delete(answered.map { it.mutationId })
                for (mutation in answered) {
                    val result = results.getValue(mutation.mutationId)
                    val outboxEntity = outboxEntities[mutation.entity]
                    if (result.status == SyncMutationStatus.APPLIED) {
                        outboxEntity?.onApplied(mutation)
                        applied++
                    } else {
                        outboxEntity?.onRejected(mutation, result.code, result.message)
                        rejected++
                    }
                }
            }
            // Si faltó alguna respuesta, volver a pedir mandaría lo mismo: queda para la próxima
            if (answered.size < batch.size) {
                Log.w(TAG, "El servidor no respondió ${batch.size - answered.size} cambios de la bandeja")
                break
            }
        }
        if (rejected > 0) Log.w(TAG, "El servidor rechazó $rejected cambios de la bandeja")
        return applied to rejected
    }

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
            // Un cambio sin enviar gana: el próximo envío lo manda y recién después baja la versión
            // del servidor (RNF-OFF-02). Si no, la pantalla volvería a mostrar el valor viejo y un
            // registro borrado reaparecería hasta la próxima sincronización
            val pending = MutationEntity.entries.firstOrNull { it.name == name }
                ?.let { pendingMutationDao.entityIds(it).toSet() }
                .orEmpty()
            var unreadable = 0
            for (row in rows) {
                try {
                    val fields = row.jsonObject
                    if (pending.isNotEmpty() && fields["id"]?.jsonPrimitive?.contentOrNull in pending) {
                        received++
                        continue
                    }
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

    private suspend fun send(batch: List<PendingMutationEntity>): SyncPushResponse = withConnection {
        val request = SyncPushRequest(
            batch.map {
                SyncMutation(
                    mutationId = it.mutationId,
                    entity = it.entity.name,
                    op = SyncOp.valueOf(it.op.name),
                    data = Json.parseToJsonElement(it.payload),
                )
            },
        )
        val response = httpClient.post("/api/v1/sync/push") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status != HttpStatusCode.OK) throw response.toApiException()
        response.body()
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

        /** Cambios por pedido de `/sync/push`. */
        const val PUSH_BATCH = 100

        private const val TAG = "SyncEngine"
    }
}
