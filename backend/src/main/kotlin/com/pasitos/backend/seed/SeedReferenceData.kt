package com.pasitos.backend.seed

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.config.DatabaseFactory
import com.pasitos.backend.vaccination.CalendarRepository
import com.pasitos.backend.vaccination.CalendarSeeder
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.config.ApplicationConfig
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SeedReferenceData")

/**
 * `./gradlew seedReferenceData`: carga en la base los datos de referencia de `backend/seed/` (13 ·
 * Base de datos, Datos iniciales). Solo para `local` y `ci`: en el piloto los gestiona el panel.
 *
 * Usa la misma configuración que el backend (`backend/.env` o las variables de entorno) y aplica
 * las migraciones antes, así funciona también con una base vacía. Se puede correr más de una vez:
 * lo que ya está cargado se saltea.
 */
fun main() {
    dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }
    val dataSource = DatabaseFactory.init(ApplicationConfig("application.conf"))
    try {
        val loaded = runBlocking { CalendarSeeder(CalendarRepository(), ArgentinaClock()).seed(File("seed/calendar")) }
        if (loaded.isEmpty()) {
            logger.info("Calendario Nacional: todas las versiones ya estaban cargadas")
        } else {
            logger.info("Calendario Nacional: se cargaron las versiones vigentes desde ${loaded.joinToString()}")
        }
    } catch (e: IllegalArgumentException) {
        logger.error("No se cargó el calendario. ${e.message}")
        exitProcess(1)
    } finally {
        dataSource.close()
    }
}
