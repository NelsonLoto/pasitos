# Pasitos — Plan de implementación

| | |
|---|---|
| **Versión** | 1.0.5 |
| **Fecha** | 24/10/2026 |
| **Basado en** | [SPEC funcional 1.2.0](../spec/README.md) y [SPEC técnica 1.2.0](../spec-tecnica/README.md) |

## Cómo se usa este plan

- Cada tarea tiene un identificador (`I0-03`, `I1-B4`) y se convierte en un **issue de GitHub**
  con la plantilla [`Tarea`](../../.github/ISSUE_TEMPLATE/tarea.md).
- Las tareas de **I0 e I1 están detalladas** y listas para cargarse como issues. Las de I2 a I6
  están en versión preliminar: se detallan al inicio de cada incremento, con lo aprendido y con
  las validaciones médicas ya resueltas (planificación por olas).
- Cada tarea indica de qué depende, qué bloquea, qué carpetas toca y cuándo se puede hacer en
  paralelo con otras. Los cuatro integrantes pueden tomar cualquier tarea.

> **⚠️ Antes de tomar una tarea, sea una persona o un agente de IA:** verificar que no esté en
> curso según [`CONTRIBUTING.md`](../../CONTRIBUTING.md#antes-de-tomar-una-tarea-personas-y-agentes-de-ia):
> el issue sin responsable, sin rama remota con su número, sin PR abierto que lo mencione y con
> sus dependencias en `Hecho`. Un agente de IA informa el resultado de esas verificaciones antes
> de crear una rama o tocar código, y se detiene si encuentra trabajo en curso.

## Archivos

| Archivo | Contenido |
|---|---|
| [I0 · Fundaciones técnicas](I0-fundaciones.md) | 10 tareas detalladas |
| [I1 · Base de tutores](I1-base-de-tutores.md) | 16 tareas detalladas |
| [I2 a I6 · Preliminar](I2-a-I6-preliminar.md) | Tareas candidatas por incremento |
| [Tareas transversales](transversales.md) | Datos, validación médica, cuentas externas y legales, con fecha límite |

## Calendario

| Período | Etapa | Encuentro con el profesor (aprox.) |
|---|---|---|
| 19/10 – 01/11/2026 | **I0** · Fundaciones técnicas | Demo de I0 |
| 02/11 – 15/11/2026 | **I1** · Base de tutores | Demo de I1 |
| 16/11 – 29/11/2026 | **I2** · Motor de crecimiento | Demo de I2 |
| 30/11 – 13/12/2026 | **I3** · Pediatras | Demo de I3 |
| 14/12/2026 – 03/01/2027 | Margen: atrasos y receso de fin de año | — |
| 04/01 – 17/01/2027 | **I4** · Consulta guiada | Demo de I4 |
| 18/01 – 31/01/2027 | **I5** · Seguimiento y sin conexión | Demo de I5 |
| 01/02 – 14/02/2027 | **I6** · Cierre | Demo de I6 |
| 15/02 – 07/03/2027 | **Piloto** con usuarios y datos ficticios | Resultados del piloto |
| 08/03 – 14/03/2027 | Ajustes finales y documentación de la tesis | — |
| 15/03 – 26/03/2027 | Presentación de la tesis | — |

Si un incremento se atrasa, primero se usa el margen de diciembre. Si no alcanza, se recortan
las tareas marcadas como "puede pasar al incremento siguiente" antes de mover fechas.

## Reglas de trabajo en paralelo

1. **Olas.** Dentro de cada incremento, las tareas se agrupan en olas: todas las de una ola se
   pueden hacer en paralelo; una ola empieza cuando terminan las tareas de las que depende, no
   necesariamente la ola anterior completa.
2. **Carpetas que toca.** Dos tareas en paralelo no deberían tocar los mismos archivos. Cuando
   comparten un punto de conflicto conocido (registro de rutas, navegación, base de Room), el
   cambio es de una línea y se resuelve al mergear.
3. **Tareas exclusivas.** Algunas tareas (marcadas 🔒) cambian archivos de todo un componente y se
   hacen solas: mientras están abiertas, nadie más toca ese componente.
4. **Contratos primero.** Cuando una tarea de app depende de una de backend, el DTO en
   `shared/api` se mergea primero (puede ser el primer commit del PR de backend). Con el contrato
   definido, la app puede avanzar con un repositorio falso.
5. **PR en borrador temprano.** Apenas se toma una tarea, se pushea la rama y se abre el PR como
   borrador.

## Tamaños

| Tamaño | Esfuerzo de referencia |
|---|---|
| **S** | Hasta 1 día de una persona |
| **M** | 2 o 3 días |
| **L** | 4 o 5 días; conviene partirla si se puede |

## Cómo pasar el plan a GitHub

1. Crear el tablero de GitHub Projects con las columnas `Por hacer`, `En curso`, `En revisión`
   y `Hecho`.
2. Crear un issue por tarea con la plantilla `Tarea`, copiando la ficha del plan. Etiquetas: el
   incremento (`I0`…) y el componente (`app`, `backend`, `shared`, `infra`, `docs`, `datos`).
3. Marcar las dependencias en el cuerpo del issue ("Depende de #12") y cargar las obligatorias
   como dependencias nativas de GitHub, según
   [32 · Proceso de desarrollo](../spec-tecnica/32-proceso-de-desarrollo.md#dependencias).
4. Al cerrar cada incremento, revisar y detallar las tareas del siguiente.
