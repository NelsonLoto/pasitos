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

Cada tarea es un issue de GitHub. La rama lleva el número del issue
(`feature/42-alta-de-hijo`), el PR incluye `Closes #42` y se abre como borrador apenas se
empieza, para que el resto del equipo vea qué está en curso. Detalle del proceso en
[`docs/spec-tecnica/32-proceso-de-desarrollo.md`](docs/spec-tecnica/32-proceso-de-desarrollo.md).

## Tablero

El [tablero de Pasitos](https://github.com/users/NelsonLoto/projects/1) mueve solo la mayoría de
las tarjetas: un issue nuevo entra en `Por hacer`, abrir el PR con `Closes #42` lo pasa a
`En curso` y mergearlo lo pasa a `Hecho`. Lo único manual es mover la tarjeta a `En revisión`
cuando marcás el PR como listo para revisar. No muevas a `Hecho` una tarjeta con PR antes del
merge: eso cierra el issue. Detalle de cada automatización en
[`32-proceso-de-desarrollo.md`](docs/spec-tecnica/32-proceso-de-desarrollo.md#tablero).

## Antes de tomar una tarea (personas y agentes de IA)

Para no pisar el trabajo de otro integrante, verificá **antes de asignarte un issue o crear una
rama**:

1. El issue no tiene responsable y está en `Por hacer` en el tablero.
2. No existe una rama remota con el número del issue:
   `git fetch --prune` y después `git branch -r | grep "/<número>-"`.
3. No hay un PR abierto que lo mencione: `gh pr list --search "<número>"`.
4. Las tareas de las que depende ya están en `Hecho`: el issue no figura como bloqueado
   (*Blocked by*). Las dependencias están cargadas como dependencias nativas de GitHub, así que
   alcanza con mirar el issue o con que este comando no devuelva nada:
   `gh api repos/NelsonLoto/pasitos/issues/<número>/dependencies/blocked_by --jq '.[] | select(.state=="open") | .number'`.

Si alguno de los cuatro puntos falla, **no tomes la tarea**: elegí otra o consultá con quien la
tiene. Recién después asignate el issue, movelo a `En curso` y creá la rama.

> **Nota para agentes de IA (Claude Code u otros):** antes de crear una rama o modificar código
> para una tarea, ejecutá las verificaciones 1 a 4 e informá el resultado a la persona con la que
> estás trabajando. Si encontrás trabajo en curso sobre la misma tarea o sobre los mismos
> archivos, detenete y consultá antes de seguir. No asumas que una tarea está libre porque no la
> ves en tu copia local: el estado real está en GitHub.

## Promoción a producción

Cuando `develop` esté estable y se decida hacer un release, se abre un Pull Request de `develop` → `main`.

## Commits

Se recomienda un mensaje de commit corto y descriptivo del *qué* y el *por qué*, evitando mensajes genéricos como "fix" o "cambios".

## Sobre la protección de ramas

GitHub Free no permite activar protección de ramas (bloqueo de push directo, revisión obligatoria) en repositorios **privados** — esa función requiere GitHub Pro o que el repositorio sea público. Como el repo se mantiene privado por manejar datos de salud de menores, `main` y `develop` **no están bloqueadas técnicamente**: nada impide un `git push` directo.

Esto significa que el flujo de arriba (rama por tarea + PR) depende de la disciplina del equipo, no de una regla forzada por GitHub. **No se debe pushear directo a `main` ni a `develop` bajo ninguna circunstancia**, aunque el sistema lo permita. Si en algún momento se evalúa pasar a GitHub Pro o a una organización con plan pago, esta sección se actualiza para reflejar la protección real.
