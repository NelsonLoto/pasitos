# backend

Backend de Pasitos: Kotlin + Ktor, con Exposed + HikariCP para el acceso a
PostgreSQL y Flyway para versionar el schema.

## Requisitos

- JDK 21
- PostgreSQL 18 corriendo local (nativo, no Docker — la base `pasitos` y el
  usuario `pasitos_dev` ya se crean a mano una sola vez por máquina de
  desarrollo).

## Setup

1. Copiar la plantilla de variables de entorno del backend (si no existe
   todavía `backend/.env`):

   ```
   cp .env.example .env
   ```

   `.env` está en `.gitignore` — nunca se commitea. Ahí van host, puerto,
   nombre de base, usuario y contraseña de conexión a la instancia nativa de
   Postgres (por defecto `localhost:5432`, base `pasitos`, usuario
   `pasitos_dev`).

2. Levantar el backend:

   ```
   ./gradlew run
   ```

   Al arrancar, `DatabaseFactory` corre las migraciones de Flyway
   (`src/main/resources/db/migration`) contra la base `pasitos` y después
   abre el pool de conexiones (HikariCP) que usa Exposed.

3. Verificar: `GET http://localhost:8080/health` debería responder `OK`.

## Schema

El schema versionado en Flyway (`V1__esquema_inicial.sql`) es el mismo que
`D:\Pasitos\Pasitos_base_de_datos.sql` (fuente editable en
`D:\Pasitos\App_Nino_Sano_BD.dbml`). Cambios al modelo de datos van como
nuevas migraciones `V2__...`, `V3__...`, nunca editando `V1` una vez
mergeado.
