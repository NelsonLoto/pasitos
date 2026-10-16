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

El esquema está definido en
[13 · Base de datos](../docs/spec-tecnica/13-base-de-datos.md) y se versiona con
Flyway en `src/main/resources/db/migration`.

- La línea base es `V2026_10_16_0954__create_baseline.sql` (I0-04). Reemplazó
  a `V1` y `V2` (`personas`, `usuarios`, `sesiones`).
- Cada migración nueva lleva la fecha y hora en que se crea:
  `V2026_10_20_1530__create_care_links.sql` (ADR-23). Flyway corre con
  `outOfOrder`, así que no importa en qué orden se mergeen las ramas.
- Una migración ya mergeada **nunca se edita**: se corrige con otra nueva.
- Si una migración borra una columna, en el mismo PR se actualiza el objeto
  `Table` de Exposed.

### Borrar la base local (una sola vez, después de I0-04)

Si tu base local tiene las migraciones viejas, el backend no arranca y avisa
con "La base tiene migraciones que no coinciden con las del código". Borrala y
volvé a levantarla (desde la raíz del repo):

```
docker compose --env-file backend/.env down -v
docker compose --env-file backend/.env up -d
```

`-v` borra el volumen con los datos. Solo hay datos de prueba.

## Estructura

Un paquete por dominio (`auth`, `child`, `guardianship`, `measurement`), cada
uno con rutas, servicio, repositorio y tablas
([11 · Backend](../docs/spec-tecnica/11-backend.md#1-capas)). Lo transversal
está en `common/` y `config/`; los servicios se crean en `Dependencies.kt`.

## Pruebas

```
./gradlew test
```

Las pruebas de integración levantan un PostgreSQL 16 real con
[Testcontainers](https://testcontainers.com/), así que **Docker tiene que estar
corriendo**. No usan la base local ni el archivo `.env`.
