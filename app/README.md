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

Para ver el esqueleto de sincronización, iniciar sesión contra el backend y filtrar
Logcat por `PasitosSync`. Se programa al iniciar sesión y al volver a la app, más un trabajo
periódico con red cada 15 minutos (Android puede diferirlo por ahorro de energía).
El disparador manual se expone como `SyncScheduler.requestSync(MANUAL)`. En I0-07 el trabajo solo registra que corrió: todavía no
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

## Sesión contra el backend (I0-06)

El email y la contraseña se validan en el backend; las antiguas credenciales del
repositorio falso ya no permiten entrar. Crear una cuenta ficticia desde el registro
(nombre y apellido, contraseña de al menos 8 caracteres con letra y número y aceptación
explícita de términos). En I0 no se exige verificar el email. Google sigue simulado
hasta I1-A2 y no otorga acceso al backend.

Iniciar el backend con su PostgreSQL local (ver [`backend/README.md`](../backend/README.md))
y conectar el emulador o teléfono:

```sh
adb reverse tcp:8080 tcp:8080
```

Debug usa `http://localhost:8080/`; HTTP solo se permite para ese nombre y en esa
variante. Staging usa HTTPS, configurado con `-PSTAGING_API_URL=https://…/` o la propiedad
en `~/.gradle/gradle.properties`, y muestra el aviso de datos ficticios. Su URL por
defecto es un dominio reservado `.invalid`: hay que configurarla antes de distribuir.
La firma de staging se agrega en I1-I2; release no se distribuye durante la tesis.

`SessionRepository` restaura desde el DataStore `secrets` sin necesitar red. La cuenta
y ambos tokens se guardan juntos, cifrados con Tink/Keystore y un dato asociado propio;
las pantallas solo reciben la cuenta. Mientras se restaura se muestra un indicador,
sin login ni datos de una cuenta anterior. Ktor Client (OkHttp) renueva ante 401,
serializa las renovaciones y persiste el token rotado aun si se cancela el pedido.
Login y registro no envían tokens ni se reintentan ante una contraseña incorrecta.
Los permisos no se envían a otro origen y el cliente no sigue redirects.

Si la renovación devuelve 401, se vuelve al login con «Tu sesión venció», conservando
la cuenta y la base con los cambios pendientes. Entrar con la misma cuenta los conserva;
entrar con otra borra la base y reinicia el proceso antes de mostrarla. Una falla de red
o 5xx no cierra la sesión. La sincronización solo se programa con sesión iniciada.

En Perfil, «Cerrar sesión» pide confirmación y advierte si hay cambios pendientes.
Revoca la sesión, cancela los trabajos, borra tokens/base/clave y reinicia mediante un
proceso auxiliar privado, para retirar también los datos de memoria. Sin conexión
cierra igual en el teléfono y conserva cifrado el token de revocación para el trabajo
`revoke-sessions`, que termina el cierre al recuperar red. Las distintas revocaciones
pendientes se procesan por separado y no se borran al eliminar la sesión local.

### Pruebas de sesión

Las pruebas unitarias verifican restauración, renovación concurrente y cancelación,
401 frente a errores transitorios, cambio de cuenta y limpieza offline. Las pruebas
de `SessionStoreTest` requieren el Keystore real del emulador. `RealSessionTest`
requiere además un backend temporal con cuentas y base ficticias y `adb reverse`;
firma un JWT ya vencido para probar renovación real, sin esperar 15 minutos.
Ese backend de prueba debe usar esta clave **ficticia**, nunca la de un entorno real:

```powershell
$env:JWT_SECRET = 'synthetic-app160-backend-signing-key-'.PadRight(96, '0')
```

La prueba crea su propia cuenta y su almacenamiento cifrado aislado; no cambia el
reloj ni la vigencia de los tokens del servidor. Ejecutar `connectedDebugAndroidTest`
con el backend temporal en 8080. El recorrido visual de compatibilidad usa Google
simulado; la autenticación real se comprueba por separado contra el servidor.

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
