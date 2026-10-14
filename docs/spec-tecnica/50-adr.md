# 50 · Decisiones de arquitectura (ADR)

Cada decisión registra su contexto, la decisión, las alternativas descartadas y sus
consecuencias. Las ADR-01 a ADR-22 se tomaron con el equipo en las rondas técnicas 1 y 2 del
14/10/2026 y la ADR-25 se aceptó después. Las ADR-23, 24 y 26 a 28 son decisiones de detalle de
esta SPEC, en revisión por el equipo.

| ID | Decisión | Estado |
|---|---|---|
| ADR-01 | Módulo compartido Kotlin/JVM con build compuesto | Aceptada |
| ADR-02 | App con Room como fuente de datos local y sincronización con WorkManager desde I0 | Aceptada |
| ADR-03 | Nueva línea base del esquema con UUID | Aceptada |
| ADR-04 | API REST JSON en `/api/v1` con OpenAPI | Aceptada |
| ADR-05 | Backend en Docker, independiente del proveedor; hosting a definir | Aceptada (proveedor pendiente) |
| ADR-06 | Push FCM sin datos personales | Aceptada |
| ADR-07 | Email transaccional por SMTP con proveedor en la UE | Aceptada |
| ADR-08 | Sentry en región UE sin datos personales | Aceptada |
| ADR-09 | Panel de administración con Ktor, Thymeleaf y HTMX | Aceptada |
| ADR-10 | Informes PDF generados en el servidor | Aceptada |
| ADR-11 | Tokens de acceso de 15 min y de renovación rotativos de 30 días | Aceptada |
| ADR-12 | Renombre completo a Pasitos | Aceptada (repositorio ya renombrado) |
| ADR-13 | Prueba cerrada de Google Play para el piloto | Aceptada |
| ADR-14 | Integración continua con GitHub Actions | Aceptada |
| ADR-15 | Sincronización por versión global, lápidas y bandeja de salida | Aceptada |
| ADR-16 | Alcance de datos en el teléfono según el rol | Aceptada |
| ADR-17 | Base local cifrada con SQLCipher | Aceptada |
| ADR-18 | Tareas programadas con db-scheduler; avisos a las 09:00 | Aceptada |
| ADR-19 | Estrategia de pruebas en cuatro niveles | Aceptada |
| ADR-20 | Percentiles y estados calculados al leer | Aceptada |
| ADR-21 | Inglés en código, API y base de datos | Aceptada |
| ADR-22 | Tareas en GitHub Issues y Projects con reglas de trabajo en paralelo | Aceptada |
| ADR-23 | Migraciones versionadas por fecha y hora | Propuesta |
| ADR-24 | Eventos automáticos derivados, no guardados | Propuesta |
| ADR-25 | Incremento I0 de fundaciones técnicas | Aceptada |
| ADR-26 | Fechas del calendario como `date` en hora argentina | Propuesta |
| ADR-27 | Páginas web del backend para los links de email | Propuesta |
| ADR-28 | Escrituras sincronizables solo por `/sync/push` | Propuesta |

---

### ADR-01 · Módulo compartido con build compuesto

- **Contexto:** la app debe calcular percentiles sin conexión (RNF-OFF-02) y el backend los
  necesita para informes, estados y alertas. Dos implementaciones divergirían (riesgo R-01).
- **Decisión:** biblioteca `shared/` en Kotlin/JVM puro, incluida por `app/` y `backend/` como
  build compuesto de Gradle.
- **Alternativas:** Kotlin Multiplatform (innecesario mientras no haya iOS; `shared` puede
  migrarse después); publicar el módulo en un repositorio Maven (más pasos para cada cambio);
  duplicar el código (descartado).
- **Consecuencias:** misma versión de Kotlin en los tres builds; `shared` sin dependencias de
  plataforma; la app sube a Java 17.

### ADR-02 · Room como fuente de datos local desde I0

- **Contexto:** RNF-OFF-01 a 06 exigen lectura y carga sin conexión. Hoy los repositorios leen
  directamente de la red.
