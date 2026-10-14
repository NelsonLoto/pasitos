# 02 · Estructura del repositorio

## Carpetas

```
control-nino-sano-app/        ← renombre del repositorio a "pasitos" pendiente (P-02)
├── app/                      ← proyecto Android: se abre esta carpeta en Android Studio
│   ├── settings.gradle.kts   ← includeBuild("../shared")
│   ├── gradle/libs.versions.toml
│   └── app/                  ← módulo :app
├── backend/                  ← proyecto Ktor
│   ├── settings.gradle.kts   ← includeBuild("../shared")
│   ├── Dockerfile
│   └── src/main/resources/db/migration/
├── shared/                   ← biblioteca Kotlin/JVM usada por app y backend
│   ├── settings.gradle.kts
│   └── src/
├── docs/
│   ├── spec/                 ← SPEC funcional
│   ├── spec-tecnica/         ← esta SPEC
│   └── Documento_Funcional_App_Nino_Sano.docx   ← histórico, reemplazado por docs/spec
├── .github/
│   ├── workflows/ci.yml
│   ├── dependabot.yml
│   └── PULL_REQUEST_TEMPLATE.md
├── docker-compose.yml
├── CONTRIBUTING.md
└── README.md
```

## Builds de Gradle

Hay tres builds independientes. `app/` y `backend/` incluyen a `shared/` como **build
compuesto** (ADR-01):

```kotlin
// app/settings.gradle.kts y backend/settings.gradle.kts
includeBuild("../shared")

// app/app/build.gradle.kts y backend/build.gradle.kts
dependencies {
    implementation("com.pasitos:shared")
}
```

Gradle reemplaza la dependencia `com.pasitos:shared` por el código fuente de `shared/`: un cambio
en `shared` se ve en la app y en el backend al recompilar, sin publicar nada.

Reglas:

- `shared` no depende de Android, Ktor, Exposed ni de ninguna librería de plataforma.
- La versión de Kotlin es la misma en los tres builds.
- `shared` compila a bytecode de Java 17. La app sube `compileOptions` de Java 11 a 17 en I0.

## Paquetes

Después del renombre (ADR-12):

| Build | Paquete raíz | Organización |
|---|---|---|
| App | `com.pasitos.app` | `core/` (common, designsystem, navigation, network, database, security, sync) · `data/<dominio>/` (local, remote, model, di) · `feature/<pantalla>/` · `ui/theme/` |
| Backend | `com.pasitos.backend` | Un paquete por dominio con sus rutas, servicio, repositorio y tablas · `common/`, `config/`, `jobs/`, `admin/`, `web/` |
| Shared | `com.pasitos.shared` | `growth/`, `vaccination/`, `time/`, `api/` (DTOs) |

Dominios (mismos nombres en app y backend): `auth`, `account`, `child`, `guardianship`,
`pediatrician`, `carelink`, `measurement`, `growth`, `vaccination`, `consultation`, `event`,
`milestone`, `habit`, `advice`, `report`, `notification`, `audit`, `sync`, `admin`.

## Idioma y estilo

- Clases, funciones, variables, endpoints, JSON, tablas y columnas: **inglés** (ADR-21).
- Textos de la interfaz, mensajes de error para el usuario, documentación y commits: **español**.
- Estilo de código: convención oficial de Kotlin (`kotlin.code.style=official`).
- Base de datos: `snake_case`, nombres de tabla en plural (`measurements`).
- API: rutas en `kebab-case` y plural (`/link-requests`), JSON en `camelCase`.

## Equivalencias de nombres

| SPEC funcional | Código |
|---|---|
| Cuenta | `Account` |
| Tutor · tutor principal · tutor secundario | `Guardian` · `PRINCIPAL` · `SECONDARY` |
| Tutoría (relación tutor–niño) | `Guardianship` |
| Niño | `Child` |
| Pediatra · perfil de pediatra | `Pediatrician` · `PediatricianProfile` |
| Matrícula (jurisdicción + número) | `License` (`jurisdiction`, `number`) |
| Vínculo · solicitud de vínculo | `CareLink` · `CareLink` en estado `REQUESTED` |
| Invitación · reclamar un perfil | `Invitation` · `claim` |
| Paciente propio | *own patient* (`Child` sin `Guardianship`) |
| Medición | `Measurement` |
| Longitud o talla · forma de medición | `lengthCm` · `LengthPosition` (`LYING`, `STANDING`) |
| Perímetro cefálico | `headCircumferenceCm` |
| Percentil · puntaje Z | `percentile` · `zScore` |
| Estado (Dentro de lo esperado / A controlar / Consultar con el pediatra) | `GrowthStatus` (`EXPECTED`, `MONITOR`, `CONSULT`) |
| Cruce de percentiles | `PercentileCrossing` |
| Tabla de referencia · umbral | `ReferenceTable` · `Threshold` |
| Edad corregida · semanas de gestación | `correctedAge` · `gestationalWeeks` |
| Vacuna · dosis de calendario · dosis aplicada | `Vaccine` · `CalendarDose` · `AppliedDose` |
| Otra vacuna · campaña | `OtherVaccine` · `Campaign` |
| Estados de dosis (Próxima, Pendiente, Atrasada, Aplicada, Programada) | `DoseStatus` (`UPCOMING`, `DUE`, `OVERDUE`, `APPLIED`, `SCHEDULED`) |
| Consulta · borrador · cerrada | `Consultation` · `DRAFT` · `CLOSED` |
| Observaciones · indicaciones para la familia · nota aclaratoria | `privateNotes` · `familyInstructions` · `Addendum` |
| Evento · hito · hito logrado | `Event` · `Milestone` · `MilestoneAchievement` |
| Hábito | `Habit` |
| Consejo | `Advice` |
| Notificación | `Notification` |
| Registro de accesos | `AuditLog` |
| Perfil finalizado | `ChildStatus.FINALIZED` |

## Nota sobre OneDrive

El repositorio vive en una carpeta sincronizada por OneDrive, que bloquea archivos de `build/` de
forma intermitente. Se recomienda excluir de la sincronización `app/build`, `app/app/build`,
`backend/build`, `shared/build` y las carpetas `.gradle`.
