# Pasitos

App móvil para que tutores y pediatras registren y sigan el crecimiento de niños y niñas de
0 a 15 años en Argentina: percentiles y estado según las referencias de la OMS y la SAP,
Calendario Nacional de Vacunación, eventos, hitos y hábitos, consejos por edad, consulta guiada
con un pediatra de cabecera e informes PDF. Se desarrolla como tesis.

## Documentación

El proyecto sigue Spec-Driven Development: un cambio de comportamiento empieza en la SPEC, nunca
en el código.

| Documento | Contenido |
|---|---|
| [SPEC funcional](docs/spec/README.md) | Qué hace Pasitos: actores, módulos, requerimientos y reglas |
| [SPEC técnica](docs/spec-tecnica/README.md) | Cómo se construye: arquitectura, stack, API, base de datos y proceso |
| [Plan de implementación](docs/plan/README.md) | Incrementos I0 a I6, tareas y dependencias |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Ramas, PRs, tablero y verificaciones antes de tomar una tarea |

## Estructura del repositorio

- `app/` — App Android (Kotlin + Jetpack Compose), paquete `com.pasitos.app`. En Android Studio
  se abre esta carpeta, no la raíz del repositorio.
- `backend/` — API propia (Kotlin + Ktor) con PostgreSQL.
- `shared/` — Biblioteca Kotlin que usan la app y el backend: cálculos (edades, crecimiento,
  vacunas) y contratos de la API. Ver [shared/README.md](shared/README.md).
- `docs/` — SPEC funcional, SPEC técnica y plan de implementación.

El stack con sus versiones está en [01 · Stack](docs/spec-tecnica/01-stack.md).

## Entorno local

La base de datos, Mailpit (captura los emails) y, si querés, el backend se levantan con Docker
Compose desde la raíz del repositorio:

```
cp backend/.env.example backend/.env
docker compose --env-file backend/.env --profile full up -d
```

Sin `--profile full` levanta solo la base y Mailpit, para correr el backend desde IntelliJ. El
detalle está en [backend/README.md](backend/README.md).