- **Decisión:** las pantallas leen de Room; un `SyncWorker` con WorkManager sincroniza. Se
  construye en I0 e I1; la experiencia sin conexión se completa en I5.
- **Alternativas:** solo en línea con caché (no cumple la carga sin conexión); agregar Room en I5
  (obliga a reescribir todos los repositorios).
- **Consecuencias:** más trabajo inicial en I0; cada dominio repite la misma receta.

### ADR-03 · Nueva línea base del esquema

- **Contexto:** `personas` mezcla tutores e hijos con un único `tutor_id`; las claves numéricas
  impiden crear registros sin conexión. No hay datos reales.
- **Decisión:** reemplazar V1 y V2 por un esquema nuevo según el modelo conceptual, con UUID.
- **Alternativas:** migraciones incrementales sobre `personas` (arrastra el modelo viejo y
  complica cada consulta).
- **Consecuencias:** cada integrante borra su base local una vez; se coordina con todo el equipo
  antes de mergear; el código actual de `auth`, `child` y `medicion` se adapta en I0 e I1.

### ADR-04 · API REST en `/api/v1`

- **Decisión:** REST con JSON, rutas versionadas, OpenAPI y contratos en `shared/api`.
- **Alternativas:** GraphQL (más complejo y sin ventaja clara para este dominio).
- **Consecuencias:** solo cambios compatibles dentro de `v1`.

### ADR-05 · Backend independiente del proveedor

- **Contexto:** no hay infraestructura de la universidad; el proveedor se elige más adelante; el
  art. 12 de la Ley 25.326 limita dónde alojar los datos.
