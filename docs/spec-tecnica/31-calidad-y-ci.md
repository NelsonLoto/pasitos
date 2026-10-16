# 31 · Calidad e integración continua

## 1. Estrategia de pruebas (ADR-19)

| Nivel | Qué se prueba | Herramientas | Dónde corre |
|---|---|---|---|
| 1. Motor de crecimiento y vacunas | Casos de control OMS y SAP, bordes, propiedades ([10](10-modulo-compartido.md#6-pruebas)) | kotlin.test, JUnit 5 | CI en cada PR |
| 2. Backend | Un test por criterio de aceptación de cada RF con API; **matriz de permisos** (cada rol contra cada acción sobre datos de un niño); sincronización (idempotencia, lápidas, revocaciones) | Ktor `testApplication`, Testcontainers con PostgreSQL 16 real | CI en cada PR |
| 3. App Android | ViewModels con repositorios falsos; DAOs de Room; flujos críticos de interfaz: alta de hijo, carga de medición, consulta guiada | JUnit 4, kotlinx-coroutines-test, Turbine, Room in-memory con Robolectric, Compose UI Test | Unitarias en CI; de interfaz y las que usan el Android Keystore (cifrado de la base) en emulador local antes de cerrar el incremento |
| 4. Aceptación manual | Lista de verificación por incremento generada desde los criterios de la SPEC; se ejecuta en un dispositivo real | Checklist en el issue del incremento | Antes de dar por cerrado cada incremento |

### Nombres de pruebas trazables

Cada prueba nombra el criterio que verifica, para poder buscar la cobertura de un requerimiento:

```kotlin
@Test
fun `RF-CRE-01 CA2 rechaza una fecha anterior al nacimiento`() { … }
```

## 2. Integración continua (ADR-14)

`.github/workflows/ci.yml`, en cada PR contra `develop` y en cada push a `develop`:

| Job | Se ejecuta si cambia | Pasos |
|---|---|---|
| `shared` | `shared/**` | `./gradlew test` |
| `backend` | `backend/**` o `shared/**` | `./gradlew test` (con Testcontainers) y validación de `openapi.yaml` |
| `android` | `app/**` o `shared/**` | `./gradlew assembleDebug testDebugUnitTest lint` y verificación de que no se use `LocalDate.now()` |
| `docs` | `docs/**` | Verificación de links internos entre archivos Markdown |

- Gradle con caché (`gradle/actions/setup-gradle`) para que cada ejecución tarde lo menos posible.
  El job `android` también guarda en caché las imágenes de Android que descarga Robolectric
  (`~/.m2/repository/org/robolectric`).
- **`LocalDate.now()` prohibido en la app (ADR-26):** el job `android` falla si aparece en
  `app/app/src/main` fuera de `core/time`. "Hoy" se calcula con `ArgentinaClock.today()`. Los
  archivos anteriores a I0-07 que todavía lo usan quedan en una lista de excepciones que solo
  puede achicarse: cada tarea de I1 que reescribe uno lo saca de la lista. Son diez:
  - con lógica: `PsDatePickerField.kt`, `Child.kt`, `DoseStatusResolver.kt`,
    `ChildOnboardingViewModel.kt`, `MeasurementFormUiState.kt` y `VaccineDetailViewModel.kt`;
  - vistas previas de diseño, que conviene pasar a fechas fijas: `ChildSelectorScreen.kt`,
    `GrowthChartScreen.kt`, `HomeScreen.kt` y `VaccineDetailScreen.kt`.
- Un PR que solo toca documentación no compila la app ni el backend.
- Duración estimada: entre 8 y 12 minutos cuando corren todos los jobs. El plan gratuito incluye
  2.000 minutos por mes.

**Regla del equipo:** no se mergea un PR con la integración continua en rojo. GitHub Free no
permite bloquearlo técnicamente en repositorios privados, así que depende de la disciplina del
equipo, igual que la regla de no pushear directo a `develop`.

### Dependabot

`.github/dependabot.yml`: revisión semanal de dependencias de Gradle (app, backend y `shared`) y de
las acciones de GitHub, con PRs agrupados por ecosistema.

## 3. Definición de terminado

Una tarea está terminada cuando:

- [ ] Cumple todos los criterios de aceptación de los RF que referencia su issue.
- [ ] Cada criterio tiene al menos una prueba automática, o un paso en la lista manual del
      incremento si no es automatizable.
- [ ] La integración continua está en verde.
- [ ] La app se ejecutó en emulador o dispositivo, si la tarea toca la app.
- [ ] Si cambió un comportamiento, la SPEC funcional o técnica se actualizó en el mismo PR o en
      uno previo.
- [ ] No hay datos personales en logs ni secretos en el código.
- [ ] Al menos un integrante revisó y aprobó el PR.

## 4. Revisión de código

- Todo PR tiene al menos una aprobación de otro integrante antes del merge (ya vigente en
  `CONTRIBUTING.md`).
- PRs chicos: una tarea por PR. Si un PR supera las 400 líneas cambiadas (sin contar archivos
  generados ni datos), conviene partirlo.
- El revisor verifica: que la tarea cumpla su issue y sus RF, las pruebas, los permisos (¿quién
  puede llamar a esto?) y que no se filtren datos personales.
