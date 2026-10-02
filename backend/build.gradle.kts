plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    application
}

group = "com.pasitos"
version = "0.1.0"

repositories {
    mavenCentral()
}

val ktorVersion = "3.2.0"
val exposedVersion = "0.61.0"
val flywayVersion = "11.8.2"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")

    // Auth: JWT (emisión + validación) + hashing de contraseñas
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")

    // Acceso a datos: Exposed (ORM liviano tipo DSL) + HikariCP (pool de conexiones)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.7")

    // Migraciones de schema (versiona Pasitos_base_de_datos.sql como V1__init.sql)
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Carga backend/.env en desarrollo local (no versionado); en Docker/CI las
    // variables de entorno ya vienen seteadas y esto no hace nada.
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation(kotlin("test"))
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
