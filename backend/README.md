# backend

Backend de Pasitos: Kotlin + Ktor, con Exposed + HikariCP para el acceso a
PostgreSQL y Flyway para versionar el schema.

## Requisitos

- JDK 21
- Docker (para levantar PostgreSQL y Mailpit) — necesita virtualización
  habilitada en la BIOS (Intel VT-x / AMD-V). Si tu máquina no la soporta, ver
  la sección "Alternativa sin Docker" más abajo.

## Setup

1. Copiar la plantilla de variables de entorno del backend (si no existe
   todavía `backend/.env`):

   ```
   cp backend/.env.example backend/.env
   ```

   `.env` está en `.gitignore` — nunca se commitea. Ahí van host, puerto,
   nombre de base, usuario y contraseña de conexión (por defecto
   `localhost:5433`, base `pasitos`, usuario `pasitos_dev`).

2. Levantar PostgreSQL y Mailpit con Docker (desde la raíz del repo), usando
   `backend/.env` como archivo de variables:

   ```
   docker compose --env-file backend/.env up -d
   ```

   **Importante:** Docker Compose por defecto solo lee un archivo `.env` en
   la raíz del repo (al lado de `docker-compose.yml`), no `backend/.env`. Sin
   `--env-file` usa los valores por defecto de `docker-compose.yml`, que son
   los de `.env.example`: si cambiaste algo en tu `backend/.env` (por ejemplo,
   el puerto), no se toma. Usá siempre `--env-file backend/.env`.

   Esto crea la base `pasitos` con las credenciales de desarrollo definidas en
   `backend/.env`. El contenedor expone el puerto **5433** en el host (no
   5432), para no chocar con una instalación nativa de Postgres que ya exista
   en tu máquina.

   Mailpit captura los emails que manda el backend, sin enviarlos: se ven en
   http://localhost:8025. El backend los manda por SMTP a `localhost:1025`
   desde que envía emails (I1-B1).

   Los puertos se publican solo en `127.0.0.1`, así no quedan abiertos a otras
   computadoras de la red. Si tu base es anterior a este cambio, el primer `up`
   la recrea para cambiar el puerto; los datos se conservan.

3. Levantar el backend:

   ```
   ./gradlew run
   ```

   Al arrancar, `DatabaseFactory` corre las migraciones de Flyway
   (`src/main/resources/db/migration`) contra la base `pasitos` y después
   abre el pool de conexiones (HikariCP) que usa Exposed.

4. Verificar: `GET http://localhost:8080/health` debería responder `OK`.

5. Cargar los datos de referencia (el Calendario Nacional de `seed/calendar/`):

   ```
   ./gradlew seedReferenceData
   ```

   Usa la misma configuración que el backend y aplica las migraciones antes, así
   que funciona también con la base vacía y con el backend en Docker. Se puede
   correr más de una vez: lo que ya está cargado se saltea. Sin calendario,
   `GET /api/v1/vaccination-calendar` responde con las listas vacías.

## Todo con Docker, backend incluido

Para correr el backend sin IntelliJ ni JDK (por ejemplo, si trabajás solo en la
app), el perfil `full` también lo levanta en Docker:

```
docker compose --env-file backend/.env --profile full up -d
```

- La imagen se construye con [`Dockerfile`](Dockerfile) y se reconstruye en
  cada `up`, así siempre corre el código actual. La primera vez tarda unos
  minutos; después, sin cambios, unos segundos.
- El backend responde en `http://localhost:8080`, el mismo puerto que
  `./gradlew run`: no corras los dos a la vez. Para usarlo desde el emulador,
  `adb reverse tcp:8080 tcp:8080`, como siempre.
- Logs: `docker compose logs -f backend`. Para frenar todo:
  `docker compose --profile full down` (sin `-v`, conserva la base).
- La imagen no lleva `backend/.env` ni ningún secreto: la configuración le
  llega por variables de entorno desde `docker-compose.yml`. Es la misma
  imagen que se usará en el piloto
  ([30 · Infraestructura](../docs/spec-tecnica/30-infraestructura.md#imagen-del-backend)).

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
