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
Los servicios de tareas posteriores tienen sus variables indicadas como comentarios en
`.env.example`; todavía no se usan ni se solicitan credenciales para ellos.

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
y limpieza deshabilitada. El esquema de dominio corresponde a #158 (I0-11): aquí solo
se inicializa el historial de Flyway, sin tablas de negocio ni datos anteriores.
`ErrorResponse` vive temporalmente en `common/` hasta moverlo a `shared/api` en #155.
