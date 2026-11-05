# app

Proyecto Android nativo (Kotlin + Jetpack Compose) de la app "Niño Sano".

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

## Autenticación (mock)

El backend (`../backend/`) todavía no existe. El login actual usa
`FakeAuthRepository` (in-memory, con latencia simulada). Credenciales de
prueba:

- **Email:** `demo@ninosano.app`
- **Contraseña:** `123456`

Cuando exista el backend real, se agrega `RemoteAuthRepository : AuthRepository`
y solo cambia el binding en `AuthModule` — ni la UI ni los ViewModels que
dependen de `AuthRepository` se tocan.

## Estado del scaffold

- Design System de Figma portado a código (`core/designsystem`): colores,
  tipografía (Baloo 2 + Inter) y componentes reutilizables.
- Navegación completa de las 15 pantallas del documento funcional
  (`core/navigation`): solo **Login** está implementada a fondo; el resto
  son placeholders navegables (`PlaceholderScreen`) a completar pantalla
  por pantalla, en el orden del documento funcional.
- Sin persistencia real ni backend: toda la capa de datos es mock/local.
