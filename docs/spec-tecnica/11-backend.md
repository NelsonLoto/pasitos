# 11 · Backend

## 1. Capas

Cada dominio (`child`, `measurement`, `carelink`, …) tiene la misma forma. Es la receta que se
repite en cada incremento (principio T1):

| Capa | Archivo | Responsabilidad | No hace |
|---|---|---|---|
| Rutas | `ChildRoutes.kt` | Leer el request, validar formato, obtener el usuario autenticado, llamar al servicio y responder | Reglas de negocio ni acceso a datos |
| Servicio | `ChildService.kt` | Reglas de negocio, permisos con `AccessPolicy`, transacción, registro de accesos | Conocer HTTP |
| Repositorio | `ChildRepository.kt` | Consultas Exposed y mapeo de filas a modelos | Reglas de negocio |
| Tablas | `ChildTables.kt` | Definición Exposed de las tablas del dominio | — |

```kotlin
// Rutas: solo HTTP
post("/children") {
    val caller = call.caller()                          // cuenta autenticada
    val request = call.receive<CreateChildRequest>()    // DTO de shared
    call.respond(HttpStatusCode.Created, childService.create(caller, request))
}

// Servicio: reglas, permisos y transacción
fun create(caller: Caller, request: CreateChildRequest): ChildDto = tx {
    ChildRules.validate(request, today())               // RN-HIJ-01 a 03
    val child = childRepository.insert(request, createdBy = caller.accountId)
    guardianshipRepository.insert(child.id, caller.accountId, Role.PRINCIPAL)
    child.toDto()
}
```

Las dependencias se crean a mano en `Application.module()` (sin framework de inyección): con
pocos servicios, un archivo `Dependencies.kt` que los construye es suficiente y fácil de seguir.

## 2. Guía: agregar un dominio

