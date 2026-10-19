package com.pasitos.backend.sync

import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.child.ChildStatus
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.tx
import com.pasitos.backend.measurement.MeasurementRepository
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.sync.SyncMutation
import com.pasitos.shared.api.sync.SyncMutationStatus
import com.pasitos.shared.api.sync.SyncOp
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `MEASUREMENT` en `/sync/push` y `/sync/changes` (I1-B6: RF-CRE-01, RF-CRE-02; RN-CRE-03,
 * RN-CRE-11, RN-CRE-12; ADR-28). Igual que [SyncServiceTest], instancia su propio [SyncService]
 * con un [SyncRegistry] propio - nunca el de `Dependencies.kt` -, y usa [apiTest] solo para la
 * base real y para crear cuentas/hijos por HTTP.
 */
class MeasurementSyncEntityTest {

    private fun service() = SyncService(
        SyncRegistry().apply { register(MeasurementSyncEntity(MeasurementRepository(), ChildRepository(), ArgentinaClock())) },
        SyncRepository(),
    )

    private fun upsert(
        id: UUID,
        childId: String,
        measuredOn: String,
        weightKg: Double? = null,
        lengthCm: Double? = null,
        lengthPosition: String? = null,
        headCircumferenceCm: Double? = null,
        outlierConfirmed: Boolean? = null,
    ) = SyncMutation(
        mutationId = UUID.randomUUID().toString(),
        entity = "MEASUREMENT",
        op = SyncOp.UPSERT,
        data = buildJsonObject {
            put("id", id.toString())
            put("childId", childId)
            put("measuredOn", measuredOn)
            weightKg?.let { put("weightKg", it) }
            lengthCm?.let { put("lengthCm", it) }
            lengthPosition?.let { put("lengthPosition", it) }
            headCircumferenceCm?.let { put("headCircumferenceCm", it) }
            outlierConfirmed?.let { put("outlierConfirmed", it) }
        },
    )

    private fun delete(id: UUID) = SyncMutation(
        mutationId = UUID.randomUUID().toString(),
        entity = "MEASUREMENT",
        op = SyncOp.DELETE,
        data = buildJsonObject { put("id", id.toString()) },
    )

    private fun JsonElement.field(name: String) = jsonObject[name]?.jsonPrimitive?.content

