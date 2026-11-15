# app

App Android de Pasitos (Kotlin + Jetpack Compose), paquete `com.pasitos.app`. Las
especificaciones están en la carpeta de Google Drive
[Specs y plan de trabajo](https://drive.google.com/drive/folders/1uXTYhwSPfYaTCs9BrH1HyrU7GV_3y0Y7).

## Versiones

| Componente | Versión |
|---|---|
| Kotlin | 2.4.20 |
| AGP | 9.4.1 |
| Gradle | 9.8.0 |
| JDK del daemon de Gradle | 21 (seleccionado por `gradle/gradle-daemon-jvm.properties`) |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 (Android 8.0) |
| DI | Hilt 2.60.1 + KSP 2.3.12 |
| Navegación | Navigation Compose 2.10.2 (rutas type-safe) |

El wrapper verifica la distribución de Gradle con `distributionSha256Sum`. Si Android Studio
propone bajar AGP o Gradle, actualizar Android Studio y conservar ese checksum.

## Compilar y correr

Abrir esta carpeta (`app/`) como proyecto en Android Studio y correr la configuración `app`, o
por terminal desde esta carpeta:

```bash
./gradlew assembleDebug testDebugUnitTest   # compila y corre las pruebas unitarias
./gradlew installDebug                      # instala en el emulador o teléfono conectado
```

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
│   └── navigation/      grafo de navegación y rutas type-safe
├── data/                un paquete por dominio (auth, child, measurement):
│                        repositorio, modelo (model/) y módulo de Hilt (di/)
├── feature/             un paquete por pantalla: Screen, UiState y ViewModel
└── ui/theme/            colores, tipografía y tema
```
