import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Biblioteca Kotlin/JVM pura que usan la app y el backend como build compuesto (ADR-01). No
// depende de Android, Ktor, Exposed ni de ninguna librería de plataforma.
plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    `java-library`
}

group = "com.pasitos"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Bytecode de Java 17: la app compila con Java 17 y el backend con 21.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // Solo permite la API de Java 17 aunque Gradle corra con un JDK más nuevo.
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

dependencies {
    // Misma versión que usa Ktor 3.6 en la app y el backend.
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    // JsonElement: los DTOs de sincronización (14 · Sincronización) representan filas de forma
    // variable según la entidad -el motor genérico no conoce su forma-.
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation(kotlin("test"))
    // Gradle 9 ya no agrega solo el launcher de JUnit Platform.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