- **Decisión:** imagen Docker y configuración por variables de entorno. El proveedor se elige
  antes de I6 con los criterios de [30](30-infraestructura.md#requisitos-para-elegir-el-proveedor-p-01).
- **Consecuencias:** las demos quincenales corren en local; el despliegue se prueba antes del
  piloto.

### ADR-06 · Push sin datos personales

- **Contexto:** FCM es de Google (Estados Unidos, sin nivel adecuado según la AAIP).
- **Decisión:** mensajes FCM solo de datos con el UUID de la notificación; la app pide el
  contenido al backend.
- **Alternativas:** enviar título y cuerpo por FCM (transfiere nombres de niños a Google).
- **Consecuencias:** un request extra por notificación; texto genérico si no hay conexión.

### ADR-07 · Email por SMTP con proveedor en la UE

- **Decisión:** SMTP con un proveedor con servidores en la Unión Europea y plan gratuito.
- **Consecuencias:** cambiar de proveedor es cambiar variables de entorno; requiere dominio
  propio con SPF y DKIM (P-03).

### ADR-08 · Sentry en región UE

- **Decisión:** Sentry para app y backend, en su región europea, sin datos personales.
- **Alternativas:** Firebase Crashlytics (Estados Unidos).

### ADR-09 · Panel con Ktor, Thymeleaf y HTMX

- **Contexto:** el equipo domina Kotlin; el panel tiene formularios y tablas, sin interacción
  compleja.
- **Decisión:** páginas generadas por el backend con Thymeleaf y HTMX.
- **Alternativas:** aplicación web aparte con React o Vue (otro stack y otro despliegue).
- **Consecuencias:** un solo despliegue; las mismas plantillas sirven para emails y PDFs.

### ADR-10 · PDFs en el servidor

- **Decisión:** plantilla Thymeleaf a PDF con openhtmltopdf y curvas en SVG.
- **Alternativas:** generar el PDF en el teléfono (duplica el dibujo de curvas y depende de
  tablas locales).
- **Consecuencias:** los informes requieren conexión, como ya define RNF-OFF-06.

### ADR-11 · Tokens de acceso y renovación

- **Contexto:** RF-CTA-06 pide 30 días de sesión; hoy hay un JWT de 8 horas sin renovación.
- **Decisión:** acceso de 15 minutos; renovación opaca, rotativa, de 30 días que se extiende con
  el uso, con detección de reutilización. Google con Credential Manager.
- **Consecuencias:** se reemplaza el esquema actual de `sesiones`.

### ADR-12 · Renombre a Pasitos

- **Decisión:** en I0, un único PR cambia `applicationId`, `namespace`, paquetes
  (`com.ninosano.app` → `com.pasitos.app`), nombre del proyecto de Gradle, README y textos. El
  repositorio se renombró en GitHub el 14/10/2026: `NelsonLoto/control-nino-sano-app` →
  `NelsonLoto/pasitos` (P-02).
- **Consecuencias:** el renombre de paquetes se hace sin ramas abiertas (confirmado el
  14/10/2026). GitHub redirige las URLs viejas del repositorio, pero cada integrante actualiza
  su remoto. La carpeta local puede seguir llamándose como antes.

### ADR-13 · Distribución

- **Decisión:** prueba cerrada de Google Play para el piloto; Firebase App Distribution para
  versiones internas; firma de apps de Google Play.

### ADR-14 · Integración continua

- **Decisión:** GitHub Actions con jobs por componente y filtros por ruta; regla de equipo de no
  mergear en rojo.

### ADR-15 · Protocolo de sincronización

- **Decisión:** versión global por secuencia de PostgreSQL, lápidas con `deleted_at`, bandeja de
  salida en el teléfono, `UPSERT` idempotente por UUID, último cambio gana (solo el autor edita),
  fotos completas para niños que entran al alcance y avisos de revocación.
- **Alternativas:** marcas de tiempo como cursor (sensibles a relojes y transacciones
  concurrentes); librerías de sincronización de terceros (dependencia pesada).

### ADR-16 · Alcance por rol

- **Decisión:** el tutor sincroniza todo de sus hijos; el pediatra, la lista de pacientes y el
  detalle de hasta 50 abiertos recientemente, sin el detalle de hábitos.

### ADR-17 · Base local cifrada

- **Decisión:** SQLCipher con clave aleatoria protegida por el Android Keystore mediante Tink.

### ADR-18 · Tareas programadas

- **Decisión:** db-scheduler sobre PostgreSQL; notificaciones diarias a las 09:00 hora argentina.

### ADR-19 · Pruebas en cuatro niveles

- **Decisión:** motor compartido, backend con PostgreSQL real, app y aceptación manual
  ([31](31-calidad-y-ci.md)).

### ADR-20 · Datos derivados calculados al leer

- **Contexto:** RN-CRE-13 pide mostrar siempre los valores con las tablas vigentes; el código
  actual guarda el percentil en la medición.
- **Decisión:** no se guardan percentiles ni estados; se calculan con el motor compartido.
- **Consecuencias:** se eliminan las columnas de percentiles de la tabla de mediciones.

### ADR-21 · Idioma del código

- **Decisión:** inglés en código, API y base; español en interfaz y documentación; tabla de
  equivalencias en [02](02-estructura-del-repo.md#equivalencias-de-nombres).

### ADR-22 · Organización de tareas

- **Decisión:** GitHub Issues y Projects; verificación de trabajo en curso antes de tomar una
  tarea, también para agentes de IA ([32](32-proceso-de-desarrollo.md#4-trabajo-en-paralelo)).

### ADR-23 · Migraciones versionadas por fecha y hora (propuesta)

- **Contexto:** Flyway aplica las migraciones en orden de versión (`V1`, `V2`, `V3`…) y exige
  que cada versión sea única. Con 4 personas trabajando en paralelo, es normal que dos ramas
  necesiten cambiar la base al mismo tiempo. Si las dos crean `V3__….sql`, al mergear la segunda
  hay dos migraciones con la misma versión y el backend **no arranca** ("Found more than one
  migration with version 3"). Renumerar a mano antes de mergear es posible, pero si alguien ya
  aplicó su `V3` en su base local, Flyway detecta la diferencia y también falla.
- **Decisión:** la versión es la fecha y hora en que se crea la migración:
  `V2026_10_20_1530__create_children.sql` (Flyway la lee como `2026.10.20.1530`). Dos personas
  tendrían que crear una migración en el mismo minuto para chocar. Se activa
  `outOfOrder = true`: si una rama creó su migración el lunes pero se mergea después de otra
  creada el martes, Flyway igual la aplica en las bases que ya tenían la del martes.
- **Alternativas:**
  - Números correlativos con la regla "una sola PR con migración abierta a la vez": genera
    esperas entre integrantes, justo lo que el plan de trabajo en paralelo quiere evitar.
  - Renumerar antes de cada merge: trabajo manual y propenso a errores.
- **Consecuencias:**
  - Cada migración debe ser **independiente** de las de otras ramas en curso. Si una migración
    necesita una tabla que crea otra rama, se espera a que esa rama se mergee (ya es así por
    las dependencias del plan).
  - En el entorno del piloto, las migraciones pueden aplicarse en un orden levemente distinto al
    de las fechas; no hay problema si son independientes.

### ADR-24 · Eventos automáticos derivados, no guardados (propuesta)

- **Contexto:** la línea de tiempo muestra eventos automáticos ("Medición", "Vacuna aplicada",
  "Control con la Dra. Pérez", "Cambio de pediatra", "Hito logrado") junto con los manuales.
  Una forma de hacerlo es guardar una fila en `events` cada vez que ocurre algo. Pero ese dato
  ya existe en su tabla de origen, y la copia hay que mantenerla igual: si el tutor corrige la
  fecha de una medición, hay que corregir también su evento; si la borra, borrar el evento. Un
  olvido en cualquiera de esos caminos deja eventos "fantasma" o con datos viejos, y RN-EVT-02
  dice justamente que el evento automático refleja su registro de origen.
- **Decisión:** la tabla `events` guarda **solo los eventos manuales**. La línea de tiempo se
  arma en el momento de mostrarla, juntando en una sola lista ordenada por fecha: mediciones,
  dosis aplicadas, consultas cerradas, inicios y fines de vínculos, hitos logrados y eventos
  manuales. En la app es una consulta a Room sobre datos que ya están en el teléfono.
- **Alternativas:** guardar los eventos automáticos con triggers de base de datos o desde cada
  servicio (más código, más puntos de falla, datos duplicados).
- **Consecuencias:**
  - Imposible que un evento quede desactualizado: si cambia el origen, cambia lo que se ve.
  - La consulta de la línea de tiempo combina seis fuentes. Con los volúmenes de un niño
    (cientos de registros) es inmediata.
  - Los hábitos no entran en la línea de tiempo (RN-HAB-03).

### ADR-25 · Incremento I0 de fundaciones técnicas

Aceptada el 14/10/2026 e incorporada al plan de incrementos de la SPEC funcional (v1.1.0).

- **Contexto:** varias decisiones (módulo compartido, nuevo esquema, Room y sincronización,
  sesión persistente, renombre) cambian la base de todo el código. Si se hacen dentro de I1 o
  más tarde, obligan a rehacer lo construido.
- **Decisión:** antes de I1, un incremento sin requerimientos funcionales propios con las tareas
  de [00](00-arquitectura.md#5-fundaciones-técnicas-incremento-i0).
- **Consecuencias:** la primera demo quincenal muestra fundaciones (integración continua en
  verde, app renombrada que inicia sesión y la conserva, cálculo de prueba del motor) en lugar de
  funcionalidad nueva.

### ADR-26 · Manejo de fechas y zona horaria (propuesta)

- **Contexto:** Argentina está en UTC−3, y el servidor del piloto probablemente esté en Europa,
  con su reloj en UTC. Dos ejemplos de errores típicos si no se define una regla:
  - Un tutor carga a las 22:00 una medición "de hoy, 14/10". Si se guarda como momento en UTC,
    queda registrada el **15/10** a la 01:00. La edad en la medición, el percentil y el orden en
    la línea de tiempo salen corridos un día.
  - Entre las 21:00 y la medianoche de Argentina, en un servidor en UTC ya es el día siguiente.
    La validación "la fecha no puede ser futura" aceptaría mañana como fecha válida, y una dosis
    pasaría a "Atrasada" o un niño cumpliría 15 años un día antes.
- **Decisión:** dos tipos de dato, nunca mezclados.
  - **Fechas del calendario** (nacimiento, medición, aplicación de vacuna, consulta, evento):
    tipo `date`, sin hora ni zona. "14/10/2026" es lo que dijo el usuario y nada más.
  - **Momentos** (hora de un hábito, accesos, sesiones, notificaciones): `timestamptz` guardado
    en UTC y mostrado en hora argentina.
  - **"Hoy"** siempre se calcula en `America/Argentina/Buenos_Aires`, en la app y en el
    servidor, sin depender del reloj del sistema. Las tareas programadas usan la misma zona
    (por ejemplo, avisos a las 09:00 de Argentina aunque el servidor esté en Europa).
- **Alternativas:** guardar todo como momento en UTC (genera los errores de arriba) o confiar en
  la zona del servidor (cambia según dónde se aloje).
- **Consecuencias:** en Kotlin se usa `LocalDate` para fechas e `Instant` para momentos; una
  función única `ArgentinaClock.today()` en app y backend.

### ADR-27 · Páginas web del backend para los links de email (propuesta)

- **Contexto:** los emails de verificación de cuenta, recuperación de contraseña y cambio de
  email llevan un link. Hay que decidir qué abre ese link. Además, Google Play exige que la
  cuenta se pueda eliminar también desde una página web (RNF-LEG-03).
- **Decisión:** el link abre una página simple servida por el backend (`/web/verify-email`,
  `/web/reset-password`, etc.) que hace la acción y muestra el resultado, por ejemplo "Tu email
  quedó verificado. Volvé a la app para continuar". La app detecta el cambio al consultar
  `GET /me` (botón "Ya verifiqué" o al volver a primer plano).
- **Alternativas:** *App Links*, que abren la app directamente desde el link. Requieren un
  dominio propio verificado y que la app esté instalada **en el mismo dispositivo** donde se lee
  el email: si el tutor abre el email en la computadora, el link no sirve. Para recuperar una
  contraseña desde la computadora no habría forma.
- **Consecuencias:** cuatro páginas HTML con Thymeleaf, la misma tecnología del panel de
  administración. Funcionan desde cualquier dispositivo y navegador, y la de eliminación de
  cuenta cumple el requisito de Google Play.

### ADR-28 · Un solo camino de escritura para lo sincronizable (propuesta)

- **Contexto:** mediciones, hábitos, eventos manuales e hitos logrados pueden cargarse sin
  conexión (RNF-OFF-02), así que la app ya necesita enviarlos por `POST /sync/push`. Si además
  existieran endpoints REST tradicionales para cargarlos con conexión (`POST /measurements`),
  habría **dos caminos** para crear lo mismo: dos implementaciones de validación que pueden
  divergir (una verifica "una medición por autor y por fecha" y la otra se olvida), el doble de
  pruebas, y casos ambiguos como una medición creada por REST que también quedó en la bandeja de
  salida.
- **Decisión:** esas cuatro entidades se escriben **siempre** por `/sync/push`, con o sin
  conexión. La app guarda en Room y en la bandeja de salida y, si hay conexión, el trabajo de
  sincronización la envía en segundos. En el servidor hay una única función de validación por
  entidad.
- **Alternativas:** REST con conexión y sincronización solo sin conexión (los problemas de
  arriba).
- **Consecuencias:**
  - El usuario ve su dato guardado al instante, pero un eventual rechazo del servidor llega unos
    segundos después y se muestra sobre el registro (RNF-OFF-05), no en el momento de tocar
    "Guardar". Para que sea raro, la app aplica antes las mismas validaciones con el módulo
    compartido.
  - El pediatra usa el mismo endpoint, pero la app solo se lo permite con conexión.
  - Las demás entidades (hijos, vacunas, vínculos, consultas) siguen con endpoints REST porque
    siempre requieren conexión (RNF-OFF-06).
