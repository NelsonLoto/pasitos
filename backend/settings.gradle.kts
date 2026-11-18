pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
rootProject.name = "pasitos-backend"

// Módulo compartido con la app, como build compuesto: sus cambios se ven al recompilar (ADR-01).
includeBuild("../shared")