    @Test
    fun `RF-CRE-01 un alta valida se guarda y llega por sync changes`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken, birthDate = "2026-01-10")
        val sync = service()
        val id = UUID.randomUUID()

        val result = sync.push(
            caller,
            listOf(upsert(id, child.id, "2026-10-01", weightKg = 6.4, lengthCm = 62.55, lengthPosition = "LYING", headCircumferenceCm = 40.8, outlierConfirmed = true)),
        ).results.single()

        assertEquals(SyncMutationStatus.APPLIED, result.status)
        val row = sync.changes(caller, cursor = 0, patients = null).changes.getValue("MEASUREMENT")
            .single { it.field("id") == id.toString() }
        assertEquals(child.id, row.field("childId"))
        assertEquals("2026-10-01", row.field("measuredOn"))
        assertEquals("6.4", row.field("weightKg"))
        assertEquals("62.6", row.field("lengthCm"))
        assertEquals("LYING", row.field("lengthPosition"))
        assertEquals(auth.account.id, row.field("authorAccountId"))
        assertEquals("GUARDIAN", row.field("authorRole"))
        assertEquals("true", row.jsonObject["outlierConfirmed"]?.jsonPrimitive?.content)
        assertEquals("false", row.jsonObject["deleted"]?.jsonPrimitive?.content)
    }

    @Test
    fun `RF-CRE-01 CA2 fecha futura o anterior al nacimiento se rechaza`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken, birthDate = "2026-06-15")
        val tomorrow = ArgentinaClock().today().plusDays(1).toString()
        val sync = service()

        val future = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, tomorrow, weightKg = 6.4))).results.single()
        val beforeBirth = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-01-01", weightKg = 6.4))).results.single()

        assertEquals(SyncMutationStatus.REJECTED, future.status)
        assertEquals(ErrorCode.INVALID_INPUT, future.code)
        assertEquals(SyncMutationStatus.REJECTED, beforeBirth.status)
        assertEquals(ErrorCode.INVALID_INPUT, beforeBirth.code)
    }

    @Test
    fun `sin ningun valor o con un valor fuera del rango absoluto se rechaza`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val sync = service()

        val noValues = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-01"))).results.single()
        val weightOutOfRange = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-01", weightKg = 0.0))).results.single()
        val lengthOutOfRange = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-01", lengthCm = 500.0, lengthPosition = "LYING"))).results.single()

        listOf(noValues, weightOutOfRange, lengthOutOfRange).forEach {
            assertEquals(SyncMutationStatus.REJECTED, it.status)
            assertEquals(ErrorCode.INVALID_INPUT, it.code)
        }
    }

    @Test
    fun `RN-CRE-03 desde los 60 meses no se admite medir acostado`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken, birthDate = "2021-10-16")
        val sync = service()

        val lying = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-16", lengthCm = 98.0, lengthPosition = "LYING"))).results.single()
        val standing = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-16", lengthCm = 98.0, lengthPosition = "STANDING"))).results.single()

        assertEquals(SyncMutationStatus.REJECTED, lying.status)
        assertEquals(ErrorCode.INVALID_INPUT, lying.code)
        assertEquals(SyncMutationStatus.APPLIED, standing.status)
    }

    @Test
    fun `RN-CRE-12 una segunda medicion del mismo autor en la misma fecha se rechaza`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val sync = service()
        sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-01", weightKg = 6.4)))

        val result = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-01", weightKg = 6.5))).results.single()

        assertEquals(SyncMutationStatus.REJECTED, result.status)
        assertEquals(ErrorCode.BUSINESS_RULE, result.code)
        assertEquals("RN-CRE-12", result.rule)
    }

    @Test
    fun `RN-CRE-12 editar la propia medicion a una fecha que ya usaba otra se rechaza, pero no choca consigo misma`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val sync = service()
        val firstId = UUID.randomUUID()
        sync.push(caller, listOf(upsert(firstId, child.id, "2026-10-01", weightKg = 6.4)))
        val secondId = UUID.randomUUID()
        sync.push(caller, listOf(upsert(secondId, child.id, "2026-10-05", weightKg = 6.5)))

        // Reeditar la primera con su misma fecha (no choca consigo misma).
        val sameDate = sync.push(caller, listOf(upsert(firstId, child.id, "2026-10-01", weightKg = 6.45))).results.single()
        // Pero cambiarle la fecha a la del otro, sí.
        val conflicting = sync.push(caller, listOf(upsert(firstId, child.id, "2026-10-05", weightKg = 6.45))).results.single()

        assertEquals(SyncMutationStatus.APPLIED, sameDate.status)
        assertEquals(SyncMutationStatus.REJECTED, conflicting.status)
        assertEquals("RN-CRE-12", conflicting.rule)
    }

    @Test
    fun `RF-CRE-02 el autor edita su propia medicion y el cambio llega por sync changes`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val sync = service()
        val id = UUID.randomUUID()
        sync.push(caller, listOf(upsert(id, child.id, "2026-10-01", weightKg = 6.4)))

        val edited = sync.push(caller, listOf(upsert(id, child.id, "2026-10-01", weightKg = 7.1))).results.single()

        assertEquals(SyncMutationStatus.APPLIED, edited.status)
        val row = sync.changes(caller, cursor = 0, patients = null).changes.getValue("MEASUREMENT")
            .filter { it.field("id") == id.toString() }
            .maxByOrNull { it.field("id").orEmpty() }
        assertEquals("7.1", row?.field("weightKg"))
        assertEquals(
            1L,
            TestDatabase.column<Long>("SELECT count(*) FROM measurements WHERE id = ?", id).single(),
        )
    }

    @Test
    fun `solo el autor edita o borra, otro tutor del mismo nino lo recibe como no encontrada`() = apiTest { client ->
        val ana = client.register(email = "ana@example.com")
        val beto = client.register(email = "beto@example.com")
        val anaCaller = Caller(UUID.fromString(ana.account.id), UUID.randomUUID())
        val betoCaller = Caller(UUID.fromString(beto.account.id), UUID.randomUUID())
        val child = client.createChild(ana.tokens.accessToken)
        seedSecondaryGuardianship(child.id, beto.account.id)
        val sync = service()
        val id = UUID.randomUUID()
        sync.push(anaCaller, listOf(upsert(id, child.id, "2026-10-01", weightKg = 6.4)))

        val editByOther = sync.push(betoCaller, listOf(upsert(id, child.id, "2026-10-01", weightKg = 9.0))).results.single()
        val deleteByOther = sync.push(betoCaller, listOf(delete(id))).results.single()

        assertEquals(SyncMutationStatus.REJECTED, editByOther.status)
        assertEquals(ErrorCode.MEASUREMENT_NOT_FOUND, editByOther.code)
        assertEquals(SyncMutationStatus.REJECTED, deleteByOther.status)
        assertEquals(ErrorCode.MEASUREMENT_NOT_FOUND, deleteByOther.code)
    }

    @Test
    fun `un tutor sin acceso al nino recibe child_not_found`() = apiTest { client ->
        val ana = client.register(email = "ana@example.com")
        val beto = client.register(email = "beto@example.com")
        val betoCaller = Caller(UUID.fromString(beto.account.id), UUID.randomUUID())
        val child = client.createChild(ana.tokens.accessToken)
        val sync = service()

        val result = sync.push(betoCaller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-01", weightKg = 6.4))).results.single()

        assertEquals(SyncMutationStatus.REJECTED, result.status)
        assertEquals(ErrorCode.CHILD_NOT_FOUND, result.code)
    }

    @Test
    fun `RF-CRE-01 CA6 un perfil finalizado no admite mediciones nuevas`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        finalize(child.id)
        val sync = service()

        val result = sync.push(caller, listOf(upsert(UUID.randomUUID(), child.id, "2026-10-01", weightKg = 6.4))).results.single()

        assertEquals(SyncMutationStatus.REJECTED, result.status)
        assertEquals(ErrorCode.BUSINESS_RULE, result.code)
        assertEquals("RF-CRE-01-CA6", result.rule)
    }

    @Test
    fun `RF-CRE-02 borrado logico, viaja como lapida y es idempotente`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val sync = service()
        val id = UUID.randomUUID()
        sync.push(caller, listOf(upsert(id, child.id, "2026-10-01", weightKg = 6.4)))

        val first = sync.push(caller, listOf(delete(id))).results.single()
        val second = sync.push(caller, listOf(delete(id))).results.single()

        assertEquals(SyncMutationStatus.APPLIED, first.status)
        assertEquals(SyncMutationStatus.APPLIED, second.status)
        val row = sync.changes(caller, cursor = 0, patients = null).changes.getValue("MEASUREMENT")
            .single { it.field("id") == id.toString() }
        assertEquals("true", row.jsonObject["deleted"]?.jsonPrimitive?.content)
    }

    @Test
    fun `UPSERT es idempotente, reenviar el mismo lote no crea una fila nueva`() = apiTest { client ->
        val auth = client.register()
        val caller = Caller(UUID.fromString(auth.account.id), UUID.randomUUID())
        val child = client.createChild(auth.tokens.accessToken)
        val sync = service()
        val mutation = upsert(UUID.randomUUID(), child.id, "2026-10-01", weightKg = 6.4)

        val first = sync.push(caller, listOf(mutation)).results.single()
        val second = sync.push(caller, listOf(mutation)).results.single()

        assertEquals(SyncMutationStatus.APPLIED, first.status)
        assertEquals(SyncMutationStatus.APPLIED, second.status)
        assertEquals(1L, TestDatabase.column<Long>("SELECT count(*) FROM measurements").single())
    }

    private fun seedSecondaryGuardianship(childId: String, accountId: String) {
        TestDatabase.connect().use {
            it.createStatement().execute(
                "INSERT INTO guardianships (child_id, account_id, role) VALUES ('$childId', '$accountId', 'SECONDARY')",
            )
        }
    }

    private suspend fun finalize(childId: String) = tx {
        com.pasitos.backend.child.Children.update({ com.pasitos.backend.child.Children.id eq UUID.fromString(childId) }) {
            it[status] = ChildStatus.FINALIZED
        }
    }
}
