package com.pasitos.backend.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    /** Abre el pool, aplica las migraciones y conecta Exposed. Quien llama cierra el pool al apagar. */
    fun init(config: ApplicationConfig): HikariDataSource {
        val dataSource = createDataSource(config)
        migrate(dataSource)
        Database.connect(dataSource)
        return dataSource
    }

    /**
     * Las migraciones se versionan por fecha y hora (ADR-23). `outOfOrder` aplica también una
     * migración con fecha anterior a la última aplicada: pasa cuando una rama se mergea después
     * de otra más nueva.
     */
    fun migrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .outOfOrder(true)
            .load()
            .migrate()
    }

    private fun createDataSource(config: ApplicationConfig): HikariDataSource {
        val host = config.property("db.host").getString()
        val port = config.property("db.port").getString()
        val name = config.property("db.name").getString()
        val user = config.property("db.user").getString()
        val password = config.property("db.password").getString()

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$name"
            username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }
}
