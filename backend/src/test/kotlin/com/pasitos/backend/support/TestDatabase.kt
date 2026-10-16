package com.pasitos.backend.support

import com.pasitos.backend.config.DatabaseFactory
import io.ktor.server.config.MapApplicationConfig
import java.sql.Connection
import java.sql.DriverManager
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * PostgreSQL 16 real en Docker, uno solo para toda la corrida de pruebas (ADR-19). Testcontainers
 * lo borra al terminar. Las migraciones se aplican al arrancar, con la misma configuración que el
 * backend.
 */
object TestDatabase {

    private val container = PostgreSQLContainer("postgres:16-alpine").apply { start() }

    init {
        DatabaseFactory.migrate(
            PGSimpleDataSource().apply {
                setURL(container.jdbcUrl)
                user = container.username
                password = container.password
            },
        )
    }

    /** Configuración del backend apuntando a este contenedor. */
    fun config() = MapApplicationConfig(
        "db.host" to container.host,
        "db.port" to container.getMappedPort(5432).toString(),
        "db.name" to container.databaseName,
        "db.user" to container.username,
        "db.password" to container.password,
        "jwt.secret" to "secreto-de-prueba",
        "jwt.issuer" to "pasitos-backend",
        "jwt.audience" to "pasitos-app",
        "jwt.expirationMinutes" to "480",
    )

    /** Vacía todas las tablas, salvo la historia de Flyway, para que cada prueba arranque limpia. */
    fun reset() {
        connect().use { connection ->
            val tables = connection.createStatement()
                .executeQuery("SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'")
                .use { rows -> generateSequence { if (rows.next()) rows.getString(1) else null }.toList() }
            connection.createStatement().use { it.execute("TRUNCATE ${tables.joinToString()} CASCADE") }
        }
    }

    /** Conexión directa, para verificar en la base lo que la API no muestra. */
    fun connect(): Connection = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)

    /** Primera columna de cada fila de [sql]. */
    fun <T> column(sql: String, vararg params: Any): List<T> = connect().use { connection ->
        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, param -> statement.setObject(index + 1, param) }
            statement.executeQuery().use { rows ->
                @Suppress("UNCHECKED_CAST")
                generateSequence { if (rows.next()) rows.getObject(1) as T else null }.toList()
            }
        }
    }
}
