# Backend de Pasitos

Servidor Kotlin/JVM con Ktor Netty y PostgreSQL 16. Abrir `backend/` en IntelliJ IDEA.

La especificación oficial está en
[Google Drive](https://drive.google.com/drive/folders/1uXTYhwSPfYaTCs9BrH1HyrU7GV_3y0Y7),
especialmente 01 · Stack y 11 · Backend.

## Requisitos

- JDK 21 y PostgreSQL 16.
- Docker con su motor iniciado para las pruebas de integración con Testcontainers.
- Gradle 9.8.0 se descarga con el wrapper; no hace falta instalarlo.

## Ejecutar en local

Desde `backend/`, copiar `.env.example` como `.env` y completar los datos de una base
PostgreSQL local existente. En PowerShell: `Copy-Item .env.example .env`.
Elegir una contraseña propia y conservar `APP_ENV=local`. Las variables del proceso
tienen prioridad sobre `.env`; en `ci` y `staging` no se carga el archivo.

```sh
./gradlew run
curl http://localhost:8080/health
```

En PowerShell, usar `.\gradlew.bat run` e `Invoke-RestMethod http://localhost:8080/health`.
La respuesta sana es `{"status":"ok","database":"connected"}` con HTTP 200.
Si la base deja de responder, `/health` devuelve HTTP 503. Al iniciar, una conexión o
migración inválida impide levantar el servidor.

`HOST` y `PORT` controlan la escucha HTTP (por defecto `0.0.0.0:8080`).
`DB_HOST`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` son obligatorias; `DB_PORT` vale 5432
por defecto. `APP_ENV` admite solo `local`, `ci` o `staging`.
`JWT_SECRET` es obligatorio: usar una clave aleatoria de al menos 64 caracteres,
guardada fuera del repositorio y estable entre reinicios. Cambiarla invalida los
permisos de acceso emitidos con la clave anterior. `TERMS_VERSION` vale `1` por defecto.
Los servicios de tareas posteriores tienen sus variables indicadas como comentarios en
`.env.example`; todavía no se usan ni se solicitan credenciales para ellos.

## Sesiones (I0-05, #159)

Todas las rutas usan JSON bajo `/api/v1`:

| Método y ruta | Resultado |
| --- | --- |
| `POST /auth/register` | Crea cuenta tutora, registra aceptación de términos y abre sesión (201). |
| `POST /auth/login` | Valida email y contraseña y abre una sesión independiente. |
| `POST /auth/refresh` | Renueva el acceso y reemplaza el token de renovación. |
| `POST /auth/logout` | Cierra la sesión autenticada (204). |
| `GET /me` | Devuelve la cuenta de la sesión autenticada. |

Los contratos están en `shared/api/auth`. El registro exige nombre, apellido, email,
contraseña de al menos 8 caracteres con letra y número, `acceptTerms=true` y la
versión vigente. El email se normaliza; las contraseñas se guardan con bcrypt, costo
12. Para contraseñas de más de 72 bytes se usa la estrategia SHA-512 de la biblioteca
bcrypt; no se descarta el final de la contraseña. En I0 no se exige verificar el email;
verificación, Google y registro profesional corresponden a tareas posteriores.

El acceso usa JWT HS256 de 15 minutos con `sub`, `sid` y `jti`, sin datos personales.
Las rutas protegidas exigen `Authorization: Bearer <accessToken>` y comprueban que la
sesión siga activa en PostgreSQL. El token de renovación tiene 256 bits aleatorios;
solo se guarda su hash SHA-256. La sesión vence tras 30 días sin uso: tanto renovar
como usar una ruta autenticada extienden ese plazo. Reutilizar el token inmediatamente
anterior revoca toda esa sesión. Uno más antiguo se rechaza sin revocar la actual.
Cerrar sesión invalida tanto el acceso como la renovación de esa sesión.

Hay dos límites independientes:

- Cinco contraseñas incorrectas consecutivas bloquean la cuenta durante 15 minutos.
  Una contraseña correcta reinicia el contador cuando la cuenta no está bloqueada.
- `AUTH_RATE_LIMIT` vale **10 pedidos por minuto por IP de conexión**, compartidos
  entre las rutas `/auth/*`, sean correctos o incorrectos. Es el valor inicial acordado
  el 22/11/2026 para #159; Drive exige limitar pedidos, pero no define esa cifra.
  No se confía en `X-Forwarded-For` enviado por clientes. Al desplegar detrás de un
  proxy habrá que configurar explícitamente el origen confiable para conservar el
  límite por cliente. `GET /me` permite 120 pedidos por minuto por cuenta.

El exceso de pedidos devuelve 429 con `Retry-After`; una cuenta bloqueada devuelve
423. Los errores usan `ErrorResponse` compartido. Los registros no incluyen cuerpos,
contraseñas, tokens ni datos personales. Este cambio no conecta todavía la pantalla
Android de inicio de sesión.

## Pruebas

```sh
./gradlew test
```

En PowerShell: `.\gradlew.bat test`. Testcontainers crea un PostgreSQL 16 temporal, aislado
de la base local, y lo elimina al terminar. La suite falla si Docker no está disponible;
no omite las pruebas de integración ni usa una base en memoria.
Los informes quedan en `build/reports/tests/test/index.html`.

## Organización

`Application.kt` construye las dependencias a mano y registra las rutas.
`common/` contiene configuración, conexión, errores y logs; `health/` contiene el estado
del servicio. Los dominios nuevos van en paquetes en inglés con `XRoutes`, `XService`,
`XRepository` y `XTables`, sin reglas de negocio en las rutas ni en los repositorios.

Flyway ejecuta las migraciones de `src/main/resources/db/migration` con `outOfOrder=true`
y limpieza deshabilitada. El esquema de dominio corresponde a #158 (I0-11); #159 usa
sus tablas de cuentas, consentimientos y sesiones sin modificar la migración publicada.
`ErrorResponse` y los contratos de autenticación viven en `shared/api`.
