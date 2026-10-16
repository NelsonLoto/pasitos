# 15 · App Android

## 1. Capas

Se mantiene la estructura actual (`core`, `data`, `feature`, `ui`), con un cambio central: los
repositorios leen de **Room** y no de la red (ADR-02).

```mermaid
flowchart LR
    screen[Pantalla Compose] --> vm[ViewModel]
    vm --> repo[Repositorio]
    repo -->|Flow| dao[DAO Room]
    repo -->|escrituras sincronizables| outbox[pending_mutations]
    repo -->|operaciones en línea| api[Cliente API]
    worker[SyncWorker] --> outbox
    worker --> api
    worker --> dao
    vm --> engine[GrowthEngine de shared]
```

| Capa | Responsabilidad | Regla |
|---|---|---|
| Pantalla (`feature/x/XScreen.kt`) | Dibujar el estado y enviar eventos | Sin lógica; recibe un `UiState` inmutable |
| ViewModel (`feature/x/XViewModel.kt`) | Combinar flujos de repositorios en un `StateFlow<UiState>` y ejecutar acciones | No conoce Room ni Ktor |
| Repositorio (`data/x/XRepository.kt`) | Única puerta a los datos del dominio | Lee de Room; escribe en Room y bandeja de salida (sincronizables) o llama a la API (en línea) |
| Local (`data/x/local/`) | Entidades y DAOs de Room | — |
| Remoto (`data/x/remote/`) | Llamadas a la API con DTOs de `shared` | — |

Las interfaces de repositorio actuales (`AuthRepository`, `ChildRepository`,
`MeasurementRepository`, …) se conservan; cambian sus implementaciones. Los ViewModels no deberían
necesitar cambios grandes.

## 2. Guía: agregar una pantalla con datos

1. Entidad y DAO en `data/x/local/` y registro en `PasitosDatabase`, subiendo su versión. **Hasta
   el piloto**, la base local se recrea al cambiar de versión (`fallbackToDestructiveMigration`):
   es una copia de datos del servidor y se vuelve a descargar, así que dos tareas en paralelo no
   necesitan coordinar migraciones. **Desde el piloto**, cada cambio lleva su migración de Room
   para no perder cambios pendientes de sincronizar.
2. DTOs en `shared/api` y llamadas en `data/x/remote/`.
3. Implementación del repositorio en `data/x/`. En `data/x/di/` van su `@Binds` y el `@Provides`
   de su DAO a partir de `PasitosDatabase`; el repositorio recibe el DAO inyectado. Después de
   cada escritura sincronizable llama a `SyncScheduler.requestSync(LOCAL_CHANGE)`.
4. `UiState`, ViewModel y pantalla en `feature/x/`. Para "hoy" se usa `ArgentinaClock.today()`,
   nunca `LocalDate.now()`, que depende de la zona horaria del teléfono (ADR-26).
5. Ruta en `core/navigation`.
6. Pruebas del ViewModel con un repositorio falso.
7. **Ejecutar la app** en emulador o dispositivo antes de abrir el PR (lección del proyecto: Hilt
   compila pero falla en ejecución si falta `@AndroidEntryPoint`).

## 3. Base local cifrada (ADR-17)

- Room con SQLCipher (`SupportOpenHelperFactory`), en `core/database`.
- La clave de la base son 32 bytes aleatorios generados en el primer uso, cifrados con una clave
  AES del **Android Keystore** mediante Tink y guardados en DataStore (`core/security`):
  - `LocalCipher` cifra con AES-256-GCM. Tink guarda su keyset en las preferencias
    `pasitos_keyset`, cifrado con la clave maestra del Keystore, que no se puede extraer del
    teléfono. Cada valor cifrado lleva un dato asociado según su uso, para que la clave de la base
    no se pueda descifrar como si fuera un token ni al revés.
  - La clave cifrada vive en el DataStore `secrets`, que en I0-06 también guarda los tokens.
  - SQLCipher recibe la clave como clave cruda (`x'…'`): ya es aleatoria, así que no hace falta
    derivarla con PBKDF2 en cada apertura.
  - La clave se lee recién cuando Room abre el archivo, que siempre ocurre fuera del hilo
    principal.
- Hay una sola `PasitosDatabase` por proceso, creada por `LocalDatabase`. Hilt la inyecta, y
  también sus DAOs.
- **Cierre de sesión (RF-CTA-07):** no debe quedar nada de la sesión anterior, ni en el teléfono
  ni en memoria. Lo implementa I0-06, en este orden:
  1. Si hay cambios pendientes, advertirlo y pedir confirmación (CA2).
  2. Revocar la sesión en el servidor.
  3. Cancelar los trabajos de sincronización.
  4. Borrar los tokens y llamar a `LocalDatabase.wipe()`, que cierra la base y borra el archivo
     y su clave (CA1).
  5. Reiniciar el proceso de la app. Así desaparece todo lo que estaba en memoria: tokens en
     caché, datos en repositorios y pantallas, la clave de la base en claro. Ningún componente
     tiene que acordarse de limpiarse.
