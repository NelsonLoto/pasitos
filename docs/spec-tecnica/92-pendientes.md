# 92 · Pendientes técnicos

## Decisiones y tareas abiertas

| ID | Pendiente | Responsable | Fecha límite | Referencia |
|---|---|---|---|---|
| P-01 | Elegir el proveedor de hosting del piloto según los requisitos de [30](30-infraestructura.md#requisitos-para-elegir-el-proveedor-p-01) | Equipo | Antes de I6 | ADR-05 |
| P-02 | ✅ **Resuelto 14/10/2026.** Repositorio renombrado a `NelsonLoto/pasitos`. GitHub redirige las URLs viejas; igual, cada integrante actualiza su remoto: `git remote set-url origin https://github.com/NelsonLoto/pasitos.git` | — | — | ADR-12 |
| P-03 | Registrar un dominio (por ejemplo, `.ar` en NIC Argentina o `.app`), necesario para HTTPS del piloto y para el email con SPF y DKIM | Equipo | Antes de I6 | ADR-07 |
| P-04 | Crear la cuenta del proveedor de email en región UE. En desarrollo alcanza con Mailpit | Equipo | Antes de I6 | ADR-07 |
| P-05 | Crear el proyecto de Firebase con una cuenta de Google del equipo (no personal) y registrar `com.pasitos.app` | Equipo | I1 (Google Sign-In) | ADR-06, ADR-13 |
| P-06 | Cuenta de desarrollador de Google Play | Nelson | Antes del piloto | ADR-13 |
| P-07 | Crear la organización de Sentry en región UE. Se postergó de I0 a antes del piloto el 19/10/2026, junto con I0-10: hasta el piloto Sentry casi no aporta (T-09, #63) | Equipo | Antes de I6 | ADR-08 |
| P-08 | Definir dónde se guarda la clave de subida de la app (gestor de contraseñas compartido) | Equipo | Antes de la primera subida a Play | [30](30-infraestructura.md#publicación-de-la-app) |
| P-09 | Descargar las tablas expandidas de la OMS (L, M, S por día y por longitud o talla) y convertirlas al formato de importación | Equipo | I2 | [10](10-modulo-compartido.md#datos-de-referencia) |
| P-10 | Confirmar si el libro de la SAP publica L, M y S o solo percentiles (define si el puntaje Z de 5 a 15 años es exacto o aproximado) | Equipo con pediatras | I2 | PVM-14 |
| P-11 | ✅ **Resuelto 14/10/2026.** La nota para agentes de IA y las verificaciones previas a tomar una tarea están en [`CONTRIBUTING.md`](../../CONTRIBUTING.md#antes-de-tomar-una-tarea-personas-y-agentes-de-ia) | — | — | ADR-22 |
| P-12 | ✅ **Resuelto 16/10/2026.** Ktor Server pasó de 3.2.0 a 3.6.0, la misma versión que el cliente de la app (I0-02) | — | — | [01](01-stack.md) |
| P-13 | Límite general de 120 requests por minuto por cuenta (RNF-SEG-07). I0-05 implementó solo los de `/auth`; falta asignarlo a una tarea, por ejemplo con el primer dominio de I1 | Equipo | I1 | [20](20-seguridad.md) |
| P-14 | Crear `openapi.yaml` y su validación en la integración continua: ninguna tarea del plan lo hace, y `/api/v1/auth` y `/me` (I0-05) ya están sin documentar. Sugerencia: que lo haga I1-B1 (#39), la primera tarea de backend de I1, con todo `/api/v1/auth`; desde ahí, cada tarea de backend actualiza el archivo con sus endpoints | Equipo | I1 | [11](11-backend.md#9-documentación-de-la-api), [31](31-calidad-y-ci.md) |
| P-15 | Evaluar Exposed 1.x con un PR exploratorio: cambia los nombres de los paquetes (`org.jetbrains.exposed.sql` ya no existe), así que hay que migrar todas las tablas y los repositorios del backend. Mientras tanto, `.github/dependabot.yml` ignora sus versiones mayores; al evaluarlo, se borra esa regla | Equipo | Antes del piloto | [01](01-stack.md#política-de-versiones), [31](31-calidad-y-ci.md#dependabot) |
| P-16 | Tener la versión de Kotlin de los tres builds en un solo archivo, por ejemplo un catálogo de versiones en `shared/` que también lea el backend. Así el PR de Kotlin de Dependabot no necesitaría el commit a mano en `backend/build.gradle.kts`. Antes de adoptarlo, hay que verificar que Dependabot procese bien ese archivo | Equipo | Sin fecha: si el paso a mano resulta incómodo | [31](31-calidad-y-ci.md#dependabot) |
| P-17 | Al eliminarse el perfil de un niño, la fila de `children` queda con borrado lógico y conserva nombre, apellido y fecha de nacimiento, aunque sus vacunas ya se borran físicamente (I1-B7). Definir cuándo se borra o se anonimiza esa fila, por ejemplo en la limpieza de I6-B3, una vez que ningún teléfono necesite la lápida | Equipo | I6 | RN-HIJ-08, RNF-PRI-04, [13](13-base-de-datos.md#vacunación) |

## Supuestos técnicos

Detalles que esta SPEC define sin haberlos tratado en el relevamiento. Si no se objetan en la
revisión, quedan confirmados al mergear:

| Supuesto | Dónde |
|---|---|
| Límite de 120 requests por minuto por cuenta y 10 por minuto por IP en `/auth/login` | [20](20-seguridad.md) |
| `/auth/register` también limitado a 10 por minuto por IP; `/auth/refresh` sin límite propio, porque exige un token de renovación imposible de adivinar | [20](20-seguridad.md) |
| Hasta 50 pacientes con detalle guardado en el teléfono del pediatra | [14](14-sincronizacion.md#4-alcance-por-rol-adr-16) |
| Páginas de hasta 500 filas en la sincronización | [14](14-sincronizacion.md#2-traer-cambios) |
| Copias de seguridad: 14 diarias y 8 semanales | [30](30-infraestructura.md#copias-de-seguridad) |
| PRs de hasta unas 400 líneas como referencia | [31](31-calidad-y-ci.md#4-revisión-de-código) |
| Sin entorno de producción para la tesis: el piloto cumple ese rol | [30](30-infraestructura.md#entornos) |
