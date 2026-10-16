# 14 · Sincronización

Implementa RNF-OFF-01 a 06 con un protocolo simple de **traer cambios** y **enviar cambios**
(ADR-15). El mecanismo se construye en I0 e I1 porque todos los repositorios de la app dependen
de él; en I5 se completan la experiencia sin conexión (indicadores, errores) y sus pruebas.

## 1. Conceptos

| Concepto | Definición |
|---|---|
| **Entidad sincronizable** | Tabla cuyos cambios viajan al teléfono: `children`, `guardianships`, `care_links`, `measurements`, `applied_doses`, `other_vaccines`, `consultations` (sin `private_notes` de otros), `events`, `milestone_achievements`, `habits` |
| **Versión de sincronización** | Número global creciente que el servidor asigna a cada fila en cada cambio (`sync_version`, [13](13-base-de-datos.md#versión-de-sincronización)) |
| **Cursor** | La mayor versión que el teléfono ya recibió |
| **Lápida** | Fila marcada con `deleted_at`: viaja como borrado y el teléfono elimina su copia |
| **Alcance** | Qué niños sincroniza cada usuario (sección 4) |
| **Bandeja de salida** | Tabla local `pending_mutations` con los cambios hechos en el teléfono que todavía no confirmó el servidor |

## 2. Traer cambios

```
GET /api/v1/sync/changes?cursor=18342&patients=<uuid>,<uuid>
```

```json
{
  "cursor": 18517,
  "hasMore": false,
  "changes": {
    "children": [ … ],
    "measurements": [ { "id": "…", "childId": "…", "measuredOn": "2026-10-10", "weightKg": 7.25,
                        "authorAccountId": "…", "authorRole": "GUARDIAN", "deleted": false } ],
    "habits": [ … ]
  },
  "newChildIds": [ "…" ],
  "revokedChildIds": [ "…" ]
}
```

- Devuelve como máximo 500 filas por respuesta; si `hasMore` es `true`, la app vuelve a pedir con
  el nuevo cursor.
- **`newChildIds`**: niños que entraron al alcance desde el último cursor (por ejemplo, un vínculo
  aceptado o una invitación de tutor aceptada). Sus filas antiguas tienen versiones menores al
  cursor, así que la app pide una **foto completa** con `GET /sync/snapshot/{childId}` y la guarda.
- **`revokedChildIds`**: niños a los que el usuario perdió acceso (tabla `access_revocations`). La
  app borra todos sus datos locales y descarta sus cambios pendientes (RNF-OFF-05).

## 3. Enviar cambios

```
POST /api/v1/sync/push
```

```json
{
  "mutations": [
    { "mutationId": "…", "entity": "MEASUREMENT", "op": "UPSERT",
      "data": { "id": "…", "childId": "…", "measuredOn": "2026-10-14", "weightKg": 7.31, … } },
    { "mutationId": "…", "entity": "HABIT", "op": "DELETE", "data": { "id": "…" } }
  ]
}
```

```json
{
  "results": [
    { "mutationId": "…", "status": "APPLIED", "syncVersion": 18520 },
    { "mutationId": "…", "status": "REJECTED", "code": "business_rule", "rule": "RN-CRE-12",
      "message": "Ya cargaste una medición en esa fecha" }
  ]
}
```

| Regla | Detalle |
|---|---|
| Entidades admitidas | `MEASUREMENT`, `HABIT`, `EVENT`, `MILESTONE_ACHIEVEMENT` (ADR-28) |
| Idempotencia | `UPSERT` por `id` (UUID generado en el teléfono): reenviar el mismo lote produce el mismo resultado. `DELETE` de algo ya borrado es `APPLIED` |
| Validación | El servidor aplica las mismas reglas que en línea: permisos, autoría, rangos, reglas de negocio |
| Orden | Las mutaciones se aplican en el orden recibido, cada una en su propia transacción |
| Conflictos | Solo el autor edita un registro, así que solo pueden chocar dos teléfonos del mismo usuario: gana el último que llega al servidor |
| Rechazos | La app marca el registro local con el error y lo muestra para que el usuario lo corrija o lo descarte |
| Pediatra | Usa el mismo endpoint, pero la app solo lo permite con conexión (RNF-OFF-02 es solo para tutores) |

## 4. Alcance por rol (ADR-16)

| Rol | Se sincroniza | Se consulta en línea |
|---|---|---|
| Tutor | Todos sus hijos, con todas las entidades sincronizables | Informes, resumen de hábitos del servidor |
| Pediatra | Lista de pacientes con resumen (`GET /patients`, guardada para lectura sin conexión) y el detalle completo de los **pacientes abiertos recientemente** (hasta 50, el más antiguo sale primero), que se envían en `patients` | Hábitos: solo el resumen (`GET /children/{id}/habits/summary`), porque el detalle puede ser de miles de filas |
| Ambos perfiles en la misma cuenta | La unión de ambos alcances, separados por modo en la interfaz | — |

Las observaciones privadas solo viajan al dispositivo de su autor.

## 5. Datos de referencia y catálogos

| Datos | Cómo se actualizan |
|---|---|
| Tablas, umbrales y parámetros del motor de crecimiento | Al abrir la app y una vez por día: `GET /reference-data/version`; si cambió, `GET /reference-data` y se reemplaza en Room |
| Calendario, campañas, hitos, consejos, cronograma | Mismos momentos, con `ETag` / `If-None-Match` para no descargar si no cambiaron |

Sin la primera descarga de datos de referencia no se pueden calcular percentiles: la app la hace
durante el inicio de sesión y, si falla, la reintenta antes de mostrar resultados.

## 6. En la app

```mermaid
sequenceDiagram
    participant UI as Pantalla
    participant R as Repositorio
    participant DB as Room
    participant W as SyncWorker
    participant API as Backend

    UI->>R: guardar medición
    R->>DB: insertar fila (PENDING) + mutación en pending_mutations
    DB-->>UI: Flow emite el cambio (se ve al instante)
    R->>W: encolar sincronización inmediata
    W->>API: POST /sync/push
    API-->>W: APPLIED (syncVersion)
    W->>DB: marcar SYNCED, borrar mutación
    W->>API: GET /sync/changes?cursor=…
    API-->>W: cambios de otros usuarios
    W->>DB: aplicar cambios y nuevo cursor
```

| Disparador | Trabajo |
|---|---|
| Después de cada escritura local | Sincronización inmediata (`OneTimeWorkRequest` con restricción de red, acelerado desde Android 12) |
| Periódico | Cada 15 minutos con red disponible (mínimo de WorkManager) |
| Al abrir la app o volver a ella | Traer cambios |
| "Sincronizar ahora" | Inmediato (RNF-OFF-03) |
| Aviso FCM de notificación | Traer cambios después de mostrarla |

- `SyncScheduler` (`core/sync`) encola los trabajos con nombre único: `sync-now` para los
  inmediatos y `sync-periodic` para el periódico. Un pedido inmediato nuevo reemplaza al que
  seguía esperando; es seguro porque reenviar cambios no los duplica.
- Antes de Android 12, un trabajo acelerado corre como servicio en primer plano y obliga a mostrar
  una notificación. Por eso ahí el inmediato se encola como trabajo normal, que con la app abierta
  igual corre en segundos.
- La app abre o vuelve al frente: `ProcessLifecycleOwner` pide una sincronización inmediata.
- Sin sesión no se sincroniza: la periódica se programa al iniciar o restaurar la sesión y todo se
  cancela al vencer o cerrarla ([15](15-android.md#4-sesión)).
- La inmediata y la periódica son trabajos distintos y pueden coincidir. Si dos sincronizaciones
  se pisan, un registro puede volver a una versión vieja. El motor de sincronización
  (I0-08 e I1) cumple dos reglas:
  - **Una sola a la vez:** usa un `Mutex`; la que llega segunda espera a que termine la primera.
  - **Nunca pisar con algo más viejo:** al aplicar un cambio del servidor, se ignora si la fila
    local ya tiene una `sync_version` mayor.

- Cada fila local tiene `syncState`: `SYNCED`, `PENDING` o `ERROR` (RNF-OFF-04).
- La app guarda la fecha de la última sincronización exitosa y la muestra en Perfil.
- Al cerrar sesión se borran la base local y su clave (RF-CTA-07). Si hay mutaciones pendientes,
  primero se advierte (RF-CTA-07, CA2).
