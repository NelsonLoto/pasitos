import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kotlin con id completo para que Dependabot lo agrupe con el de la app (31 · Calidad e integración continua).
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21"
    `java-library`
}

// La app y el backend lo usan como build compuesto con implementation("com.pasitos:shared") (ADR-01).
group = "com.pasitos"
version = "0.1.0"
repositories { mavenCentral() }

// Compila con el JDK 21 y genera bytecode 17, que es lo que admite la app (02 · Estructura del repositorio).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

dependencies {
    // api: los tipos @Serializable de shared/api son parte de su interfaz pública.
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }
