# 01 · Stack

Las versiones exactas viven en los archivos de build (`libs.versions.toml` en la app,
`build.gradle.kts` en backend y `shared`). Esta tabla registra las vigentes al 14/10/2026 y las
librerías que se incorporan; las nuevas se fijan en I0 con la última versión estable compatible.

## Común

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.2.10 | Lenguaje de app, backend y `shared`. Debe ser la **misma versión** en los tres builds |
| kotlinx.serialization | 1.8.x | Contratos de la API en `shared` |
| Gradle | 9.5.0 (wrapper) | Build |
| JDK | 21 para compilar; bytecode 17 en `shared` | Backend corre en 21; Android y `shared` apuntan a 17 |

## Módulo compartido (`shared/`)

| Tecnología | Uso |
|---|---|
| Kotlin/JVM puro, sin dependencias de Android ni de Ktor | Para que lo usen la app y el backend (ADR-01) |
| `java.time` | Fechas y edades (disponible en Android desde la API 26, el mínimo de la app) |
| kotlinx.serialization | DTOs de la API |
| kotlin.test + JUnit 5 | Pruebas con casos de control |

## Backend

| Tecnología | Versión | Uso |
|---|---|---|
| Ktor Server (Netty) | 3.2.0 → alinear con la del cliente (3.6.x) en I0 | HTTP, autenticación, sesiones del panel |
| Plugins Ktor | — | `content-negotiation`, `status-pages`, `auth-jwt`, `rate-limit`, `sessions`, `thymeleaf`, `call-logging` |
| Exposed (DSL) | 0.61.0 | Acceso a datos |
| PostgreSQL | 16 | Base de datos |
| HikariCP | 5.1.0 | Pool de conexiones |
| Flyway | 11.8.2 | Migraciones |
| java-jwt (Auth0) | 4.5.0 | Tokens de acceso |
| jwks-rsa (Auth0) | Nueva | Verificación de tokens de Google |
| bcrypt (at.favre) | 0.10.2 | Contraseñas, costo 12 |
| db-scheduler | Nueva | Tareas programadas persistidas en PostgreSQL (ADR-18) |
| Thymeleaf | Nueva | Plantillas del panel, páginas públicas y PDFs (ADR-09, ADR-10) |
| HTMX | Nueva (archivo estático) | Interacción en el panel sin framework de frontend |
| openhtmltopdf + soporte SVG | Nueva | HTML a PDF (ADR-10) |
| Firebase Admin SDK | Nueva | Envío de avisos FCM |
| Jakarta Mail (vía Simple Java Mail) | Nueva | Envío de emails por SMTP (ADR-07) |
| Sentry Java | Nueva | Errores (ADR-08) |
| Logback | 1.5.18 | Logs |
| dotenv-kotlin | 6.5.1 | Variables de entorno en desarrollo local |
| Testcontainers (PostgreSQL) | Nueva | Pruebas de integración |

## App Android

| Tecnología | Versión | Uso |
|---|---|---|
| Android Gradle Plugin | 9.3.2 | Build, con Kotlin integrado de AGP 9 |
| `minSdk` / `compileSdk` / `targetSdk` | 26 / 37 / 37 | Android 8.0 o superior (RNF-COM-01) |
| Jetpack Compose (BOM) | 2026.02.01 | Interfaz, Material 3 |
| Navigation Compose | 2.9.8 | Navegación con rutas tipadas |
| Hilt | 2.59.2 | Inyección de dependencias |
| KSP | 2.2.10-2.0.2 | Procesamiento de anotaciones (Hilt, Room) |
| Ktor Client (OkHttp) | 3.6.0 | Cliente de la API |
| Room | Nueva | Base local (ADR-02) |
| SQLCipher for Android | Nueva | Cifrado de la base local (ADR-17) |
| WorkManager | Nueva | Sincronización en segundo plano |
| DataStore | Nueva | Preferencias y sesión |
| Tink | Nueva | Cifrado de tokens y clave de la base con el Android Keystore |
| Credential Manager + Google ID | Nueva | Inicio con Google (ADR-11) |
| Firebase Cloud Messaging | Nueva | Recepción de avisos |
| Biometric | Nueva | Bloqueo con huella o PIN (RF-CTA-08) |
| Sentry Android | Nueva | Errores |
| JUnit 4, kotlinx-coroutines-test, Turbine, Compose UI Test | Nueva/actual | Pruebas |

## Herramientas

| Herramienta | Uso |
|---|---|
| Docker y Docker Compose | Base local, imagen del backend, despliegue |
| GitHub Actions | Integración continua (ADR-14) |
| Dependabot | Avisos de actualización de dependencias |
| GitHub Issues + Projects | Tareas y tablero (ADR-22) |
| Android Studio | IDE de la app (abrir la carpeta `app/`) |
| IntelliJ IDEA | IDE de backend y `shared` |

## Política de versiones

- Las actualizaciones las propone Dependabot y se aplican en PRs propios, nunca mezcladas con
  funcionalidad.
- Antes de subir Kotlin, verificar la compatibilidad de KSP, Hilt, Room y AGP (lección del
  proyecto: Hilt 2.57 no funcionaba con AGP 9).
- Las versiones mayores (por ejemplo, Exposed 1.x) se evalúan con un PR exploratorio antes de
  adoptarlas.
