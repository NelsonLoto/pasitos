# 32 · Proceso de desarrollo

## 1. Flujo de Spec-Driven Development

```mermaid
flowchart LR
    spec[SPEC funcional<br/>qué] --> tech[SPEC técnica<br/>cómo]
    tech --> plan[Plan del incremento<br/>tareas y dependencias]
    plan --> issue[Issue por tarea]
    issue --> pr[Rama + PR<br/>código y pruebas]
    pr --> ci[Integración continua]
    ci --> review[Revisión y merge]
    review --> demo[Demo quincenal]
    demo -.cambios.-> spec
```

- Un cambio de comportamiento empieza en la SPEC, nunca en el código.
- Si durante la implementación aparece un hueco o una contradicción en la SPEC, se frena esa parte,
  se resuelve en la SPEC (PR propio, aunque sea chico) y después se sigue.

## 2. Tareas

Cada tarea es un **issue de GitHub** dentro del tablero de GitHub Projects (ADR-22):

| Campo | Contenido |
|---|---|
| Título | Verbo + resultado: "Implementar alta de hijo (backend)" |
| Descripción | RF y RN que cubre, criterios de aceptación, archivos o módulos que toca, dependencias |
| Etiquetas | Incremento (`I0`…`I6`), componente (`backend`, `android`, `shared`, `admin`, `docs`) |
| Responsable | Quien la toma (asignación) |
| Estado en el tablero | `Por hacer` → `En curso` → `En revisión` → `Hecho` |

Los issues los crea el plan de cada incremento, con las dependencias marcadas para saber qué se
puede hacer en paralelo.

### Tablero

