# Cómo contribuir

## Ramas

- `main` — rama productiva. Solo recibe merges desde `develop` vía Pull Request. Nadie pushea directo acá.
- `develop` — rama de integración. Acá se juntan todas las features antes de promocionar a `main`.
- Ramas de trabajo — una por tarea, **no una por persona**, creadas siempre desde `develop`:
  - `feature/<nombre-corto>` — funcionalidad nueva. Ej: `feature/login-usuario`, `feature/curva-crecimiento`.
  - `fix/<nombre-corto>` — corrección de bug. Ej: `fix/calculo-percentil-bordes`.
  - `chore/<nombre-corto>` — configuración, dependencias, tooling. Ej: `chore/setup-ktor`.

## Flujo de trabajo

1. Actualizar `develop` local: `git checkout develop && git pull`.
2. Crear la rama de la tarea: `git checkout -b feature/nombre-de-la-tarea`.
3. Commitear y pushear: `git push -u origin feature/nombre-de-la-tarea`.
4. Abrir un Pull Request contra `develop` (no contra `main`) y completar el template.
5. Esperar al menos 1 aprobación antes de mergear.
6. Borrar la rama una vez mergeada (GitHub lo ofrece automáticamente al cerrar el PR).

## Promoción a producción

Cuando `develop` esté estable y se decida hacer un release, se abre un Pull Request de `develop` → `main`.

## Commits

Se recomienda un mensaje de commit corto y descriptivo del *qué* y el *por qué*, evitando mensajes genéricos como "fix" o "cambios".

## Sobre la protección de ramas

GitHub Free no permite activar protección de ramas (bloqueo de push directo, revisión obligatoria) en repositorios **privados** — esa función requiere GitHub Pro o que el repositorio sea público. Como el repo se mantiene privado por manejar datos de salud de menores, `main` y `develop` **no están bloqueadas técnicamente**: nada impide un `git push` directo.

Esto significa que el flujo de arriba (rama por tarea + PR) depende de la disciplina del equipo, no de una regla forzada por GitHub. **No se debe pushear directo a `main` ni a `develop` bajo ninguna circunstancia**, aunque el sistema lo permita. Si en algún momento se evalúa pasar a GitHub Pro o a una organización con plan pago, esta sección se actualiza para reflejar la protección real.
