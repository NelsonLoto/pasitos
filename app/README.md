# app

Proyecto Android nativo (Kotlin + Jetpack Compose) de la app Pasitos.

## Stack y versiones

| Componente | Versión |
|---|---|
| Kotlin | 2.2.10 |
| AGP | 9.3.2 |
| Gradle | 9.5.0 |
| Compose BOM | 2026.02.01 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 (Android 8.0) |
| DI | Hilt 2.59.2 + KSP 2.2.10-2.0.2 |
| Navegación | Navigation Compose 2.9.8 (rutas type-safe) |
| Base local | Room 2.8.5 + SQLCipher 4.19.1, clave protegida con Tink 1.23.0 y el Android Keystore |
| Sincronización | WorkManager 2.12.0 + Hilt Work 1.4.0 |
| Preferencias y secretos | DataStore 1.2.1 |

## Compilar y correr

Abrir esta carpeta (`app/`) como proyecto en Android Studio, o por terminal:

```bash
./gradlew assembleDebug
```

Si `java` no está en el PATH, usar el JDK embebido de Android Studio:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

> **Nota:** si el proyecto vive dentro de una carpeta sincronizada por OneDrive,
> Gradle puede fallar de forma intermitente al limpiar `build/` con errores
> tipo `AccessDeniedException` / "not a regular file". Es un lock transitorio
> de OneDrive, no un error del proyecto — reintentar el build (o excluir
> `app/build/` y `app/app/build/` de la sincronización) lo resuelve.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

Corren en la JVM, también en la integración continua. Las de Room y WorkManager usan
Robolectric, que la primera vez descarga una imagen de Android (unos minutos).

```bash
./gradlew connectedDebugAndroidTest
```

Necesitan un emulador o un teléfono conectado. Prueban el cifrado de la base con SQLCipher y el
Android Keystore reales, que no existen en la JVM. Al terminar, Gradle desinstala la app.

## Base local y sincronización

- `core/database`: base Room cifrada con SQLCipher, una por proceso. Hilt inyecta los DAOs; cada
  dominio provee los suyos en `data/x/di/`. Al cerrar sesión se borran la base y su clave, y la
  app se reinicia.
- `core/security`: clave de la base cifrada con Tink y el Android Keystore.
- `core/time`: `ArgentinaClock.today()` para "hoy". No usar `LocalDate.now()`.
- `core/sync`: `SyncWorker` con WorkManager. Corre al abrir la app o volver a ella, después de
  cada escritura local y cada 15 minutos con red. En I0 solo registra la ejecución en Logcat
  (etiqueta `SyncWorker`).

Detalle en [15 · App Android](../docs/spec-tecnica/15-android.md#3-base-local-cifrada-adr-17) y
[14 · Sincronización](../docs/spec-tecnica/14-sincronizacion.md#6-en-la-app).

## Autenticación

Login y registro son reales contra el backend (`../backend/`, ver su propio
README para levantarlo) — `AuthApiRepository` llama a `POST /auth/login` y
`POST /auth/register`. Hace falta registrar una cuenta nueva primero (ya no
hay credenciales de demo hardcodeadas). `loginWithGoogle()` sigue siendo un
mock (Google Sign-In real es Etapa 4).

La app apunta a `http://localhost:8080` (`core/network/NetworkConfig.kt`). Para que
funcione (emulador o dispositivo físico, da igual) hay que correr una vez por sesión
de depuración, antes de instalar/abrir la app:

```bash
adb reverse tcp:8080 tcp:8080
```

Esto túnel-ea el puerto a través del cable/canal de `adb` en vez de depender de red
(evita el alias `10.0.2.2` del emulador, que en esta máquina chocaba con el firewall
de Windows).

## Estado del scaffold

- Design System de Figma portado a código (`core/designsystem`): colores,
  tipografía (Baloo 2 + Inter) y componentes reutilizables.
- Navegación completa de las 15 pantallas del documento funcional
  (`core/navigation`): solo **Login** está implementada a fondo; el resto
  son placeholders navegables (`PlaceholderScreen`) a completar pantalla
  por pantalla, en el orden del documento funcional.
- Cuentas, hijos y mediciones van contra el backend; el resto de la capa de datos sigue siendo
  mock. Desde I1 los repositorios leen de la base local en lugar de la red (ADR-02).
