# 31 · Calidad e integración continua

## 1. Estrategia de pruebas (ADR-19)

| Nivel | Qué se prueba | Herramientas | Dónde corre |
|---|---|---|---|
| 1. Motor de crecimiento y vacunas | Casos de control OMS y SAP, bordes, propiedades ([10](10-modulo-compartido.md#6-pruebas)) | kotlin.test, JUnit 5 | CI en cada PR |
| 2. Backend | Un test por criterio de aceptación de cada RF con API; **matriz de permisos** (cada rol contra cada acción sobre datos de un niño); sincronización (idempotencia, lápidas, revocaciones); emails (verificados contra el contenido real recibido) | Ktor `testApplication`, Testcontainers con PostgreSQL 16 y Mailpit reales | CI en cada PR |
| 3. App Android | ViewModels con repositorios falsos; DAOs de Room; flujos críticos de interfaz: alta de hijo, carga de medición, consulta guiada | JUnit 4, kotlinx-coroutines-test, Turbine, Room in-memory con Robolectric, Compose UI Test | Unitarias en CI; de interfaz y las que usan el Android Keystore (cifrado de la base) en emulador local antes de cerrar el incremento |
| 4. Aceptación manual | Lista de verificación por incremento generada desde los criterios de la SPEC; se ejecuta en un dispositivo real | Checklist en el issue del incremento | Antes de dar por cerrado cada incremento |

### Nombres de pruebas trazables

Cada prueba nombra el criterio que verifica, para poder buscar la cobertura de un requerimiento:

```kotlin
@Test
fun `RF-CRE-01 CA2 rechaza una fecha anterior al nacimiento`() { … }
```

## 2. Integración continua (ADR-14)

`.github/workflows/ci.yml`, en cada PR (contra cualquier rama, así también el de release a `main`
y los PRs encadenados) y en cada push a `develop`:

| Job | Se ejecuta si cambia | Pasos |
|---|---|---|
| `shared` | `shared/**` | Verificación del checksum del wrapper y `./gradlew test` |
| `backend` | `backend/**` o `shared/**` | Verificación del checksum del wrapper y `./gradlew test` (con Testcontainers). La validación de `openapi.yaml` se agrega con el archivo ([P-14](92-pendientes.md)) |
| `android` | `app/**`, `shared/**` o `.github/scripts/**` | Verificación del checksum del wrapper, de que no se use la zona horaria del teléfono y `./gradlew assembleDebug testDebugUnitTest lintDebug` |
| `docker` | `backend/**`, `shared/**`, `docker-compose.yml` o `.dockerignore` | `docker compose --profile full up` construye la imagen del backend y levanta base, Mailpit y backend; verifica que `/health` responda y que la imagen no tenga archivos `.env` (en ningún lugar) ni claves (`*.pem`, `*.jks`, `*.keystore`) en la aplicación ni dentro de los jars del backend y de `shared`, con un `backend/.env` de prueba en el contexto. Las claves no se buscan en el resto del sistema porque la imagen base trae certificados públicos `.pem` ([30](30-infraestructura.md#imagen-del-backend)) |
| `docs` | Cualquier `.md` del repositorio | Links entre archivos Markdown y sus anclas (`#seccion`), sin consultar internet (lychee) |

- Un cambio en `ci.yml` corre todos los jobs, para probar el cambio. Un PR que solo toca
  documentación no compila la app ni el backend.
- Si un job falla, se guardan sus informes de pruebas (y de lint en `android`) como artefacto
  durante 7 días.
- Gradle con caché (`gradle/actions/setup-gradle`) para que cada ejecución tarde lo menos posible.
  La caché se escribe en `develop` y los PRs la leen. El job `android` también guarda en caché las
  imágenes de Android que descarga Robolectric (`~/.m2/repository/org/robolectric`).
- **Seguridad del workflow:** permisos de solo lectura y acciones fijadas por SHA del commit, con
  la versión en un comentario, para que una etiqueta movida no cambie el código que corre.
  Dependabot las actualiza. La única excepción de permisos es `dependencias.yml`, un workflow
  aparte que no corre en PRs ([Alertas y actualizaciones de
  seguridad](#alertas-y-actualizaciones-de-seguridad)).
- **Zona horaria del teléfono prohibida en la app (ADR-26):**
  `.github/scripts/check-local-date-now.sh` falla si en `app/app/src/main`, fuera de `core/time`,
  aparece `LocalDate.now()`, `LocalDateTime.now()`, `LocalTime.now()`, `ZonedDateTime.now()`,
  `OffsetDateTime.now()`, `ZoneId.systemDefault()` o `TimeZone.getDefault()`. "Hoy" se calcula
  con `ArgentinaClock.today()`; `LocalDate.now(clock)` con un reloj explícito se permite. Los
  archivos anteriores a I0-07 que todavía usan `LocalDate.now()` están en
  `.github/scripts/local-date-now-allowlist.txt`, una lista que solo puede achicarse: el script
  también falla si un archivo de la lista ya no lo usa, así la tarea de I1 que lo reescribe tiene
  que sacarlo. Quedan cuatro:
  - con lógica: `PsDatePickerField.kt` y `Child.kt`;
  - vistas previas de diseño, que conviene pasar a fechas fijas: `GrowthChartScreen.kt` y
    `HomeScreen.kt`.
- **Distribución de Gradle verificada:** `.github/scripts/check-gradle-wrapper.sh` falla en los
  jobs `shared`, `backend` y `android` si su `gradle-wrapper.properties` no tiene
  `distributionSha256Sum`. Sin esa línea, el wrapper baja el ZIP de Gradle sin comprobar que sea
  el oficial, y nada más lo detecta: `setup-gradle` valida solo el `gradle-wrapper.jar`. Un
  checksum equivocado no necesita control, porque Gradle rechaza el ZIP. Android Studio borra la
  línea cuando regenera el wrapper, por ejemplo al proponer bajar la versión de AGP o de Gradle
  (#128).
- Los scripts de shell y los `gradlew` se guardan con fin de línea LF (`.gitattributes`), para
  que funcionen igual en Linux y en Git Bash de Windows.
- Duración estimada: entre 8 y 12 minutos cuando corren todos los jobs. El plan gratuito incluye
  2.000 minutos por mes.

**Regla del equipo:** no se mergea un PR con la integración continua en rojo. GitHub Free no
permite bloquearlo técnicamente en repositorios privados, así que depende de la disciplina del
equipo, igual que la regla de no pushear directo a `develop`.

### Dependabot

`.github/dependabot.yml`: revisión semanal (lunes 6:00 de Argentina) de dependencias de Gradle, de
las imágenes base del backend y de las acciones de GitHub, con PRs agrupados por ecosistema y el
prefijo `chore:`.

- Los tres builds de Gradle (app, backend y `shared`) se actualizan en los mismos PRs, porque
  Kotlin y kotlinx.serialization tienen que tener la misma versión en los tres
  ([01](01-stack.md)).
- Kotlin, KSP y Hilt van en un PR propio: cada versión de Kotlin necesita una de KSP y una de
  Hilt que la soporten, así que suben juntas. Antes de mergearlo hay que verificar también Room y
  AGP, y si se traba no frena al resto.
- Para que Dependabot reconozca a Kotlin en el backend y en `shared`, el plugin se declara con su
  id completo (`id("org.jetbrains.kotlin.jvm")`), no con `kotlin("jvm")`: con la forma corta
  queda fuera del grupo y llega en otro PR, con una versión distinta de la de la app.
- **El Kotlin del backend se sube a mano en el PR de Kotlin.** Dependabot actualiza cada
  dependencia una sola vez por grupo. Al revisar `app/` también actualiza `shared/`, porque la app
  lo incluye como build compuesto, y en `backend/` la saltea porque ya la dio por resuelta. Antes
  de mergear ese PR se le suma un commit que lleva `backend/build.gradle.kts` a la misma versión.
  Si se olvida, el job `backend` falla, porque no puede leer `shared` compilado con un Kotlin más
  nuevo. Tener la versión en un solo archivo, para no depender de este paso, queda como
  [P-16](92-pendientes.md).
- Las demás versiones menores y parches van agrupados en otro PR. Cada versión mayor llega en su
  propio PR, porque se evalúa aparte. Las versiones mayores de Exposed se ignoran hasta evaluarlas
  ([P-15](92-pendientes.md)).
- Las imágenes base de `backend/Dockerfile` van fijadas por digest, así que dependen de Dependabot
  para recibir los parches de seguridad del sistema y del JRE. Se ignora el cambio de versión
  mayor de Java, que es la 21 en todo el proyecto ([01](01-stack.md)). Las imágenes de
  `docker-compose.yml` (PostgreSQL y Mailpit) son solo para desarrollo local y se actualizan a
  mano.
- Una versión se propone recién 14 días después de publicada (`cooldown`), para dar tiempo a que
  se detecte una versión maliciosa o rota. La espera también vale para los digest de las imágenes
  base. Las únicas que no esperan son las actualizaciones de seguridad (abajo).

#### Alertas y actualizaciones de seguridad

Desde el 18/10/2026, el repositorio tiene activadas en *Settings → Advanced Security* las alertas
de Dependabot y sus actualizaciones de seguridad.

- GitHub no lee los `build.gradle.kts`: hay que enviarle las dependencias de Gradle. Lo hace
  `.github/workflows/dependencias.yml`, con un job por build (`app`, `backend` y `shared`) y
  `gradle/actions/dependency-submission`. Las acciones de GitHub las lee solo de los workflows.
- El workflow corre en cada push a `develop` que cambie dependencias (`*.gradle.kts`,
  `libs.versions.toml` o el wrapper de Gradle), y también a mano. No hace falta más: GitHub
  vuelve a evaluar el grafo ya enviado cada vez que se publica una vulnerabilidad.
- **Solo se envía lo que se ejecuta en producción:** `runtimeClasspath` del backend y de `shared`,
  y `debugRuntimeClasspath` y `releaseRuntimeClasspath` de la app. Quedan fuera las herramientas
  de compilación (`classpath` con los plugins de Gradle como AGP, `androidLintTool`, KSP y el
  compilador de Kotlin) y las pruebas. Así cada alerta es algo que llega al teléfono o al
  servidor; las vulnerabilidades de esas herramientas se corrigen cuando Dependabot sube su
  versión. Si AGP renombrara esas configuraciones, sus dependencias desaparecerían del grafo sin
  aviso: después de cada cambio de AGP conviene revisar que el grafo siga teniendo, por ejemplo,
  Room y Ktor.
- Es el único workflow con permiso de escritura (`contents: write`), que pide la API de envío. No
  corre en PRs, y valida que el `gradle-wrapper.jar` sea el oficial antes de ejecutarlo.
- El envío automático de GitHub (*automatic dependency submission*) no sirve en este repositorio:
  busca un build de Gradle en la raíz, y acá hay tres en subcarpetas.
- Si una dependencia tiene una vulnerabilidad con aviso publicado en la base de GitHub (*GitHub
  Advisory Database*), aparece una alerta y Dependabot abre un PR de seguridad sin esperar los 14
  días.
- Una corrección sin aviso en GitHub, por ejemplo una que solo figura en el registro de cambios de
  la librería, no genera alerta: llega con la revisión semanal, después de la espera. Si es
  urgente, se sube a mano en un PR propio.
- Las imágenes de Docker no entran en el grafo de dependencias: sus parches llegan solo con la
  revisión semanal.

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
