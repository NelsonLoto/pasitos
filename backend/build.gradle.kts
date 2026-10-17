plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
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
val flywayVersion = "11.8.2"

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
    implementation("com.auth0:java-jwt:4.6.1")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")

    // Acceso a datos: Exposed (ORM liviano tipo DSL) + HikariCP (pool de conexiones)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.13")

    // Migraciones del esquema, versionadas por fecha y hora (ADR-23)
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.6.5")

    // Carga backend/.env en desarrollo local (no versionado); en Docker/CI las
    // variables de entorno ya vienen seteadas y esto no hace nada.
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    testImplementation(kotlin("test"))
    // Gradle 9 ya no agrega solo el launcher de JUnit Platform.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Pruebas de integración contra PostgreSQL 16 real en Docker (ADR-19)
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
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