El tablero es el proyecto [Pasitos](https://github.com/users/NelsonLoto/projects/1) de GitHub
Projects. Las tarjetas son solo issues: el PR de cada tarea se ve dentro de su tarjeta, en
"Linked pull requests".

| Movimiento | Cuándo | Cómo |
|---|---|---|
| → `Por hacer` | Se crea o se reabre el issue | Automático |
| `Por hacer` → `En curso` | Se toma la tarea | A mano al asignarse; si se olvida, automático al abrir el PR en borrador con `Closes #N` |
| `En curso` → `En revisión` | El PR se marca como listo para revisar | **A mano**: GitHub no tiene una automatización para este paso |
| `En revisión` → `Hecho` | Se mergea el PR con `Closes #N` | Automático: el merge cierra el issue y el cierre lo pasa a `Hecho` |
| → `Hecho` en tareas sin PR | Tareas transversales (`T-xx`) terminadas | A mano: mover la tarjeta a `Hecho` cierra el issue |

Automatizaciones configuradas en el proyecto (menú `⋯` → *Workflows*):

| Automatización | Configuración |
|---|---|
| Auto-add to project | Repositorio `pasitos`, filtro `is:issue is:open` |
| Item added to project | Issues → `Por hacer` |
| Item reopened | Issues → `Por hacer` |
| Pull request linked to issue | → `En curso`. No `En revisión`: el PR se abre en borrador apenas se toma la tarea |
| Item closed | Issues → `Hecho` |
| Auto-close issue | Al pasar a `Hecho`, se cierra el issue |
| Pull request merged | → `Hecho`. Sin efecto en la práctica, porque los PRs no son tarjetas |
| Auto-add sub-issues to project | Encendida |
| Code changes requested, Code review approved, Auto-archive items | Apagadas |

- `Closes #N` cierra el issue al mergear porque `develop` es la rama por defecto del
  repositorio. Si la rama por defecto cambia, los issues dejan de cerrarse solos.
- Una tarjeta con PR no se mueve a mano a `Hecho` antes del merge: la automatización cerraría el
  issue.

### Dependencias

Las dependencias obligatorias entre tareas se escriben en el cuerpo del issue y además se cargan
como **dependencias nativas** de GitHub (*Blocked by*). Mientras alguna siga abierta, GitHub
marca el issue como bloqueado. Las dependencias blandas del plan, las que van entre paréntesis,
solo se escriben en el cuerpo.

- Se cargan al crear los issues de cada incremento, desde la barra lateral del issue
  (*Relationships* → *Mark as blocked by*) o por consola:
  `gh api -X POST repos/NelsonLoto/pasitos/issues/<número>/dependencies/blocked_by -F issue_id=<id>`,
  donde `<id>` es el identificador interno de la tarea que bloquea
  (`gh api repos/NelsonLoto/pasitos/issues/<número-que-bloquea> --jq .id`).
- Un pendiente técnico (`P-xx`) se carga como la tarea transversal que lo resuelve. Por ejemplo,
  I0-10 depende de P-07, que se carga como T-09.
- Si el plan escribe una dependencia solo en la columna "Bloquea" de la otra tarea, también se
  carga.

## 3. Ramas y PRs

Se mantiene el flujo de [`CONTRIBUTING.md`](../../CONTRIBUTING.md) y se agrega:

- Nombre de rama con el número de issue: `feature/42-alta-de-hijo`.
- El PR menciona `Closes #42` para cerrar el issue al mergear. Esa mención es también la que
  vincula el PR con la tarjeta y la mueve sola en el [tablero](#tablero).
- **PR en borrador temprano:** apenas se empieza una tarea, se pushea la rama y se abre el PR como
  *draft*. Así el resto del equipo ve qué está en curso y qué archivos se están tocando.
- Al marcar el PR como listo para revisar (*Ready for review*), se mueve la tarjeta a
  `En revisión` a mano.

## 4. Trabajo en paralelo

### Antes de tomar una tarea

Todo integrante, y **todo agente de IA que trabaje en nombre de un integrante**, verifica que la
tarea no esté en curso:

1. El issue no tiene responsable y está en `Por hacer`.
2. No existe una rama remota con el número del issue: `git fetch --prune` y
   `git branch -r | grep "/<número>-"`.
3. No hay un PR abierto que la mencione: `gh pr list --search "<número>"`.
4. Las tareas de las que depende ya están en `Hecho`: el issue no figura como bloqueado
   (*Blocked by*). Por consola, este comando no debe devolver nada:
   `gh api repos/NelsonLoto/pasitos/issues/<número>/dependencies/blocked_by --jq '.[] | select(.state=="open") | .number'`.

Si cualquiera de los cuatro puntos falla, **no se toma la tarea**: se elige otra o se consulta con
quien la tiene. Recién después se asigna el issue, se mueve a `En curso` y se crea la rama.

> **Nota para agentes de IA (Claude Code u otros):** antes de crear una rama o modificar código
> para una tarea, ejecutá las verificaciones 1 a 4 de esta sección e informá el resultado. Si
> encontrás trabajo en curso sobre la misma tarea o sobre los mismos archivos, detenete y
> consultá a la persona con la que estás trabajando. No asumas que una tarea está libre porque
> no la ves en tu copia local.

Estas verificaciones y la nota para agentes están también en
[`CONTRIBUTING.md`](../../CONTRIBUTING.md#antes-de-tomar-una-tarea-personas-y-agentes-de-ia),
que es lo primero que leen tanto las personas como los agentes al trabajar en el repositorio.

### Puntos de conflicto y cómo evitarlos

| Punto de conflicto | Regla |
|---|---|
| Migraciones de base de datos | Versión por fecha y hora (ADR-23); una migración por PR |
| Contratos de la API en `shared/api` | El PR que cambia un contrato incluye el ajuste del backend; la app se adapta en un PR siguiente. No se cambian contratos que otro integrante está usando en una tarea en curso sin avisar |
| Registro de rutas (`Application.module()`) | Cada dominio expone una única función `xRoutes()`; agregarla es una sola línea |
| Navegación de la app | Cada pantalla agrega su ruta en una línea; los grafos por modo evitan tocar el mismo bloque |
| Base de Room (`PasitosDatabase`) | Agregar una entidad es una línea más una migración de Room numerada; coordinar si dos tareas agregan entidades en la misma semana |
| Módulos de Hilt | Uno por dominio (`data/x/di/`), nunca un módulo global compartido |

### División recomendada

Las tareas se dividen **por dominio de punta a punta** (por ejemplo, "vacunas: backend + app") o
por capa dentro de un mismo dominio cuando hay dependencia clara (primero backend, después app).
Dos personas no trabajan a la vez en el mismo dominio salvo que el plan lo indique.

## 5. Commits

- En español, con un prefijo que indica el tipo: `feat:`, `fix:`, `chore:`, `docs:`, `test:`,
  `refactor:`.
- Mensaje que explica el qué y el porqué: `feat: alta de hijo con datos de nacimiento (RF-HIJ-01)`.

## 6. Demos quincenales

Al cierre de cada incremento:

1. Lista de aceptación manual ejecutada en un dispositivo real.
2. Demo de los RF del incremento contra el backend local con datos ficticios.
3. Registro de los cambios pedidos por el profesor: si afectan comportamiento, se cargan como
   cambio de SPEC antes de implementarlos.
