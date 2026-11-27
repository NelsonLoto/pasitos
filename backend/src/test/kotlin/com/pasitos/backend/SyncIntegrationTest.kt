package com.pasitos.backend

import com.pasitos.backend.auth.*
import com.pasitos.backend.common.*
import com.pasitos.backend.common.DatabaseConfig
import com.pasitos.backend.sync.*
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.sync.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.sql.DriverManager
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import kotlin.test.*

@Testcontainers
class SyncIntegrationTest {
    companion object {
        @Container @JvmStatic val postgres = PostgreSQLContainer("postgres:16-alpine")
        private lateinit var factory: DatabaseFactory
        private lateinit var passwords: PasswordHasher
        private lateinit var hash: String
        @BeforeAll @JvmStatic fun setup() {
            factory = DatabaseFactory(DatabaseConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password))
            passwords = PasswordHasher(); hash = passwords.hash("Synthetic12345")
            transaction(factory.database) {
                exec("CREATE TABLE sync_test_rows (id uuid PRIMARY KEY, kind text NOT NULL, child_id uuid NOT NULL, owner_id uuid NOT NULL, value integer NOT NULL, sync_version bigint NOT NULL, deleted_at timestamptz, updated_at timestamptz)")
                exec("CREATE TRIGGER sync_test_rows_version BEFORE INSERT OR UPDATE ON sync_test_rows FOR EACH ROW EXECUTE FUNCTION set_sync_version()")
            }
        }
        @AfterAll @JvmStatic fun close() = factory.close()
    }

    private object Rows : Table("sync_test_rows") {
        val id = uuid("id"); val kind = text("kind"); val childId = uuid("child_id")
        val ownerId = uuid("owner_id"); val value = integer("value"); val version = long("sync_version")
        val deletedAt = timestamp("deleted_at").nullable(); val updatedAt = timestamp("updated_at").nullable()
    }

    private lateinit var owner: Caller
    private lateinit var outsider: Caller
    private lateinit var child: UUID
    private lateinit var measurements: TestEntity
    private lateinit var habits: TestEntity
    private lateinit var service: SyncService

    @BeforeEach fun reset() {
        transaction(factory.database) {
            exec("TRUNCATE accounts, children, access_revocations, sync_test_rows CASCADE")
            exec("ALTER SEQUENCE sync_seq RESTART WITH 1")
            owner = account(); outsider = account(); child = child(owner.accountId)
        }
        measurements = TestEntity("MEASUREMENT", "measurements"); habits = TestEntity("HABIT", "habits")
        service = SyncService(factory.database, SyncRegistry(listOf(measurements, habits)))
    }

    private fun account(): Caller = UUID.randomUUID().let { id ->
        com.pasitos.backend.account.AccountsTable.insert {
            it[com.pasitos.backend.account.AccountsTable.id] = id; it[email] = "$id@example.test"
            it[firstName] = "Prueba"; it[lastName] = "Sintética"; it[passwordHash] = hash; it[isGuardian] = true
        }
        Caller(id, UUID.randomUUID())
    }

    private fun child(accountId: UUID): UUID = UUID.randomUUID().also { id ->
        org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec(
            "INSERT INTO children (id,first_name,last_name,sex,birth_date,gestational_weeks,created_by_account_id) VALUES ('$id','Prueba','Sintética','FEMALE','2026-01-01',40,'$accountId')",
        )
        org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec(
            "INSERT INTO guardianships (child_id,account_id,role) VALUES ('$id','$accountId','PRINCIPAL')",
        )
    }

    private fun seed(entity: String = "MEASUREMENT", target: UUID = child, author: UUID = owner.accountId, value: Int = 1): UUID =
        UUID.randomUUID().also { id -> Rows.insert {
            it[Rows.id] = id; it[kind] = entity; it[childId] = target; it[ownerId] = author; it[Rows.value] = value; it[version] = 0
        } }

    private fun mutation(id: UUID, op: String = "UPSERT", value: Int = 1, target: UUID? = child, rejectAfterWrite: Boolean = false) =
        SyncMutation(UUID.randomUUID().toString(), "MEASUREMENT", op, buildJsonObject {
            put("id", id.toString()); target?.let { put("childId", it.toString()) }; put("value", value); put("rejectAfterWrite", rejectAfterWrite)
        })

    @Test fun `I0-08 pagina fusionada respeta 500 incluso si una entidad tiene mas de 501`() {
        val ids = transaction(factory.database) { buildSet {
            repeat(550) { add(seed()); if (it < 5) add(seed("HABIT")) }
        } }
        val first = service.changes(owner, 0)
        assertTrue(first.hasMore); assertEquals(500, first.changes.values.sumOf { it.size })
        val second = service.changes(owner, first.cursor)
        assertFalse(second.hasMore); assertEquals(55, second.changes.values.sumOf { it.size })
        val received = (first.changes.values.flatten() + second.changes.values.flatten()).map { UUID.fromString(it["id"]!!.jsonPrimitive.content) }
        assertEquals(ids, received.toSet()); assertEquals(555, received.size)
        assertTrue(second.cursor > first.cursor)
        assertTrue(service.changes(owner, second.cursor).changes.isEmpty())
    }

    @Test fun `ADR-15 lapidas viajan como deleted pero no aparecen en la foto`() {
        val id = transaction(factory.database) { seed() }
        val cursor = service.changes(owner, 0).cursor
        transaction(factory.database) { Rows.update({ Rows.id eq id }) { it[deletedAt] = Instant.parse("2026-11-26T00:00:00Z") } }
        val deleted = service.changes(owner, cursor).changes.getValue("measurements").single()
        assertTrue(deleted["deleted"]!!.jsonPrimitive.boolean)
        assertTrue(service.snapshot(owner, child).changes.getValue("measurements").isEmpty())
    }

    @Test fun `ADR-16 nuevos hijos incluyen foto de filas anteriores al cursor`() {
        val target = transaction(factory.database) { child(outsider.accountId).also { seed(target = it, author = outsider.accountId) } }
        val before = service.changes(owner, 0).cursor
        transaction(factory.database) {
            org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec("INSERT INTO guardianships (child_id,account_id,role) VALUES ('$target','${owner.accountId}','SECONDARY')")
        }
        val page = service.changes(owner, before)
        assertEquals(listOf(target.toString()), page.newChildIds)
        assertTrue(page.changes.isEmpty())
        assertEquals(1, service.snapshot(owner, target).changes.getValue("measurements").size)
    }

    @Test fun `RNF-OFF-05 revocacion llega aunque el nino ya no este en alcance y se conserva tras recuperar acceso`() {
        val cursor = service.changes(owner, 0).cursor
        transaction(factory.database) {
            org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec("UPDATE guardianships SET deleted_at=now() WHERE account_id='${owner.accountId}'")
            SyncRepository().recordRevocation(owner.accountId, child)
        }
        assertEquals(listOf(child.toString()), service.changes(owner, cursor).revokedChildIds)
        assertEquals(ErrorCode.NOT_FOUND, assertFailsWith<SyncFailure> { service.snapshot(owner, child) }.code)
        transaction(factory.database) {
            org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec("UPDATE guardianships SET deleted_at=NULL WHERE account_id='${owner.accountId}'")
        }
        val regained = service.changes(owner, cursor)
        assertEquals(listOf(child.toString()), regained.revokedChildIds); assertEquals(listOf(child.toString()), regained.newChildIds)
        assertTrue(service.changes(outsider, cursor).revokedChildIds.isEmpty())
    }

    @Test fun `ADR-28 upsert repetido conserva version delete repetido aplica y una lapida no resucita`() {
        val id = UUID.randomUUID(); val request = SyncPushRequest(listOf(mutation(id)))
        val first = service.push(owner, request).results.single()
        assertEquals(SyncMutationStatus.APPLIED, first.status)
        assertEquals(first, service.push(owner, request).results.single())
        val delete = SyncPushRequest(listOf(mutation(id, op = "DELETE", target = null)))
        val deleted = service.push(owner, delete).results.single()
        assertEquals(deleted, service.push(owner, delete).results.single())
        assertEquals("measurement_not_found", service.push(owner, request).results.single().code)
        transaction(factory.database) { assertEquals(1L, Rows.selectAll().count()) }
    }

    @Test fun `I0-08 mutaciones mantienen orden transacciones independientes y rechazo con codigo y regla`() {
        val id = UUID.randomUUID(); val rejected = UUID.randomUUID()
        val result = service.push(owner, SyncPushRequest(listOf(mutation(id), mutation(rejected, rejectAfterWrite = true), mutation(id, value = 3))))
        assertEquals(listOf(SyncMutationStatus.APPLIED, SyncMutationStatus.REJECTED, SyncMutationStatus.APPLIED), result.results.map { it.status })
        assertEquals("business_rule", result.results[1].code); assertEquals("TEST-ROLLBACK", result.results[1].rule)
        transaction(factory.database) {
            val row = Rows.selectAll().single(); assertEquals(id, row[Rows.id]); assertEquals(3, row[Rows.value])
        }
    }

    @Test fun `ADR-16 ni lectura ni snapshot ni escrituras dan acceso a otro tutor`() {
        transaction(factory.database) { seed() }
        assertTrue(service.changes(outsider, 0).changes.isEmpty())
        assertEquals(ErrorCode.NOT_FOUND, assertFailsWith<SyncFailure> { service.snapshot(outsider, child) }.code)
        val result = service.push(outsider, SyncPushRequest(listOf(mutation(UUID.randomUUID())))).results.single()
        assertEquals("forbidden", result.code)
        transaction(factory.database) { assertEquals(1L, Rows.selectAll().count()) }
    }

    @Test fun `ADR-28 registro de produccion vacio rechaza escrituras y no publica entidades prematuramente`() {
        val production = SyncService(factory.database, productionSyncRegistry())
        transaction(factory.database) { seed() }
        assertTrue(production.changes(owner, 0).changes.isEmpty())
        assertEquals("invalid_input", production.push(owner, SyncPushRequest(listOf(mutation(UUID.randomUUID())))).results.single().code)
        val readOnly = mutation(UUID.randomUUID()).copy(entity = "CHILD")
        assertEquals("invalid_input", service.push(owner, SyncPushRequest(listOf(readOnly))).results.single().code)
    }

    @Test fun `I0-08 cursor invalido o futuro se rechaza sin retroceder`() {
        for (cursor in listOf(-1L, Long.MAX_VALUE)) assertEquals(ErrorCode.INVALID_INPUT, assertFailsWith<SyncFailure> { service.changes(owner, cursor) }.code)
    }

    @Test fun `I0-08 migracion actualiza una base previa sin perder filas ni retroceder versiones`() {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use {
            it.createStatement().use { statement -> statement.execute("CREATE DATABASE upgrade_sync") }
        }
        val url = "jdbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/upgrade_sync"
        Flyway.configure().dataSource(url, postgres.username, postgres.password)
            .locations("classpath:db/migration").target(MigrationVersion.fromVersion("2026.11.18.1455")).load().migrate()
        DriverManager.getConnection(url, postgres.username, postgres.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("INSERT INTO children (id,first_name,last_name,sex,birth_date,gestational_weeks,created_by_account_id,sync_version) VALUES (gen_random_uuid(),'Legacy','Test','FEMALE','2026-01-01',40,gen_random_uuid(),500)")
            }
        }
        DatabaseFactory(DatabaseConfig(postgres.host, postgres.firstMappedPort, "upgrade_sync", postgres.username, postgres.password)).use { upgraded ->
            transaction(upgraded.database) {
                assertEquals(500L, SyncRepository().committedVersion())
                exec("UPDATE children SET first_name='Updated'")
                assertTrue(exec("SELECT sync_version > 500 AND updated_at IS NOT NULL FROM children") { rows -> rows.next() && rows.getBoolean(1) } == true)
            }
        }
    }

    @Test fun `I0-08 todos los triggers previstos quedan instalados y las mutaciones invalidas no frenan el lote`() {
        transaction(factory.database) {
            assertEquals(6, exec("SELECT count(*) FROM pg_trigger WHERE NOT tgisinternal AND tgname IN ('children_sync_version','guardianships_sync_version','measurements_sync_version','applied_doses_sync_version','other_vaccines_sync_version','access_revocations_sync_version')") { rows -> rows.next(); rows.getInt(1) })
        }
        val valid = mutation(UUID.randomUUID())
        val result = service.push(owner, SyncPushRequest(listOf(valid.copy(mutationId = "bad"), valid.copy(op = "UNKNOWN"), valid))).results
        assertEquals(listOf(SyncMutationStatus.REJECTED, SyncMutationStatus.REJECTED, SyncMutationStatus.APPLIED), result.map { it.status })
        assertEquals(listOf("invalid_input", "invalid_input"), result.take(2).map { it.code })
    }

    @Test fun `ADR-15 una pagina espera escrituras pendientes y no pierde sus versiones`() = runBlocking {
        val inserted = CountDownLatch(1); val commit = CountDownLatch(1)
        val writer = async(Dispatchers.IO) { transaction(factory.database) {
            seed(); inserted.countDown(); check(commit.await(10, TimeUnit.SECONDS))
        } }
        assertTrue(inserted.await(10, TimeUnit.SECONDS))
        val reader = async(Dispatchers.IO) { service.changes(owner, 0) }
        try { delay(100); assertFalse(reader.isCompleted) } finally { commit.countDown() }
        writer.await(); val page = reader.await()
        assertEquals(1, page.changes.getValue("measurements").size)
        assertTrue(service.changes(owner, page.cursor).changes.isEmpty())
    }

    @Test fun `I0-08 rutas exigen sesion y validan cursor y pacientes sin ampliar alcance`() = testApplication {
        application {
            configureHttp(factory::isHealthy)
            val config = AuthConfig("synthetic-sync-signing-key-".repeat(4), requestsPerMinute = 100)
            configureAuth(AuthService(factory.database, config, AuthTokens(config, Clock.systemUTC()), passwords, Clock.systemUTC()), config)
            configureSync(service)
        }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/sync/changes").status)
        val login = client.post("/api/v1/auth/login") { contentType(ContentType.Application.Json); setBody(Json.encodeToString(LoginRequest("${owner.accountId}@example.test", "Synthetic12345"))) }
        val access = Json.parseToJsonElement(login.bodyAsText()).jsonObject["accessToken"]!!.jsonPrimitive.content
        val foreign = transaction(factory.database) { child(outsider.accountId).also { seed(target = it, author = outsider.accountId) } }
        val response = client.get("/api/v1/sync/changes?patients=$foreign") { bearerAuth(access) }
        assertEquals(HttpStatusCode.OK, response.status); assertTrue(Json.decodeFromString<SyncChangesResponse>(response.bodyAsText()).changes.isEmpty())
        val invalid = client.get("/api/v1/sync/changes?cursor=bad") { bearerAuth(access) }
        assertEquals(HttpStatusCode.BadRequest, invalid.status); assertEquals(ErrorCode.INVALID_INPUT, Json.decodeFromString<ErrorResponse>(invalid.bodyAsText()).code)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/sync/snapshot/$foreign") { bearerAuth(access) }.status)
        val push = client.post("/api/v1/sync/push") { bearerAuth(access); contentType(ContentType.Application.Json); setBody(Json.encodeToString(SyncPushRequest(listOf(mutation(UUID.randomUUID()))))) }
        assertEquals(HttpStatusCode.OK, push.status); assertEquals(SyncMutationStatus.APPLIED, Json.decodeFromString<SyncPushResponse>(push.bodyAsText()).results.single().status)
    }

    private inner class TestEntity(override val name: String, override val collectionName: String) : SyncEntity {
        override fun changesSince(caller: Caller, childIds: Set<UUID>, cursor: Long, limit: Int): List<SyncRow> = Rows.selectAll().where {
            (Rows.kind eq name) and (Rows.childId inList childIds) and (Rows.version greater cursor)
        }.orderBy(Rows.version).limit(limit).map { SyncRow(it[Rows.version], it[Rows.childId], json(it)) }
        override fun snapshot(caller: Caller, childId: UUID) = Rows.selectAll().where {
            (Rows.kind eq name) and (Rows.childId eq childId) and Rows.deletedAt.isNull()
        }.map(::json)

        override fun apply(caller: Caller, op: SyncOperation, data: JsonObject): SyncApplied {
            val id = UUID.fromString(data["id"]!!.jsonPrimitive.content)
            val current = Rows.selectAll().where { (Rows.id eq id) and (Rows.kind eq name) }.singleOrNull()
            val target = data["childId"]?.jsonPrimitive?.content?.let(UUID::fromString) ?: current?.get(Rows.childId)
            if (target != null && target !in SyncRepository().guardianChildren(caller.accountId)) throw SyncRejection("forbidden", "No tenés permiso para ese cambio")
            if (current != null && current[Rows.ownerId] != caller.accountId) throw SyncRejection("forbidden", "Solo el autor puede editar")
            if (op == SyncOperation.DELETE) {
                if (current == null) return SyncApplied(null)
                if (current[Rows.deletedAt] == null) Rows.update({ Rows.id eq id }) { it[deletedAt] = Instant.parse("2026-11-26T00:00:00Z") }
            } else {
                if (current?.get(Rows.deletedAt) != null) throw SyncRejection("measurement_not_found", "No se encontró ese registro")
                val value = data["value"]!!.jsonPrimitive.int
                if (value < 0) throw SyncRejection("business_rule", "El valor no cumple la regla", "TEST-VALIDATION")
                if (current == null) {
                    Rows.insert { it[Rows.id] = id; it[kind] = name; it[childId] = target ?: throw SyncRejection("invalid_input", "Falta el niño")
                        it[ownerId] = caller.accountId; it[Rows.value] = value; it[version] = 0 }
                } else {
                    if (target != current[Rows.childId]) throw SyncRejection("invalid_input", "No se puede mover el registro a otro niño")
                    if (current[Rows.value] != value) Rows.update({ Rows.id eq id }) { it[Rows.value] = value }
                }
                if (data["rejectAfterWrite"]?.jsonPrimitive?.boolean == true) throw SyncRejection("business_rule", "El cambio no cumple la regla", "TEST-ROLLBACK")
            }
            return SyncApplied(Rows.selectAll().where { Rows.id eq id }.single()[Rows.version])
        }
        private fun json(row: ResultRow) = buildJsonObject {
            put("id", row[Rows.id].toString()); put("childId", row[Rows.childId].toString()); put("value", row[Rows.value]); put("deleted", row[Rows.deletedAt] != null)
        }
    }
}
