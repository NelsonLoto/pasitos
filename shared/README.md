# shared

Biblioteca Kotlin/JVM pura que usan la app y el backend
([10 · Módulo compartido](../docs/spec-tecnica/10-modulo-compartido.md), ADR-01). Así un cálculo
hecho sin conexión en el teléfono da lo mismo que en el servidor, y los contratos de la API son
las mismas clases en los dos lados.

## Paquetes

| Paquete | Contenido |
|---|---|
| `com.pasitos.shared.time` | Formato de edades (`formatAge`, RN-HIJ-04 y 05). Próximo control (RN-NOT-02) |
| `com.pasitos.shared.api` | DTOs de la API y errores (`ErrorResponse`, `ErrorCode`) |
| `com.pasitos.shared.growth` | Motor de crecimiento (I2) |
| `com.pasitos.shared.vaccination` | Estados de las dosis (I1) |

## Reglas

- Sin dependencias de Android, Ktor, Exposed ni de ninguna librería de plataforma. Solo
  `java.time` y kotlinx.serialization.
- Código puro: recibe la fecha de "hoy" como parámetro, nunca lee el reloj, la red ni la base.
- Compila a bytecode de Java 17.

## Cómo se usa

La app y el backend lo incluyen como **build compuesto**: `includeBuild("../shared")` en su
`settings.gradle.kts` y `implementation("com.pasitos:shared")` en su `build.gradle.kts`. Un
cambio en `shared` se ve en la app y en el backend al recompilar, sin publicar nada.

## Pruebas

```
./gradlew test
```
