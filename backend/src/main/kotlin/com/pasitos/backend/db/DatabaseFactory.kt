package com.pasitos.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        val dataSource = createDataSource(config)
        runMigrations(dataSource)
        Database.connect(dataSource)
    }

    private fun createDataSource(config: ApplicationConfig): DataSource {
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

    private fun runMigrations(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