- **Si la clave no se puede leer**, el archivo tampoco se puede abrir. Entonces se borra y se
  empieza con una base vacía; los datos vuelven del servidor. Lo cargado sin conexión y no enviado
  se pierde, porque sin la clave no se puede recuperar. Hay dos casos:
  - El valor guardado no se puede descifrar: se crea una clave nueva.
  - El cifrado mismo dejó de funcionar, por ejemplo porque el sistema invalidó la clave maestra
    del Keystore (pasa en algunos teléfonos después de actualizaciones del sistema): se borra el
    DataStore `secrets` completo, el keyset y la clave maestra, y se regeneran. Los tokens
    también se pierden, así que el usuario vuelve a iniciar sesión.
- **Riesgo aceptado:** si el Keystore no funciona en un teléfono desde el primer uso, Tink guarda
  el keyset sin cifrar dentro del almacenamiento privado de la app. Otras apps no lo pueden leer y
  Android cifra ese almacenamiento, pero falta la protección del Keystore. La app no se bloquea:
  lo registra en el log (y en Sentry desde I0-10) para saber cuántos teléfonos caen en este caso.
- La base, el DataStore `secrets` y el keyset quedan fuera de las copias de seguridad y del
  traspaso a otro teléfono (`backup_rules.xml` y `data_extraction_rules.xml`). Sin la clave del
  Keystore de origen, no se podrían descifrar en otro teléfono.
- Tablas de soporte: `pending_mutations`, `sync_state` (cursor, última sincronización),
  `reference_data`, catálogos y caché de pacientes del pediatra. Las dos primeras existen desde I0.
- Las fechas del calendario se guardan como texto ISO (`2026-10-14`) y los momentos como
  milisegundos en UTC (`Converters`, ADR-26). "Hoy" sale de `ArgentinaClock.today()` (`core/time`).

## 4. Sesión

| Elemento | Implementación |
|---|---|
| Tokens | Acceso y renovación guardados en DataStore, cifrados con Tink y el Android Keystore |
| Renovación automática | El plugin `Auth` de Ktor Client renueva el token de acceso cuando el servidor responde 401 y reintenta el request. Si la renovación falla, se cierra la sesión |
| Estado de sesión | `SessionRepository` expone `StateFlow<Session?>` con cuenta, perfiles y modo activo. Al iniciar la app se restaura desde DataStore (RF-CTA-06) |
| Google | Credential Manager con `GetGoogleIdOption` (client ID web del proyecto). El ID token se envía a `POST /auth/google` |
| Bloqueo con huella o PIN | `BiometricPrompt` con `BIOMETRIC_STRONG` o credencial del dispositivo. Se pide al abrir la app y al volver después de 5 minutos en segundo plano. Con el bloqueo activo se marca la ventana con `FLAG_SECURE` (RNF-SEG-08) |

## 5. Modos tutor y pediatra

- Un único `NavHost` con dos grafos: `tutorGraph` (Inicio, Crecimiento, Vacunas, Consejos,
  Perfil) y `pediatricianGraph` (Pacientes, Solicitudes, Perfil).
- El modo activo se guarda en la sesión y en el servidor (`accounts.last_mode`).
- Cambiar de modo navega al inicio del otro grafo y limpia la pila.

## 6. Notificaciones

1. `PasitosMessagingService` (FCM) recibe un aviso **de datos** con `notificationId`.
2. Encola un trabajo acelerado que llama a `GET /notifications/{id}`.
3. Construye la notificación local con título, cuerpo e intent al `deepLink` (RF-NOT-01, CA3).
4. Desde Android 13 se pide el permiso `POST_NOTIFICATIONS` en un momento con contexto (después
   del alta del primer hijo), no al abrir la app por primera vez.
5. El token FCM se registra con `PUT /me/devices/{token}` al iniciar sesión y cuando cambia.

## 7. Informes PDF

La app pide el PDF al backend, lo guarda en el almacenamiento propio de la app y lo ofrece con
`Intent.ACTION_SEND` mediante un `FileProvider` (RF-INF-04). "Guardar" usa el selector de
documentos del sistema (`ACTION_CREATE_DOCUMENT`), sin pedir permisos de almacenamiento.

## 8. Configuración por entorno

| Variante | URL del backend | Tráfico HTTP sin cifrar |
|---|---|---|
| `debug` | `http://localhost:8080` con `adb reverse tcp:8080 tcp:8080` (práctica actual del proyecto) | Permitido solo para `localhost` (configuración de seguridad de red de `debug`) |
| `pilot` | URL HTTPS del servidor del piloto. Muestra en todas las pantallas el aviso "Versión de prueba: no cargues datos de niños reales" (RNF-PRI-08) | No |
| `release` | URL HTTPS de producción | No |

La URL se define con `buildConfigField` por variante.

## 9. Accesibilidad e interfaz

- Componentes del Design System existente, con `contentDescription` en todo elemento
  interactivo y en los gráficos (RNF-ACC-03).
- Tamaños en `sp` para texto y verificación con escala de fuente del 200 % (RNF-ACC-02).
- Los estados siempre llevan ícono y texto (RNF-ACC-06).
- Curvas dibujadas con `Canvas` de Compose, con vista alternativa en tabla (RF-CRE-06, CA5).
- Formatos de número y fecha con `Locale("es", "AR")` (RNF-LOC-02 y 03).

## 10. Errores

Sentry Android con `sendDefaultPii = false` y un `beforeSend` que elimina breadcrumbs de red con
cuerpos y cualquier texto ingresado por el usuario (ADR-08).
