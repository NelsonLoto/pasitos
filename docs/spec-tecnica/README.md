# Pasitos — Especificación técnica

| | |
|---|---|
| **Versión** | 1.2.9 |
| **Fecha** | 17/10/2026 |
| **Estado** | Borrador para revisión del equipo |
| **Implementa** | [SPEC funcional v1.4.0](../spec/README.md) |

## Propósito

Define **cómo** se construye lo que la [SPEC funcional](../spec/README.md) define como **qué**:
arquitectura, tecnologías, modelo de datos, API, sincronización, seguridad, infraestructura,
pruebas y proceso de trabajo. Es la referencia para el plan de implementación y para cada PR.

Cuando esta SPEC y la funcional parecen contradecirse, manda la funcional: se corrige esta.

## Estructura

| Archivo | Contenido |
|---|---|
| [00 · Arquitectura](00-arquitectura.md) | Vista general, componentes, principios técnicos y fundaciones (I0) |
| [01 · Stack](01-stack.md) | Tecnologías y versiones por componente |
| [02 · Estructura del repositorio](02-estructura-del-repo.md) | Carpetas, builds de Gradle, nombres e idioma del código |
| [10 · Módulo compartido](10-modulo-compartido.md) | Motor de crecimiento, estados de vacunas y contratos de la API |
| [11 · Backend](11-backend.md) | Capas, autenticación, autorización, tareas programadas, configuración |
| [12 · API](12-api.md) | Convenciones y catálogo de endpoints |
| [13 · Base de datos](13-base-de-datos.md) | Esquema físico, convenciones y migraciones |
| [14 · Sincronización](14-sincronizacion.md) | Protocolo entre la app y el backend, funcionamiento sin conexión |
| [15 · App Android](15-android.md) | Capas, base local cifrada, sesión, modos, notificaciones |
| [16 · Panel de administración](16-panel-admin.md) | Panel web y páginas públicas del backend |
| [17 · Notificaciones y emails](17-notificaciones-y-emails.md) | Push sin datos, generación, emails transaccionales |
| [18 · Informes PDF](18-informes-pdf.md) | Generación de informes en el servidor |
| [20 · Seguridad y privacidad](20-seguridad.md) | Medidas técnicas para cada RNF de seguridad y privacidad |
| [30 · Infraestructura](30-infraestructura.md) | Entornos, despliegue, copias de seguridad, publicación de la app |
| [31 · Calidad e integración continua](31-calidad-y-ci.md) | Estrategia de pruebas, CI y definición de terminado |
| [32 · Proceso de desarrollo](32-proceso-de-desarrollo.md) | Flujo SDD, tareas, trabajo en paralelo y agentes de IA |
| [40 · Trazabilidad](40-trazabilidad.md) | Qué componentes implementan cada módulo funcional |
| [50 · Decisiones de arquitectura](50-adr.md) | Registro de decisiones (ADR) |
| [92 · Pendientes técnicos](92-pendientes.md) | Decisiones abiertas y tareas previas |

## Convenciones

- Los identificadores de la SPEC funcional (`RF-CRE-05`, `RN-PED-04`, `RNF-SEG-06`) se citan tal
  cual para trazar cada decisión técnica a su requerimiento.
- Las decisiones de arquitectura se identifican como `ADR-NN` y están en
  [50 · Decisiones de arquitectura](50-adr.md).
