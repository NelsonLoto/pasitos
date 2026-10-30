plugins {
    // Con el id completo y no con `kotlin("jvm")`: así Dependabot reconoce que es Kotlin y lo sube
    // en el mismo PR que la app y shared (31 · Calidad e integración continua)
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
    application
}

group = "com.pasitos"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Misma versión que el cliente de la app (P-12).
val ktorVersion = "3.6.0"
val exposedVersion = "0.61.0"
val flywayVersion = "13.9.0"

dependencies {
    // Motor de crecimiento, formato de edades y contratos de la API compartidos con la app
    implementation("com.pasitos:shared")

    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-rate-limit-jvm:$ktorVersion")

    // Auth: JWT (emisión + validación) + hashing de contraseñas
    implementation("com.auth0:java-jwt:4.6.2")
    // Claves públicas de Google para verificar sus ID tokens (I1-B2). También la trae ktor-server-auth-jwt.
    implementation("com.auth0:jwks-rsa:0.24.1")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")

    // Acceso a datos: Exposed (ORM liviano tipo DSL) + HikariCP (pool de conexiones)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")

    // Migraciones del esquema, versionadas por fecha y hora (ADR-23)
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Logging, con una línea por pedido y su identificador (11 · Backend §8)
    implementation("ch.qos.logback:logback-classic:1.6.5")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")

    // Carga backend/.env en desarrollo local (no versionado); en Docker/CI las
    // variables de entorno ya vienen seteadas y esto no hace nada.
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    // Plantillas HTML/texto de emails y páginas públicas (ADR-09, ADR-27)
    implementation("io.ktor:ktor-server-thymeleaf-jvm:$ktorVersion")

    // Envío de emails por SMTP (ADR-07)
    implementation("org.simplejavamail:simple-java-mail:8.12.6")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    testImplementation(kotlin("test"))
    // Gradle 9 ya no agrega solo el launcher de JUnit Platform.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Pruebas de integración contra PostgreSQL 16 real en Docker (ADR-19)
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    // Servidor SMTP de prueba real para verificar los emails enviados (17 · Notificaciones y emails)
    testImplementation("org.testcontainers:testcontainers:2.0.5")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.pasitos.backend.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

// Carga los datos de referencia de backend/seed/ en la base local (13 · Base de datos, Datos
// iniciales). Usa la misma configuración que el backend: backend/.env o las variables de entorno.
tasks.register<JavaExec>("seedReferenceData") {
    group = "application"
    description = "Carga el Calendario Nacional de backend/seed/ en la base configurada"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.pasitos.backend.seed.SeedReferenceDataKt")
    workingDir = projectDir
}

// T-18: carga deliberada de cuentas y niños ficticios; no se ejecuta al iniciar el servidor.
tasks.register<JavaExec>("seedDemoData") {
    group = "application"
    description = "Carga los datos ficticios de backend/seed/demo/ (requiere confirmar la base de destino)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.pasitos.backend.seed.SeedDemoDataKt")
    workingDir = projectDir
}
