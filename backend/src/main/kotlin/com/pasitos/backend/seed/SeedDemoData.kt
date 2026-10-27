package com.pasitos.backend.seed

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.config.DatabaseFactory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig

/** Evita que una invocación sin destino explícito modifique la base predeterminada del backend. */
fun requireDemoTarget(database: String, confirmation: String?) {
    require(database.isNotBlank() && confirmation == database) {
        "Confirmá la base ficticia de destino: DEMO_DATABASE debe coincidir exactamente con DB_NAME"
    }
}

/** Las variables del proceso prevalecen sobre .env también en HOCON (no se mutan System.properties). */
fun demoConfig(
    defaults: ApplicationConfig,
    environment: (String) -> String?,
    fileSetting: (String) -> String?,
): ApplicationConfig {
    val fields = listOf(
        "db.host" to "DB_HOST", "db.port" to "DB_PORT", "db.name" to "DB_NAME",
        "db.user" to "DB_USER", "db.password" to "DB_PASSWORD",
        "terms.version" to "TERMS_VERSION", "declaration.version" to "DECLARATION_VERSION",
    )
    return MapApplicationConfig(*fields.map { (path, name) ->
        path to (environment(name) ?: fileSetting(name) ?: defaults.property(path).getString())
    }.toTypedArray())
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
    val fileSettings = dotenv { ignoreIfMissing = true }
    fun setting(name: String): String? = System.getenv(name) ?: fileSettings[name]
    val config = demoConfig(ApplicationConfig("application.conf"), System::getenv) { fileSettings[it] }
    requireDemoTarget(config.property("db.name").getString(), setting("DEMO_DATABASE"))
    val password = requireNotNull(setting("DEMO_PASSWORD")) { "Falta DEMO_PASSWORD; no se imprime ni se almacena en archivos" }
    DatabaseFactory.init(config).use {
        val loaded = DemoSeeder(clock, config.property("terms.version").getString(), config.property("declaration.version").getString())
            .seed(data, password)
        println(if (loaded) "Datos ficticios T-18 cargados." else "La demo T-18 ya existe: no se modificó ni se duplicó ningún registro.")
    }
}