- Los nombres en el código, la API y la base de datos están en **inglés**; los textos de la app y
  la documentación, en español (ADR-21). La tabla de equivalencias está en
  [02 · Estructura del repositorio](02-estructura-del-repo.md#equivalencias-de-nombres).
- Los ejemplos de código son ilustrativos: fijan contratos y responsabilidades, no la
  implementación exacta.

## Cómo se modifica

Igual que la SPEC funcional: rama, PR, versión semántica y fila en el registro de cambios. Un
cambio técnico que altere comportamiento visible requiere primero el cambio en la SPEC funcional.

## Registro de cambios

| Versión | Fecha | Cambio | Motivo |
|---|---|---|---|
| 1.2.9 | 17/10/2026 | Imagen del backend: imágenes base (JDK 21 Ubuntu para compilar, JRE 21 Alpine para ejecutar) fijadas por digest, usuario `pasitos` sin permiso para modificar la aplicación, `.dockerignore` que solo deja entrar `backend/` y `shared/` y excluye los mismos secretos que `.gitignore`, límite de memoria para el contenedor del backend en el piloto; entorno local con Mailpit, el backend en el perfil `full` reconstruido en cada `up`, puertos solo en `127.0.0.1` y la base en el 5433 por defecto ([30](30-infraestructura.md)); job `docker` en la integración continua, que busca `.env` y claves también dentro de los jars, y Dependabot para las imágenes base ([31](31-calidad-y-ci.md)); `.dockerignore` en la estructura ([02](02-estructura-del-repo.md)) | Decisiones tomadas al implementar la imagen Docker y el entorno local, revisadas una por una con el equipo (#37) |
| 1.2.8 | 17/10/2026 | Dependabot: Hilt viaja en el PR de Kotlin y KSP, el plugin de Kotlin se declara con su id completo en el backend y en `shared`, y las versiones mayores de Exposed se ignoran hasta evaluarlas ([31](31-calidad-y-ci.md#dependabot)); P-15: evaluar Exposed 1.x ([92](92-pendientes.md)) | La primera corrida de Dependabot partió Kotlin en dos PRs que no compilaban (#90) |
| 1.2.7 | 16/10/2026 | Integración continua: corre en PRs contra cualquier rama y en push a `develop`, un cambio en `ci.yml` corre todos los jobs, `docs` revisa todos los `.md` y sus anclas, `lintDebug`, informes como artefacto, permisos de solo lectura y acciones fijadas por SHA, verificación de la zona horaria del teléfono (`LocalDate.now()` y similares) con una lista de excepciones que solo se achica, `.gitattributes` con LF para scripts; Dependabot con los tres builds de Gradle juntos, Kotlin y KSP aparte, las versiones mayores aparte y 14 días de espera ([31](31-calidad-y-ci.md)); P-14: `openapi.yaml` y su validación ([92](92-pendientes.md)) | Decisiones tomadas al implementar la integración continua, revisadas una por una con el equipo (#31) |
| 1.2.6 | 16/10/2026 | Sesión en la app: cuenta y tokens cifrados en `secrets`, renovación de a una vez y no cancelable, pedidos públicos sin token, sesión vencida que conserva los datos de la misma cuenta y los borra si entra otra (con la renovación perdida como riesgo aceptado y medible), cierre de sesión que sin conexión borra el teléfono al instante y revoca la sesión cuando vuelve la red, y sincronización solo con sesión ([15](15-android.md), [14](14-sincronizacion.md)); el backend registra las revocaciones por reuso del token de renovación ([11](11-backend.md)); plugin `Auth` y Ktor Client Mock ([01](01-stack.md)) | Decisiones tomadas al implementar la sesión persistente en la app, revisadas una por una con el equipo (#34); SPEC funcional 1.4.0 (D-67) |
| 1.2.5 | 16/10/2026 | Base local cifrada: dónde viven la clave y el keyset, clave cruda de SQLCipher, base fuera de las copias de seguridad, recuperación si la clave o el Keystore fallan (con el keyset sin Keystore como riesgo aceptado), y cierre de sesión que borra base y clave y reinicia la app; los repositorios reciben el DAO inyectado y usan `ArgentinaClock` para "hoy" ([15](15-android.md)); trabajos únicos de sincronización, acelerados solo desde Android 12, y reglas para que dos sincronizaciones no se pisen ([14](14-sincronizacion.md)); versiones de Room, SQLCipher, WorkManager, DataStore, Tink y Robolectric, y `sdk` de Robolectric igual al `targetSdk` ([01](01-stack.md)); verificación de `LocalDate.now()` y caché de Robolectric en la integración continua ([31](31-calidad-y-ci.md)) | Decisiones tomadas al implementar la base local y el esqueleto de sincronización, revisadas una por una con el equipo (#35) |
| 1.2.4 | 16/10/2026 | Detalles de la rotación del token de renovación y variable `TERMS_VERSION` ([11](11-backend.md)); límites de `/auth/register` y `/auth/refresh` como supuestos y P-13 para el límite general por cuenta ([92](92-pendientes.md)) | Decisiones tomadas al implementar la sesión persistente (#33) |
| 1.2.3 | 16/10/2026 | Ktor Server 3.6.0 y kotlinx.serialization 1.11.0 en los tres builds (P-12 resuelto); firma de `formatAge`, que antes de la fecha de término informa cuánto falta (SPEC funcional 1.3.0, D-66); `ErrorCode` tolera códigos desconocidos ([01](01-stack.md), [10](10-modulo-compartido.md), [92](92-pendientes.md)) | Decisiones tomadas al implementar el módulo `shared` (#30) |
| 1.2.2 | 16/10/2026 | `author_account_id` sin FK a `accounts`; las FK hacia tablas de incrementos posteriores se agregan con esas tablas ([13](13-base-de-datos.md)) | Decisiones tomadas al implementar la línea base (#32) |
| 1.2.1 | 16/10/2026 | El encabezado cita la SPEC funcional vigente (1.2.0) | Corrección de redacción (#75) |
| 1.2.0 | 14/10/2026 | Tablero: qué movimientos son automáticos y cómo están configuradas las automatizaciones del proyecto; dependencias entre tareas cargadas como dependencias nativas de GitHub y verificación 4 basada en ellas ([32](32-proceso-de-desarrollo.md)) | Revisión del tablero antes de empezar I0 (#71) |
| 1.1.0 | 14/10/2026 | Base de Room recreada al cambiar de versión hasta el piloto; P-04 (email real) pasa a antes de I6; entorno `pilot` con datos ficticios y aviso permanente en la app | Ajustes surgidos al armar el plan de implementación; piloto con datos ficticios (D-65 de la SPEC funcional) |
| 1.0.0 | 14/10/2026 | Versión inicial | Relevamiento técnico en 2 rondas (14/10/2026) sobre la SPEC funcional v1.0.0 |
