package com.pasitos.backend.seed

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.config.DatabaseFactory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.config.ApplicationConfig

/** Evita que una invocación sin destino explícito modifique la base predeterminada del backend. */
fun requireDemoTarget(database: String, confirmation: String?) {
    require(database.isNotBlank() && confirmation == database) {
        "Confirmá la base ficticia de destino: DEMO_DATABASE debe coincidir exactamente con DB_NAME"
    }
}

fun main(args: Array<String>) {
    require(args.isEmpty() || args.contentEquals(arrayOf("--preview"))) { "Uso: seedDemoData [--args=--preview]" }
    val data = readDemoData()
    val clock = ArgentinaClock()
    data.validate(clock.today())
    if (args.isNotEmpty()) {
        println("Demo T-18 válida: ${data.guardians.size} cuentas, ${data.children.size} niños, ${data.children.sumOf { it.measurements.size }} mediciones. Sin conectar a la base.")
        println("Los estados y cruces de crecimiento siguen pendientes de verificar con I2.")
        return
    }
    dotenv { ignoreIfMissing = true; systemProperties = true }
    fun setting(name: String): String? = System.getenv(name) ?: System.getProperty(name)
    val config = ApplicationConfig("application.conf")
    requireDemoTarget(config.property("db.name").getString(), setting("DEMO_DATABASE"))
    val password = requireNotNull(setting("DEMO_PASSWORD")) { "Falta DEMO_PASSWORD; no se imprime ni se almacena en archivos" }
    DatabaseFactory.init(config).use {
        val loaded = DemoSeeder(clock, config.property("terms.version").getString(), config.property("declaration.version").getString())
            .seed(data, password)
        println(if (loaded) "Datos ficticios T-18 cargados." else "La demo T-18 ya existe: no se modificó ni se duplicó ningún registro.")
    }
}
