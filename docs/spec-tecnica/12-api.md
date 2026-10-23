# 12 · API

## Convenciones

- Base: `/api/v1`. JSON UTF-8, campos en `camelCase`. HTTPS obligatorio fuera de `local`.
- Autenticación: `Authorization: Bearer <token de acceso>` en todo salvo `/auth/*` y `/health`.
- Fechas: `"2026-10-14"` (`LocalDate`). Momentos: ISO 8601 en UTC (`"2026-10-14T12:00:00Z"`).
- Identificadores: UUID. Los registros sincronizables los genera la app (ADR-03).
- Números: kg con hasta 3 decimales y cm con 1 (validado en servidor).
- Listas paginadas: `?limit=50&cursor=<opaco>` y respuesta `{ "items": [...], "nextCursor": "..." }`.
- Errores: ver [11 · Backend](11-backend.md#5-errores).
- Límite de requests: 120 por minuto por cuenta; `/auth/*` y búsqueda por matrícula con límites
  propios (RNF-SEG-07).

## Regla de escritura (ADR-28)

| Tipo de entidad | Cómo se escribe | Cómo se lee en la app |
|---|---|---|
| **Sincronizable**: mediciones, hábitos, eventos manuales, hitos logrados | Solo por `POST /sync/push`, también estando en línea | `GET /sync/changes` |
| **Solo en línea**: cuentas, hijos, tutores, vacunas, vínculos, invitaciones, consultas | Endpoints REST de cada dominio | `GET /sync/changes` para los datos que se guardan en el teléfono; REST para el resto |

Así hay **un solo camino** para cada escritura sincronizable, sea con o sin conexión.

## Catálogo

### Autenticación y cuenta

| Método y ruta | Descripción | RF |
|---|---|---|
| `POST /auth/register` | Registro con email y contraseña, consentimiento y perfiles | RF-CTA-01 |
| `POST /auth/login` | Inicio de sesión. Devuelve tokens de acceso y renovación | RF-CTA-04 |
| `POST /auth/google` | Inicio o registro con ID token de Google: `{ idToken, acceptTerms, firstName, lastName }`. En el primer ingreso responde 409 `terms_required` hasta recibir `acceptTerms` con nombre y apellido ([11](11-backend.md#inicio-con-google-i1-b2)) | RF-CTA-03 |
| `POST /auth/refresh` | Rota el token de renovación | RF-CTA-06 |
| `POST /auth/logout` | Revoca la sesión actual | RF-CTA-07 |
| `POST /auth/email-verification` | Reenvía el email de verificación | RF-CTA-02 |
| `POST /auth/password-reset` | Pide el email de recuperación (respuesta idéntica exista o no la cuenta) | RF-CTA-05 |
| `GET /me` | Cuenta, perfiles, estado del perfil de pediatra y términos pendientes | RF-CTA-09 |
| `PATCH /me` | Nombre y apellido | RF-CTA-11 |
| `POST /me/email-change` | Pide cambio de email (requiere verificación del nuevo) | RF-CTA-11 |
| `POST /me/password` | Cambia la contraseña (pide la actual) | RF-CTA-11 |
| `POST /me/consents` | Acepta una versión nueva de los términos | RN-CTA-02 |
| `GET /me/deletion-preview` | Consecuencias de eliminar la cuenta | RF-CTA-10 |
| `DELETE /me` | Elimina la cuenta (requiere contraseña o confirmación) | RF-CTA-10 |
| `PUT /me/devices/{fcmToken}` · `DELETE …` | Registra o quita el dispositivo para push | RF-NOT-01 |
| `GET /me/notification-preferences` · `PUT …` | Preferencias por tipo | RF-NOT-02 |

### Hijos y tutores

| Método y ruta | Descripción | RF |
|---|---|---|
| `POST /children` | Alta de hijo. `avatarKey` es nulo o una de `AVATAR_KEYS` (`shared/api/child`); otra clave responde 400 (RN-HIJ-11) | RF-HIJ-01 |
| `GET /children/{id}` · `PATCH /children/{id}` | Perfil, alergias y antecedentes | RF-HIJ-02, 03 |
| `GET /children/{id}/deletion-preview` · `DELETE /children/{id}` | Eliminar o dejar de ser tutor | RF-HIJ-09 |
| `GET /children/{id}/guardians` | Tutores del niño | RF-HIJ-05 a 08 |
| `POST /children/{id}/guardian-invitations` | Invitar a otro tutor | RF-HIJ-05 |
| `DELETE /children/{id}/guardians/{accountId}` | Quitar tutor o abandonar (si es uno mismo) | RF-HIJ-07 |
| `POST /children/{id}/principal-transfer` | Transferir el rol principal | RF-HIJ-08 |
| `GET /me/invitations` | Invitaciones recibidas (tutor y reclamo) | RF-HIJ-06, RF-PED-12 |
| `POST /invitations/{id}/accept` · `POST /invitations/{id}/reject` | Responder una invitación de tutor | RF-HIJ-06 |
| `POST /invitations/{id}/resend` | Reenviar una invitación propia | RF-HIJ-05, RF-PED-09 |

### Crecimiento

| Método y ruta | Descripción | RF |
|---|---|---|
| `GET /reference-data/version` | Versión vigente de tablas y umbrales | RN-CRE-13 |
| `GET /reference-data` | Paquete completo `ReferenceData` | RF-CRE-05 a 08 |
| `GET /children/{id}/growth` | Mediciones con evaluación calculada en el servidor (para clientes sin caché y para pruebas) | RF-CRE-03, 05, 07 |

Las mediciones se crean, editan y eliminan por `/sync/push` (RF-CRE-01, RF-CRE-02).

### Vacunación

| Método y ruta | Descripción | RF |
|---|---|---|
| `GET /vaccination-calendar` | Versión vigente del calendario y campañas activas | RF-VAC-01, 06, 07 |
| `POST /children/{id}/applied-doses` | Registrar una dosis | RF-VAC-02 |
| `PATCH /applied-doses/{id}` · `DELETE /applied-doses/{id}` | Editar o eliminar una dosis propia | RF-VAC-03 |
| `POST /children/{id}/applied-doses/retroactive` | Carga retroactiva (lista de `doseCode`) | RF-VAC-04 |
| `POST /children/{id}/other-vaccines` · `PATCH`/`DELETE /other-vaccines/{id}` | Otras vacunas | RF-VAC-05 |

Implementación de I1-B7 (#46), con los contratos en `shared/api/vaccination`:

- **Calendario.**
  - `GET /vaccination-calendar` devuelve la versión vigente con todas sus filas (`doses`, también
    las de otras cohortes, RN-VAC-10), con qué previene y la fuente de cada vacuna (RF-VAC-07).
    La versión vigente es la `PUBLISHED` más reciente con `effectiveFrom` igual o anterior a hoy,
    en hora argentina.
  - `formerDoses` trae código, vacuna y nombre de las dosis de versiones anteriores que la vigente
    ya no tiene, para mostrar las aplicadas (RN-VAC-03).
  - Sin calendario publicado, `effectiveFrom` es nulo y las listas vienen vacías.
  - Las campañas se suman en I6 como un campo nuevo; la app ignora los campos que no conoce.
  - `ETag` con el hash SHA-256 de la respuesta: con el mismo valor en `If-None-Match`, responde
    `304` sin cuerpo.
  - `Cache-Control: private, no-cache`: el teléfono lo guarda pero vuelve a preguntar, y ningún
    intermediario lo guarda.
- **Dosis aplicadas.**
  - Solo se registran dosis del calendario vigente que corresponden al niño por su fecha de
    nacimiento. Una dosis aplicada antes de su fecha indicada se acepta.
  - La fecha no puede ser anterior al nacimiento ni posterior a hoy (RN-VAC-02, `400`).
  - Una dosis ya registrada responde `422` con la regla `RN-VAC-09`. La app ya tiene el registro
    existente por la sincronización y lo abre para verlo o editarlo (RF-VAC-02, CA4). Si dos
    tutores la registran a la vez, el índice único rechaza la segunda con la misma respuesta.
  - `PATCH` reemplaza fecha, lote y lugar. La dosis no cambia: para cambiarla se elimina y se
    registra otra. La fecha solo puede quedar vacía en una dosis de carga retroactiva que todavía
    no la tiene.
  - Los identificadores los genera el servidor, como en `/children`: no son escrituras de
    `/sync/push` (ADR-28).
- **Carga retroactiva.**
  - Recibe hasta 50 códigos. Todos tienen que ser del calendario vigente del niño, con fecha
    indicada anterior a hoy. Si uno no corresponde, se rechaza todo el pedido (`400`), en una
    transacción.
  - Las dosis ya aplicadas se saltean, así reenviar el pedido da el mismo resultado.
  - Responde `201` con las dosis creadas, sin fecha (RN-VAC-04).
- **Autoría y permisos.**
  - Registra cualquier tutor del niño.
  - Edita o elimina solo el autor; si no lo es, `403`.
  - Sin acceso al niño, `404`: `child_not_found` en las rutas del niño, `applied_dose_not_found`
    y `other_vaccine_not_found` en las de cada registro.
  - `authorRole` es `GUARDIAN` hasta que los pediatras tengan acceso (I3).
- **Límites de texto (SUP-24).**
  - Lote 40, lugar 120, nombre de otra vacuna 120 y nota 280.
  - Se recortan los espacios de los extremos y un texto vacío se guarda como nulo.
  - Las constantes están en `shared/api/vaccination`, para que la app use las mismas.

### Pediatras, vínculos y pacientes

| Método y ruta | Descripción | RF |
|---|---|---|
| `PUT /me/pediatrician-profile` | Crear o actualizar el perfil profesional | RF-PED-01, RN-PED-02 |
| `GET /pediatricians/search?jurisdiction=&number=` | Buscar por matrícula (solo validados) | RF-PED-03 |
| `POST /children/{id}/link-requests` | Solicitar vínculo a un pediatra registrado | RF-PED-04, 08 |
| `POST /children/{id}/pediatrician-invitations` | Invitar a un pediatra no registrado | RF-PED-09 |
| `DELETE /link-requests/{id}` | Cancelar solicitud | RF-PED-06 |
| `GET /me/link-requests` | Solicitudes recibidas (pediatra) | RF-PED-05 |
| `POST /link-requests/{id}/accept` · `POST /link-requests/{id}/reject` | Responder solicitud | RF-PED-05, 08 |
| `DELETE /children/{id}/care-link` | Revocar el vínculo activo | RF-PED-07 |
| `GET /children/{id}/care-links` | Pediatra actual, pendientes e historial | RF-PED-15 |
| `GET /patients?filter=alerts&query=` | Lista de pacientes con resumen | RF-PED-10 |
| `GET /patients/former` | Pacientes anteriores (solo sus consultas) | RF-PED-10, CA5 |
| `POST /patients` · `DELETE /patients/{id}` | Alta y baja de paciente propio | RF-PED-11, RN-PED-08 |
| `POST /patients/{id}/claim-invitations` | Invitar al tutor a reclamar el perfil | RF-PED-12 |
| `GET /invitations/{id}/merge-preview?targetChildId=` | Resumen de la unificación | RF-PED-13 |
| `POST /invitations/{id}/claim` | Aceptar reclamo: `{ "mode": "NEW" }` o `{ "mode": "MERGE", "targetChildId": … }` | RF-PED-12, 13 |

### Consultas

| Método y ruta | Descripción | RF |
|---|---|---|
| `POST /children/{id}/consultations` | Crear borrador (o devolver el existente) | RF-CON-01, 09 |
| `GET /consultations/{id}` | Detalle según quién pide (autor, tutor u otro pediatra) | RF-CON-12 |
| `PATCH /consultations/{id}` | Guardar datos de pasos del borrador y paso actual | RF-CON-02 a 09 |
| `DELETE /consultations/{id}` | Descartar borrador | RF-CON-09 |
| `POST /consultations/{id}/close` | Cerrar e incorporar datos al historial | RF-CON-10 |
| `POST /consultations/{id}/addenda` | Nota aclaratoria | RF-CON-11 |
| `GET /children/{id}/consultations` | Lista con la visibilidad de cada consulta | RF-CON-12 |

### Eventos, hitos, hábitos y consejos

| Método y ruta | Descripción | RF |
|---|---|---|
| `GET /milestones` | Catálogo de hitos | RF-EVT-05 |
| `GET /children/{id}/habits/summary?days=7` | Resumen de hábitos (calculado en el servidor para el pediatra) | RF-HAB-05 |
| `GET /advice?updatedSince=` | Consejos publicados (la app filtra por edad) | RF-CJO-01 a 03 |

Eventos, hitos logrados y hábitos se escriben por `/sync/push`. La línea de tiempo se arma en la
app con los datos sincronizados (ADR-24).

### Informes

| Método y ruta | Descripción | RF |
|---|---|---|
| `GET /children/{id}/growth-report?from=&to=` | PDF del informe de crecimiento (`application/pdf`) | RF-INF-01, 02 |
| `GET /children/{id}/consultation-record?from=&to=` | PDF del registro de controles del pediatra que lo pide | RF-INF-03 |

### Notificaciones

| Método y ruta | Descripción | RF |
|---|---|---|
| `GET /notifications/{id}` | Contenido de una notificación (lo pide la app al recibir el aviso FCM) | RF-NOT-01 |
| `GET /me/notifications?cursor=` | Centro de notificaciones | RF-NOT-01 |
| `POST /me/notifications/{id}/read` | Marcar como leída | RF-NOT-01 |

### Sincronización

| Método y ruta | Descripción |
|---|---|
| `GET /sync/changes?cursor=&patients=` | Cambios desde el cursor, en el alcance del usuario |
| `GET /sync/snapshot/{childId}` | Foto completa de un niño que acaba de entrar al alcance |
| `POST /sync/push` | Lote de cambios locales, idempotente |

Detalle en [14 · Sincronización](14-sincronizacion.md).

### Sistema

| Método y ruta | Descripción |
|---|---|
| `GET /health` | Estado del servicio y de la conexión a la base (sin autenticación) |
| `GET /api/v1/openapi.yaml` | Especificación OpenAPI |
