# backend

Backend de Pasitos: Kotlin + Ktor, con Exposed + HikariCP para el acceso a
PostgreSQL y Flyway para versionar el schema.

## Requisitos

- JDK 21
- Docker (para levantar PostgreSQL)

## Setup

1. Levantar PostgreSQL con Docker (desde la raíz del repo):

   ```
   docker compose up -d
   ```

   Esto crea la base `pasitos` con las credenciales de desarrollo definidas en
   `.env.example` (por defecto, sin necesitar nada más). Para usar otras
   credenciales, copiá un `.env` en la raíz del repo o corré
   `docker compose --env-file backend/.env up -d`.

2. Copiar la plantilla de variables de entorno del backend (si no existe
   todavía `backend/.env`):

   ```
   cp .env.example .env
   ```

   `.env` está en `.gitignore` — nunca se commitea. Ahí van host, puerto,
   nombre de base, usuario y contraseña de conexión.

3. Levantar el backend:

   ```
   ./gradlew run
   ```

   Al arrancar, `DatabaseFactory` corre las migraciones de Flyway
   (`src/main/resources/db/migration`) contra la base `pasitos` y después
   abre el pool de conexiones (HikariCP) que usa Exposed.

4. Verificar: `GET http://localhost:8080/health` debería responder `OK`.

## Schema

El schema versionado en Flyway (`V1__esquema_inicial.sql`) es el mismo que
`D:\Pasitos\Pasitos_base_de_datos.sql` (fuente editable en
`D:\Pasitos\App_Nino_Sano_BD.dbml`). Cambios al modelo de datos van como
nuevas migraciones `V2__...`, `V3__...`, nunca editando `V1` una vez
mergeado.
