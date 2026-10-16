plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "backend"

// Módulo compartido con la app, como build compuesto (ADR-01).
includeBuild("../shared")
