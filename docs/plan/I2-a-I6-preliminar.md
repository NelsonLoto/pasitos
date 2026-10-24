# I2 a I6 · Plan preliminar

Tareas candidatas de cada incremento, con sus dependencias principales. Se detallan con el
formato de [I0](I0-fundaciones.md) e [I1](I1-base-de-tutores.md) en la revisión de cierre del
incremento anterior, cuando ya se conocen los resultados de la validación médica y lo aprendido.

> **⚠️ Antes de tomar una tarea:** verificar que no esté en curso según
> [`CONTRIBUTING.md`](../../CONTRIBUTING.md#antes-de-tomar-una-tarea-personas-y-agentes-de-ia).

Prefijos de las tareas: **S** shared · **B** backend · **A** app · **W** panel web · **D** datos ·
**I** infraestructura · **Q** aceptación.

---

## I2 · Motor de crecimiento (16/11 – 29/11/2026)

**Requerimientos:** RF-CRE-04 a 09, RF-ADM-04 y 05 (carga inicial por importación).
**Requisitos previos:** T-01, T-02 y T-03 de [transversales](transversales.md); T-18 para la demo.

| Tarea | Contenido | Depende de |
|---|---|---|
| I2-D1 | Tablas OMS en CSV con casos de control por indicador, sexo y edad | T-02 |
| I2-D2 | Tablas SAP: unificar la doble transcripción, resolver diferencias y armar casos de control | T-01 |
| I2-S1 | Motor en `shared`: edades, referencia, ajuste de longitud, IMC, LMS, colas restringidas, percentil | I0-02 |
| I2-S2 | Estado, clasificación técnica, cruce de percentiles, plausibilidad y resumen en `shared` | I2-S1, T-03 |
| I2-B1 | Datos de referencia versionados en el backend, importación desde `seed`, `GET /reference-data` y su versión | I2-D1, I2-D2 |
| I2-B2 | `GET /children/{id}/growth` y validación de plausibilidad por Z en `/sync/push` | I2-S2, I2-B1 |
| I2-A1 | Datos de referencia en Room y `GrowthEngine` en la app | I2-B1, I2-S1 |
| I2-A2 | Carga e historial con percentil y estado para tutores, y confirmación de valores atípicos (RF-CRE-01, CA3) | I2-A1, I2-S2 |
| I2-A3 | Curvas: `Canvas`, líneas de la referencia, marca OMS / SAP, marcadores por autor, tramos y vista en tabla | I2-A1 |
| I2-A4 | Inicio con el resumen del estado y "Sin datos recientes" | I2-A2 |
| I2-Q1 | Aceptación y demo | Todas |

**Paralelismo:** los datos (D1, D2) y el motor (S1, S2) avanzan en paralelo desde el primer día;
B1 y A1 se destraban cuando están los datos. La vista del pediatra (percentil exacto, Z y
clasificación técnica, RF-CRE-09) se termina en I3, cuando existe el perfil de pediatra.

---

## I3 · Pediatras (30/11 – 13/12/2026)

**Requerimientos:** RF-PED-01 a 15, RF-ADM-01 a 03, RF-CTA-01 (CA5), RF-CTA-09 (CA2 y CA3),
RF-CRE-09 (vista del pediatra).

| Tarea | Contenido | Depende de |
|---|---|---|
| I3-B1 | Perfil de pediatra, estados y permisos del pediatra en `AccessPolicy` | — |
| I3-W1 | Panel mínimo: acceso de administradores, validación y suspensión de pediatras | I3-B1 |
| I3-B2 | Búsqueda por matrícula y ciclo de vida del vínculo: solicitar, aceptar, rechazar, cancelar, revocar, cambio, vencimiento, revocaciones de sincronización | I3-B1 |
| I3-B3 | Invitación a pediatras no registrados | I3-B2 |
| I3-B4 | Pacientes propios, invitación a reclamar y unificación de historiales | I3-B2 |
| I3-B5 | Lista de pacientes con resumen y filtros; alcance del pediatra en la sincronización (hasta 50 pacientes); carga de datos por el pediatra | I3-B2 |
| I3-A1 | Perfil de pediatra y selector de modo | I3-B1 |
| I3-A2 | Vínculo desde el tutor: buscar, solicitar, invitar, historial y revocar | I3-B2, I3-B3 |
| I3-A3 | Modo pediatra: solicitudes, pacientes y ficha con percentil exacto, Z y clasificación técnica | I3-B5, I3-A1 |
| I3-A4 | Pacientes propios y reclamo de perfil (pediatra y tutor) | I3-B4, I3-A3 |
| I3-Q1 | Aceptación y demo | Todas |

**Paralelismo:** B1 primero (corta); después W1, B2 y A1 en paralelo; luego B3, B4 y B5.

---

## I4 · Consulta guiada (04/01 – 17/01/2027)

**Requerimientos:** RF-CON-01 a 12. **Requisitos previos:** T-04 (pesquisas por edad, PVM-09) y
T-05 (catálogo de hitos, necesario para el paso 5).

| Tarea | Contenido | Depende de |
|---|---|---|
| I4-B1 | Consultas: borrador, guardado por pasos, descartar, cerrar e incorporar datos, notas aclaratorias, DTOs por rol | — |
| I4-B2 | Esquema JSON versionado de los pasos 3 a 8 y su validación | — |
| I4-B3 | Pruebas de privacidad: las observaciones nunca llegan a quien no es su autor | I4-B1 |
| I4-A1 | Navegación de pasos en orden fijo, índice, omitir y borrador | I4-B1 |
| I4-A2 | Pasos 1 a 4, con percentiles en vivo en el paso 2 | I4-A1, I4-B2 |
| I4-A3 | Pasos 5 a 8: hitos, pesquisas por edad, vacunas y examen físico | I4-A1, I4-B2, T-05 |
| I4-A4 | Pasos 9 y 10, cierre y notas aclaratorias | I4-A1 |
| I4-A5 | Vista de consultas para tutores y para otros pediatras | I4-B1 |
| I4-Q1 | Aceptación y demo | Todas |

**Paralelismo:** con A1 mergeada, A2, A3 y A4 son pasos distintos y se reparten entre tres personas.

---

## I5 · Seguimiento y funcionamiento sin conexión (18/01 – 31/01/2027)

**Requerimientos:** RF-EVT-01 a 05, RF-HAB-01 a 06, RF-CJO-01 a 03, RNF-OFF-01 a 06.
**Requisitos previos:** T-05 y T-06.

| Tarea | Contenido | Depende de |
|---|---|---|
| I5-B1 | Eventos manuales e hitos logrados por sincronización; catálogo de hitos desde `seed` | T-05 |
| I5-B2 | Hábitos por sincronización y resumen en el servidor | — |
| I5-B3 | Consejos desde `seed` con `GET /advice` y `ETag` | T-06 |
| I5-A1 | Línea de tiempo derivada (ADR-24) y eventos manuales con íconos | I5-B1 |
| I5-A2 | Desarrollo: hitos por edad con mensaje suave | I5-B1 |
| I5-A3 | Hábitos: registro en 3 toques, diario y resumen | I5-B2 |
| I5-A4 | Consejos y "¿Qué se viene?" | I5-B3 |
| I5-A5 | Experiencia sin conexión: indicadores, errores de sincronización y pruebas de RNF-OFF | — |
| I5-Q1 | Aceptación y demo | Todas |

**Paralelismo:** tres dominios independientes (eventos, hábitos, consejos) más la tarea de
funcionamiento sin conexión: cuatro personas en paralelo desde el inicio.

---

## I6 · Cierre (01/02 – 14/02/2027)

**Requerimientos:** RF-INF-01 a 04, RF-NOT-01 a 03, RF-CJO-04, RF-CON-13, RF-ADM-06 a 12,
RF-CTA-08, RF-CTA-10, RF-HIJ-10, RF-VAC-06. **Requisitos previos:** T-09 a T-14 y T-18.

| Tarea | Contenido | Depende de |
|---|---|---|
| I6-B1 | Informes PDF: curvas en SVG, plantillas e informe del registro de controles con auditoría | — |
| I6-B2 | Notificaciones: generación por evento y diaria, push FCM sin datos, preferencias y dispositivos | T-08 |
| I6-B3 | Tareas programadas: vencimientos, perfiles finalizados, limpiezas | — |
| I6-B4 | Eliminación de cuenta (API y página web) con RN-CTA-08 | — |
| I6-W1 | Panel completo: tablas, umbrales, calendario, campañas, consejos, hitos, cronograma, textos, auditoría y administradores | I3-W1 |
| I6-A1 | Notificaciones en la app: servicio FCM, centro, permiso y preferencias | I6-B2 |
| I6-A2 | Informes en la app: descargar, compartir y guardar | I6-B1 |
| I6-A3 | Bloqueo con huella o PIN y protección de pantalla | — |
| I6-A4 | Eliminación de cuenta, perfil finalizado y campañas en la app | I6-B3, I6-B4 |
| I6-I1 | Despliegue del entorno del piloto: servidor, TLS, copias de seguridad y prueba de restauración | T-11, T-12 |
| I6-I2 | Variante `pilot` de la app con el aviso permanente de datos ficticios y publicación en prueba cerrada de Google Play | T-13, T-14, I6-I1 |
| I0-10 | Registro de errores con Sentry, postergada desde I0 ([ficha](I0-fundaciones.md#i0-10--registro-de-errores-con-sentry)) | T-09 |
| I6-Q1 | Aceptación final y preparación del piloto | Todas |

**Paralelismo:** backend (B1 a B4), panel (W1) e infraestructura (I1) son independientes entre sí;
las tareas de app siguen a su backend.
