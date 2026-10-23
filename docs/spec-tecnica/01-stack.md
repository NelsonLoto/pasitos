# 01 · Stack

Las versiones exactas viven en los archivos de build (`libs.versions.toml` en la app,
`build.gradle.kts` en backend y `shared`), y Dependabot las cambia todas las semanas
([31](31-calidad-y-ci.md#dependabot)). Esta tabla es una foto de las vigentes al **18/10/2026**: se
actualiza al cierre de cada incremento ([32](32-proceso-de-desarrollo.md#6-demos-quincenales)), y
entre un cierre y otro mandan los archivos de build. Las librerías marcadas como "Nueva" se
incorporan en el incremento que las necesita, con la última versión estable compatible.

## Común

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.4.20 | Lenguaje de app, backend y `shared`. Debe ser la **misma versión** en los tres builds: el PR de Kotlin de Dependabot no actualiza el backend, que se sube a mano ([31](31-calidad-y-ci.md#dependabot)) |
| kotlinx.serialization | 1.11.0 | Contratos de la API en `shared`. Es la versión que trae Ktor 3.6 y se fija igual en los tres builds |
| Gradle | 9.8.0 (wrapper) | Build |
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
| Ktor Server (Netty) | 3.6.0, la misma que el cliente | HTTP, autenticación, sesiones del panel |
| Plugins Ktor | — | `content-negotiation`, `status-pages`, `auth-jwt`, `rate-limit`, `sessions`, `thymeleaf`, `call-logging` |
| Exposed (DSL) | 0.61.0 | Acceso a datos |
| PostgreSQL | 16 | Base de datos |
| HikariCP | 7.1.0 | Pool de conexiones |
| Flyway | 13.9.0 | Migraciones |
| java-jwt (Auth0) | 4.6.2 | Tokens de acceso |
| jwks-rsa (Auth0) | 0.24.1 | Claves públicas de Google para verificar sus ID tokens (I1-B2) |
| bcrypt (at.favre) | 0.10.2 | Contraseñas, costo 12 |
| db-scheduler | Nueva | Tareas programadas persistidas en PostgreSQL (ADR-18) |
| Thymeleaf (vía `ktor-server-thymeleaf`) | 3.6.0 | Plantillas del panel, páginas públicas y PDFs (ADR-09, ADR-10) |
| HTMX | Nueva (archivo estático) | Interacción en el panel sin framework de frontend |
| openhtmltopdf + soporte SVG | Nueva | HTML a PDF (ADR-10) |
| Firebase Admin SDK | Nueva | Envío de avisos FCM |
| Jakarta Mail (vía Simple Java Mail) | 8.12.6 | Envío de emails por SMTP (ADR-07) |
| Sentry Java | Nueva | Errores (ADR-08) |
| Logback | 1.6.5 | Logs |
| dotenv-kotlin | 6.5.1 | Variables de entorno en desarrollo local |
| Testcontainers (PostgreSQL, genérico) | 2.0.5 | Pruebas de integración contra PostgreSQL y Mailpit reales; requiere Docker |

## App Android

| Tecnología | Versión | Uso |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | Build, con Kotlin integrado de AGP 9 |
| `minSdk` / `compileSdk` / `targetSdk` | 26 / 37 / 37 | Android 8.0 o superior (RNF-COM-01) |
| Jetpack Compose (BOM) | 2026.09.00 | Interfaz, Material 3 |
| Navigation Compose | 2.10.2 | Navegación con rutas tipadas |
| Hilt | 2.60.1 | Inyección de dependencias |
| KSP | 2.3.12 | Procesamiento de anotaciones (Hilt, Room). Desde la 2.3, su versión ya no sigue a la de Kotlin |
| Ktor Client (OkHttp) | 3.6.0 | Cliente de la API, con el plugin `Auth` para el token de acceso y su renovación |
| Room | 2.8.5 | Base local (ADR-02) |
| SQLCipher for Android | 4.19.1 | Cifrado de la base local (ADR-17) |
| WorkManager (con Hilt Work 1.4.0) | 2.12.0 | Sincronización en segundo plano |
| DataStore | 1.2.1 | Preferencias y sesión |
| Tink | 1.23.0 | Cifrado de tokens y clave de la base con el Android Keystore |
| Credential Manager + Google ID | Nueva | Inicio con Google (ADR-11) |
| Firebase Cloud Messaging | Nueva | Recepción de avisos |
| Biometric | Nueva | Bloqueo con huella o PIN (RF-CTA-08) |
| Sentry Android | Nueva | Errores |
| JUnit 4, kotlinx-coroutines-test, Turbine, Compose UI Test | Nueva/actual | Pruebas |
| Ktor Client Mock | 3.6.0 | Backend falso para probar el cliente de la API y la renovación de tokens |
| Robolectric | 4.17 | Pruebas de Room y WorkManager en la JVM, sin emulador |

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
- Al cambiar el `targetSdk` de la app, actualizar `sdk` en
  `app/app/src/test/resources/robolectric.properties`, para que las pruebas simulen la misma
  versión de Android.
