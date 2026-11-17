package com.pasitos.backend.common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseFactory(config: DatabaseConfig) : AutoCloseable {
    private val pool = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.user
        password = config.password
        maximumPoolSize = 5
        connectionTimeout = 5_000
        validationTimeout = 2_000
        initializationFailTimeout = 5_000
    })
    val database: Database = Database.connect(pool)

    init {
        try {
            Flyway.configure().dataSource(pool).locations("classpath:db/migration")
                .outOfOrder(true).cleanDisabled(true).load().migrate()
        } catch (failure: Exception) {
            pool.close()
            throw failure
        }
    }

    fun isHealthy(): Boolean = try {
        transaction(database) {
            exec("SELECT 1") { rows -> rows.next() && rows.getInt(1) == 1 } == true
        }
    } catch (_: Exception) { false }

    override fun close() = pool.close()
}
