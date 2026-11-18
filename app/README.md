# app

App Android de Pasitos (Kotlin + Jetpack Compose), paquete `com.pasitos.app`. Las
especificaciones están en la carpeta de Google Drive
[Specs y plan de trabajo](https://drive.google.com/drive/folders/1uXTYhwSPfYaTCs9BrH1HyrU7GV_3y0Y7).

## Versiones

| Componente | Versión |
|---|---|
| Kotlin | 2.4.21 |
| AGP | 9.4.1 |
| Gradle | 9.8.2 |
| JDK del daemon de Gradle | 21 (seleccionado por `gradle/gradle-daemon-jvm.properties`) |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 (Android 8.0) |
| DI | Hilt 2.60.1 + KSP 2.3.13 |
| Navegación | Navigation Compose 2.10.2 (rutas type-safe) |
| Base local | Room 2.8.5 + SQLCipher 4.19.1 |
| Clave de la base | Tink 1.23.0 + DataStore 1.2.1 |
| Trabajos en segundo plano | WorkManager 2.12.0 + Hilt Work 1.4.0 |

El wrapper verifica la distribución de Gradle con `distributionSha256Sum`. Si Android Studio
propone bajar AGP o Gradle, actualizar Android Studio y conservar ese checksum.

## Compilar y correr

Abrir esta carpeta (`app/`) como proyecto en Android Studio y correr la configuración `app`, o
por terminal desde esta carpeta:

```bash
./gradlew assembleDebug testDebugUnitTest   # compila y corre las pruebas unitarias
./gradlew installDebug                      # instala en el emulador o teléfono conectado
./gradlew connectedDebugAndroidTest         # verifica el flujo con Hilt en el emulador
```

Las pruebas locales de los DAOs usan Room en memoria con Robolectric (API 37). Las pruebas
de SQLCipher y Keystore necesitan un emulador o dispositivo real: crean archivos, claves y
preferencias de prueba aislados, comprueban el rechazo de SQLite sin clave y la limpieza.
No se omiten estas pruebas reemplazando el cifrado por una base en memoria.

Para ver el esqueleto de sincronización, iniciar sesión con la cuenta de prueba y filtrar
Logcat por `PasitosSync`. Se programa al iniciar sesión y al volver a la app, más un trabajo
periódico con red cada 15 minutos (Android puede diferirlo por ahorro de energía).
El disparador manual se expone como `SyncScheduler.requestSync(MANUAL)`; la pantalla de
Perfil se conectará en su tarea. En I0-07 el trabajo solo registra que corrió: todavía no
envía datos ni adelanta el cursor. Sin sesión, los trabajos se cancelan.

En Windows (PowerShell), si `java` no está en el PATH, usar el JDK embebido de Android Studio:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug testDebugUnitTest
```

> **Nota:** si el proyecto vive dentro de una carpeta sincronizada por OneDrive,
> Gradle puede fallar de forma intermitente al limpiar `build/` con errores
> tipo `AccessDeniedException` / "not a regular file". Es un lock transitorio
> de OneDrive, no un error del proyecto — reintentar el build (o excluir
> `app/build/` y `app/app/build/` de la sincronización) lo resuelve.

## Credenciales de prueba

Mientras la app use `FakeAuthRepository` (en memoria), el login acepta:

- **Email:** `demo@pasitos.app`
- **Contraseña:** `123456`

## Estructura de carpetas

```
app/src/main/java/com/pasitos/app/
├── core/
│   ├── common/          componentes generales (PlaceholderScreen)
│   ├── designsystem/    componentes del Design System de Figma (prefijo Ns)
│   ├── navigation/      grafo de navegación y rutas type-safe
│   ├── database/        base cifrada, conversores y DAOs de soporte
│   ├── security/        cifrado local y clave protegida en DataStore
│   ├── sync/            disparadores y trabajo inicial de sincronización
│   └── time/            fecha actual de Argentina
├── data/                un paquete por dominio (auth, child, measurement):
│                        repositorio, modelo (model/) y módulo de Hilt (di/)
├── feature/             un paquete por pantalla: Screen, UiState y ViewModel
└── ui/theme/            colores, tipografía y tema
```
