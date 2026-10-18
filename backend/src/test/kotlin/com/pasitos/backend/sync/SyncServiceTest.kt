package com.pasitos.backend.sync

import com.pasitos.backend.common.Caller
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.sync.SyncMutation
import com.pasitos.shared.api.sync.SyncMutationStatus
import com.pasitos.shared.api.sync.SyncOp
import com.pasitos.backend.common.tx
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.insert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * I0-08: prueba el motor genérico de sincronización con [FakeSyncEntity], ajena a cualquier
 * dominio real (14 · Sincronización). Usa [apiTest] solo para levantar la base real y crear
 * cuentas/hijos por HTTP con los helpers ya existentes; el [SyncService] bajo prueba se
 * instancia acá mismo, con su propio [SyncRegistry] - nunca el de `Dependencies.kt`.
 */
class SyncServiceTest {

    private fun service(entity: FakeSyncEntity = FakeSyncEntity()) =
        SyncService(SyncRegistry().apply { register(entity) }, SyncRepository())

    private fun upsert(id: UUID, childId: UUID, value: String) = SyncMutation(
        mutationId = UUID.randomUUID().toString(),
        entity = "TEST_ENTITY",
        op = SyncOp.UPSERT,
        data = buildJsonObject {
            put("id", id.toString())
            put("childId", childId.toString())
            put("value", value)
        },
    )

    private fun delete(id: UUID) = SyncMutation(
        mutationId = UUID.randomUUID().toString(),
        entity = "TEST_ENTITY",
        op = SyncOp.DELETE,
        data = buildJsonObject { put("id", id.toString()) },
    )

    @Test
    fun `changes devuelve solo lo posterior al cursor`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val childId = UUID.fromString(child.id)

        val entity = FakeSyncEntity()
        entity.seed(childId, "uno")
        val sync = service(entity)

        val first = sync.changes(caller, cursor = 0, patients = null)
        assertEquals(1, first.changes.getValue("TEST_ENTITY").size)
        assertFalse(first.hasMore)

        entity.seed(childId, "dos")
        val second = sync.changes(caller, cursor = first.cursor, patients = null)
        assertEquals(1, second.changes.getValue("TEST_ENTITY").size)
        assertEquals("dos", second.changes.getValue("TEST_ENTITY").single().jsonObject["value"]?.jsonPrimitive?.content)

        // El cursor nuevo no vuelve a traer lo ya recibido: sin filas, la entidad ni aparece
        // en el mapa (no se manda "TEST_ENTITY": [] de más).
        val third = sync.changes(caller, cursor = second.cursor, patients = null)
        assertTrue(third.changes["TEST_ENTITY"].orEmpty().isEmpty())
    }

    @Test
    fun `pagina de a 500 y avisa que hay mas`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val childId = UUID.fromString(child.id)

        val entity = FakeSyncEntity()
        repeat(520) { entity.seed(childId, "fila-$it") }
        val sync = service(entity)

        val first = sync.changes(caller, cursor = 0, patients = null)
        assertEquals(500, first.changes.getValue("TEST_ENTITY").size)
        assertTrue(first.hasMore)

        val second = sync.changes(caller, cursor = first.cursor, patients = null)
        assertEquals(20, second.changes.getValue("TEST_ENTITY").size)
        assertFalse(second.hasMore)
    }

    @Test
    fun `una fila borrada viaja como lapida`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val childId = UUID.fromString(child.id)

        val entity = FakeSyncEntity()
        val id = entity.seed(childId, "a borrar")
        val sync = service(entity)

        val push = sync.push(caller, listOf(delete(id)))
        assertEquals(SyncMutationStatus.APPLIED, push.results.single().status)

        val changes = sync.changes(caller, cursor = 0, patients = null)
        val row = changes.changes.getValue("TEST_ENTITY").single { it.jsonObject["id"]?.jsonPrimitive?.content == id.toString() }
        assertTrue(row.jsonObject["deleted"]?.jsonPrimitive?.content.toBoolean())
    }

    @Test
    fun `UPSERT es idempotente, reenviar el mismo lote da el mismo resultado`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val childId = UUID.fromString(child.id)

        val sync = service()
        val id = UUID.randomUUID()
        val mutation = upsert(id, childId, "repetido")

        val first = sync.push(caller, listOf(mutation)).results.single()
        val second = sync.push(caller, listOf(mutation)).results.single()

        assertEquals(SyncMutationStatus.APPLIED, first.status)
        assertEquals(SyncMutationStatus.APPLIED, second.status)
        // Reenviar el mismo UPSERT no crea una segunda fila: sigue habiendo una sola.
        val changes = sync.changes(caller, cursor = 0, patients = null)
        assertEquals(1, changes.changes.getValue("TEST_ENTITY").count { it.jsonObject["id"]?.jsonPrimitive?.content == id.toString() })
    }

    @Test
    fun `newChildIds avisa un hijo que entro al alcance despues del cursor`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val sync = service()

        val before = sync.changes(caller, cursor = 0, patients = null)

        val newChild = client.createChild(auth.tokens.accessToken, name = "Nuevo")
        val after = sync.changes(caller, cursor = before.cursor, patients = null)

        assertTrue(after.newChildIds.contains(newChild.id))
    }

    @Test
    fun `revokedChildIds avisa un hijo al que se perdio acceso`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val sync = service()

        val before = sync.changes(caller, cursor = 0, patients = null)

        tx {
            AccessRevocations.insert {
                it[id] = UUID.randomUUID()
                it[accountId] = caller.accountId
                it[childId] = UUID.fromString(child.id)
            }
        }

        val after = sync.changes(caller, cursor = before.cursor, patients = null)
        assertTrue(after.revokedChildIds.contains(child.id))
    }

    @Test
    fun `una mutacion rechazada no interrumpe el lote y viaja con codigo y regla`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val childId = UUID.fromString(child.id)
        val sync = service()

        val ok = upsert(UUID.randomUUID(), childId, "valido")
        val rejected = upsert(UUID.randomUUID(), childId, "REJECT")

        val results = sync.push(caller, listOf(ok, rejected)).results

        assertEquals(SyncMutationStatus.APPLIED, results[0].status)
        assertEquals(SyncMutationStatus.REJECTED, results[1].status)
        assertEquals(ErrorCode.BUSINESS_RULE, results[1].code)
        assertEquals("RN-TEST-01", results[1].rule)
    }
}