1. Escribir los DTOs en `shared/api` (o ajustarlos) según [12 · API](12-api.md).
2. Crear la migración en `db/migration` ([13 · Base de datos](13-base-de-datos.md#migraciones)).
3. Crear `XTables.kt`, `XRepository.kt`, `XService.kt` y `XRoutes.kt` en `com.pasitos.backend.x`.
4. Registrar las rutas en `Application.module()` dentro de `route("/api/v1")`.
5. Agregar las reglas de permiso en `AccessPolicy` y sus pruebas de matriz.
6. Escribir una prueba de integración por criterio de aceptación del RF.

## 3. Autenticación

### Cuentas de la app (ADR-11)

| Elemento | Definición |
|---|---|
| Token de acceso | JWT HS256 de **15 minutos**. Claims: `sub` (id de cuenta), `sid` (id de sesión), `jti`. Se envía como `Authorization: Bearer` |
| Token de renovación | 256 bits aleatorios, opaco. Se guarda solo su hash SHA-256 en `sessions`. Vence a los **30 días sin uso**: cada renovación extiende el plazo (RN-CTA-07) |
| Rotación | `POST /auth/refresh` entrega un par nuevo e invalida el token de renovación anterior. Si llega un token ya rotado, se revoca la sesión completa (posible robo) y se registra en el log con el ID de la sesión, sin datos personales. La sesión guarda solo el token anterior (`previous_refresh_token_hash`): uno más viejo se rechaza con 401 sin revocar. La app debe renovar de a un pedido por vez (el plugin `Auth` de Ktor Client ya lo hace), o dos renovaciones simultáneas parecerían una reutilización |
| Revocación | Cerrar sesión borra la fila de `sessions`. Cada request valida que `sid` siga vigente (RNF-SEG-04) |
| Cambio o recuperación de contraseña | Revoca todas las sesiones de la cuenta (RF-CTA-05, CA2) |
| Google | La app obtiene un ID token con Credential Manager y lo envía a `POST /auth/google` (I1-B2). Detalles abajo |
| Bloqueo por intentos | `accounts.failed_login_count` y `locked_until` (RN-CTA-05) |
| Verificación y recuperación | Tokens aleatorios de un solo uso, guardados como hash en `email_tokens` con su vencimiento (RN-CTA-06). Los links abren páginas del backend ([16](16-panel-admin.md#páginas-públicas)) |
| Email sin verificar | `POST /auth/login` con credenciales correctas pero sin verificar responde 403 `email_not_verified`, sin abrir sesión ni contar como intento fallido (RF-CTA-02, CA1). El registro ya abre sesión igual (RF-CTA-01, CA1): la restricción es solo al iniciar sesión de nuevo |

### Inicio con Google (I1-B2)

`auth/google/`: `GoogleTokenVerifier` verifica el ID token y `GooglePublicKeys` le da las claves
de Google.

- **Verificación,** con `java-jwt` y `jwks-rsa`:
  - firma RS256 con una clave publicada por Google en `https://www.googleapis.com/oauth2/v3/certs`.
    El algoritmo es fijo: un token que declare otro se rechaza;
  - `iss` igual a `https://accounts.google.com` o `accounts.google.com`;
  - `aud` igual exactamente al ID del cliente web (`GOOGLE_CLIENT_ID`), y a ningún otro;
  - vencimiento, con 60 segundos de tolerancia por diferencia de reloj, y `email_verified` en
    `true`.
- **Claves:** se bajan la primera vez que aparece cada ID de clave y se guardan un día. Cuando
  Google las rota, un ID nuevo provoca otra descarga. Como mucho se piden 10 veces por minuto, así
  que tokens con IDs inventados no hacen que el backend consulte a Google sin parar.
- **Pruebas:** usan una clave RSA propia publicada en el mismo formato (`TestGoogle`), sin red.
- **Búsqueda de la cuenta:** primero por `google_subject`, que no cambia aunque cambie el email, y
  después por email:

| Caso | Resultado |
|---|---|
| Ya ingresó con esa cuenta de Google | Ingresa |
| El email es de una cuenta verificada | Asocia `google_subject` e ingresa. La contraseña sigue funcionando (RN-CTA-04) |
| El email es de una cuenta sin verificar | La marca verificada, asocia `google_subject`, borra la contraseña y cierra todas sus sesiones, y después ingresa (RN-CTA-04, D-69): la contraseña y las sesiones pudo crearlas otra persona que registró ese email antes que su dueño |
| El email ya tiene otra cuenta de Google | 409 `email_already_in_use`, por ejemplo con una dirección de una organización que se reasignó |
| Primer ingreso sin `acceptTerms` | 409 `terms_required`, sin guardar nada (RF-CTA-03, CA1). La app muestra los términos con nombre y apellido precargados y reenvía el mismo token |
| Primer ingreso con `acceptTerms` | Crea la cuenta de tutor, verificada y sin contraseña, con el nombre y el apellido confirmados (400 si faltan), y guarda el consentimiento |

- **Errores:** si el token no es válido, 401 `invalid_google_token`. Si Google no responde al pedir
  las claves, 503 `google_unavailable`, sin abrir sesión.
- **Bloqueo por intentos:** no aplica, porque no interviene la contraseña (RN-CTA-05).
- **Dos primeros ingresos simultáneos:** el segundo choca con el índice único de `accounts`.
  Exposed reintenta la transacción, y en el reintento encuentra la cuenta que creó el primero.

### Administradores

Sesión de cookie del panel web, separada de las cuentas de la app. Ver
[16 · Panel de administración](16-panel-admin.md#acceso).

## 4. Autorización

Toda operación sobre datos de un niño pasa por `AccessPolicy`, que implementa la matriz de
[01 · Actores y permisos](../spec/01-actores-y-permisos.md):

```kotlin
enum class ChildAccess { NONE, GUARDIAN, PRINCIPAL_GUARDIAN, CURRENT_PEDIATRICIAN }

class AccessPolicy(private val guardianships: GuardianshipRepository) {
    fun accessTo(caller: Caller, childId: UUID): ChildAccess
    fun require(caller: Caller, childId: UUID): ChildAccess   // lanza ChildNotFoundException (404)
}
```

`CURRENT_PEDIATRICIAN` queda reservado hasta que exista el vínculo con pediatras (I3): ningún
código lo devuelve todavía. `require` no distingue acciones (I1-B4 no lo necesitó: `GET`, `PATCH`
y `DELETE` de un niño alcanzan con "es tutor, de cualquier rol"); un parámetro de acción se agrega
cuando una tarea futura lo necesite.

Reglas transversales:

- Si el usuario no tiene ningún acceso al niño, la respuesta es **404**, no 403, para no
  revelar que el niño existe.
- Las observaciones privadas (`private_notes`) y las notas aclaratorias privadas solo se
  serializan cuando quien pide es su autor. El DTO para tutores no tiene esos campos (RF-CON-08,
  RNF-SEG-05).
- La edición o eliminación de un registro exige `author_account_id == caller` (RN de autoría).
- **Eliminar el perfil de un niño** (RN-HIJ-08): `ChildService.delete` llama, dentro de su
  transacción, al `ChildDataPurger` de cada dominio que guarda datos del niño. Así el dominio de
  hijos no conoce a los demás. Cada dominio nuevo con datos del niño registra el suyo en
  `Dependencies.kt`; vacunación lo hace desde I1-B7.
- El pediatra anterior accede solo a `GET` de sus propias consultas (RN-PED-04).
- Cada acceso de un pediatra a datos de un niño y cada modificación o exportación se registra en
  `audit_log` desde el servicio (RNF-SEG-06).

## 5. Errores

Formato único (`ErrorResponse` en `shared`):

```json
{ "code": "child_not_found", "message": "No se encontró ese perfil" }
```

| HTTP | Uso |
|---|---|
| 400 | Formato inválido (`invalid_input`), con `fields` opcional por campo |
| 401 | Sin token, token vencido o sesión revocada (`unauthorized`); email o contraseña incorrectos (`invalid_credentials`); ID token de Google inválido (`invalid_google_token`) |
| 403 | Autenticado con acceso al recurso pero sin permiso para esa acción (`forbidden`) |
| 404 | Recurso inexistente o sin acceso (`*_not_found`) |
| 409 | Conflicto de estado: email en uso, matrícula registrada, solicitud pendiente existente, primer ingreso con Google sin aceptar los términos (`terms_required`) |
| 422 | Regla de negocio incumplida (`business_rule`), con el ID de la regla, por ejemplo `RN-HIJ-01` |
| 423 | Cuenta bloqueada por intentos (`account_locked`) |
| 429 | Límite de requests (`rate_limited`) |
| 500 | Error inesperado (`internal_error`). Se registra en Sentry sin datos personales |
| 503 | Un servicio externo no respondió: Google, al verificar un ID token (`google_unavailable`) |

Los mensajes `message` están en español y listos para mostrar.

## 6. Tareas programadas (ADR-18)

Con **db-scheduler**, que guarda las tareas en la tabla `scheduled_tasks` y garantiza que cada
una corra una sola vez aunque el servidor se reinicie.

| Tarea | Frecuencia | Qué hace | Requerimientos |
|---|---|---|---|
| `daily-notifications` | Diaria, 09:00 hora argentina | Genera N-01, N-02, N-03 y N-08 del día y las envía | RF-NOT-03, RN-NOT-02, 03 y 05 |
| `expire-invitations` | Cada hora | Vence invitaciones y solicitudes de más de 15 días, borra los datos de invitados vencidos, genera N-04 y N-09 | RN-HIJ-07, RN-PED-06 y 07 |
| `finalize-profiles` | Diaria, 00:15 hora argentina | Pasa a `FINALIZED` los perfiles que cumplen 15 años y termina su vínculo | RF-HIJ-10, RN-HIJ-09 |
| `cleanup-sessions` | Diaria | Borra sesiones vencidas y tokens de email usados o vencidos | RNF-SEG-04 |
| `cleanup-audit` | Mensual | Borra registros de acceso de más de 2 años | RNF-SEG-06 |

Las notificaciones que dependen de un evento (N-04 a N-07, N-09 a N-12) no esperan a la tarea
diaria: las crea el servicio en el momento.

Hasta que llegue `expire-invitations` (I6), una invitación de tutor vencida sigue en `PENDING` y
el vencimiento se calcula al leer (`expires_at`): no se puede aceptar ni rechazar, el principal
la ve como vencida en `GET /children/{id}/guardian-invitations` y la reenvía, o invita de nuevo
al mismo email, lo que la pasa a `CANCELLED` (RF-HIJ-05, CA3).

## 7. Configuración

Todo por variables de entorno, con `.env` solo en desarrollo local (nunca versionado):

| Variable | Ejemplo | Uso |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | — | Conexión a PostgreSQL |
| `JWT_SECRET` | 64 caracteres aleatorios | Firma de tokens de acceso |
| `GOOGLE_CLIENT_ID` | ID del cliente web ([30](30-infraestructura.md#proyecto-de-google-cloud-y-firebase)) | `aud` esperado en tokens de Google. Opcional: por defecto, el del proyecto `pasitos-751c4` |
| `FIREBASE_CREDENTIALS_FILE` | ruta a JSON | Envío FCM |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `MAIL_FROM` | — | Emails |
| `PUBLIC_BASE_URL` | `https://api.pasitos.ar` | Links en emails |
| `SENTRY_DSN`, `SENTRY_ENVIRONMENT` | — | Errores |
| `ADMIN_BOOTSTRAP_EMAIL` | — | Crea el primer administrador si no existe ninguno |
| `TERMS_VERSION` | `1` | Versión vigente de los términos y la política de privacidad (RN-CTA-02). Al cambiarla, `GET /me` la informa como pendiente |
| `APP_ENV` | `local`, `ci`, `pilot` | Comportamientos por entorno |

## 8. Registros (logs)

- Formato JSON en producción, legible en local.
- Cada request registra método, ruta (sin parámetros de consulta), estado, duración e
  identificador de request.
- **Nunca** se registran cuerpos de requests o responses, tokens, emails, nombres ni datos de
  salud (RNF-PRI-03). Los identificadores (UUID) sí.

## 9. Documentación de la API

El backend sirve la especificación OpenAPI en `/api/v1/openapi.yaml` y una página Swagger UI en
`/api/docs` (solo en `local` y `ci`). La especificación se mantiene junto al código y la
integración continua verifica que sea válida.
