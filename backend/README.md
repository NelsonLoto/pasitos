# backend

Backend de Pasitos: Kotlin + Ktor, con Exposed + HikariCP para el acceso a
PostgreSQL y Flyway para versionar el schema.

## Requisitos

- JDK 21
- Docker (para levantar PostgreSQL) — necesita virtualización habilitada en
  la BIOS (Intel VT-x / AMD-V). Si tu máquina no la soporta, ver la sección
  "Alternativa sin Docker" más abajo.

## Setup

1. Copiar la plantilla de variables de entorno del backend (si no existe
   todavía `backend/.env`):

   ```
   cp backend/.env.example backend/.env
   ```

   `.env` está en `.gitignore` — nunca se commitea. Ahí van host, puerto,
   nombre de base, usuario y contraseña de conexión (por defecto
   `localhost:5433`, base `pasitos`, usuario `pasitos_dev`).

2. Levantar PostgreSQL con Docker (desde la raíz del repo), usando
   `backend/.env` como archivo de variables:

   ```
   docker compose --env-file backend/.env up -d
   ```

   **Importante:** Docker Compose por defecto solo lee un archivo `.env` en
   la raíz del repo (al lado de `docker-compose.yml`), no `backend/.env`. Si
   corrés `docker compose up -d` sin `--env-file`, `DB_PORT` no se toma de
   `backend/.env` y el contenedor usa el default `5432`, lo que puede chocar
   con una instalación nativa de Postgres en el mismo puerto. Usá siempre
   `--env-file backend/.env` para evitar esto.

   Esto crea la base `pasitos` con las credenciales de desarrollo definidas en
   `backend/.env`. El contenedor expone el puerto **5433** en el host (no
   5432), para no chocar con una instalación nativa de Postgres que ya exista
   en tu máquina.

3. Levantar el backend:

   ```
   ./gradlew run
   ```

   Al arrancar, `DatabaseFactory` corre las migraciones de Flyway
   (`src/main/resources/db/migration`) contra la base `pasitos` y después
   abre el pool de conexiones (HikariCP) que usa Exposed.

4. Verificar: `GET http://localhost:8080/health` debería responder `OK`.

## Alternativa sin Docker

Si tu PC no tiene virtualización habilitada y no querés tocar la BIOS, podés
instalar PostgreSQL nativo en Windows en su lugar: creá a mano la base
`pasitos` y el usuario `pasitos_dev`, y en tu `backend/.env` usá `DB_PORT=5432`
(el puerto nativo) en vez de 5433. El resto de los pasos (`./gradlew run`,
`/health`) funciona igual.

## Schema

El schema versionado en Flyway (`V1__esquema_inicial.sql`) es el mismo que
`D:\Pasitos\Pasitos_base_de_datos.sql` (fuente editable en
`D:\Pasitos\App_Nino_Sano_BD.dbml`). Cambios al modelo de datos van como
nuevas migraciones `V2__...`, `V3__...`, nunca editando `V1` una vez
mergeado.
