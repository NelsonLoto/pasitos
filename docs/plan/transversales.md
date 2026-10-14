# Tareas transversales

Tareas que no son de programación pero de las que dependen los incrementos: datos, validación
médica, cuentas en servicios externos, textos legales y el piloto. Cualquier integrante puede
tomarlas, y también se cargan como issues (etiqueta `datos`, `infra` o `docs`).

> **⚠️ Antes de tomar una tarea:** verificar que no esté en curso según
> [`CONTRIBUTING.md`](../../CONTRIBUTING.md#antes-de-tomar-una-tarea-personas-y-agentes-de-ia).

| ID | Tarea | Fecha límite | Necesaria para | Referencia |
|---|---|---|---|---|
| T-01 | **Transcribir las tablas SAP dos veces, por dos personas distintas y sin mirarse**, para después comparar las dos versiones. Indicar si el libro publica L, M y S o solo percentiles | 15/11/2026 | I2-D2 | R-01, PVM-14, P-10 |
| T-02 | Descargar las tablas expandidas de la OMS (L, M y S por día y por longitud o talla) para cada indicador y sexo | 15/11/2026 | I2-D1 | P-09 |
| T-03 | **Reunión con pediatras n.º 1 (crecimiento):** PVM-01 a PVM-07 y PVM-14. Llevar la lista de preguntas de la SPEC funcional | 20/11/2026 | I2-S2 | [Pendientes de validación médica](../spec/92-pendientes.md#pendientes-de-validación-médica) |
| T-04 | **Reunión con pediatras n.º 2 (consulta y contenido):** PVM-08 a PVM-13 | 18/12/2026 | I4, I5 | Ídem |
| T-05 | Catálogo inicial de hitos del desarrollo con edad esperada, edad límite y fuente | 03/01/2027 | I4-A3, I5-B1 | RN-EVT-05, PVM-08 |
| T-06 | Contenido inicial de consejos por tramo de edad y categoría, con fuentes | 17/01/2027 | I5-B3 | RN-CJO-01, PVM-11 |
| T-07 | Textos del estado para tutores, en lenguaje no diagnóstico | 29/11/2026 (provisorios); validados antes del piloto | I2-A2 | RN-CRE-10, PVM-10 |
| T-08 | Crear el proyecto de Google Cloud y Firebase con una cuenta del equipo (no personal) y registrar `com.pasitos.app` | 01/11/2026 | I1-B2, I1-A2, I6-B2 | P-05 |
| T-09 | Crear la organización de Sentry en región UE | 25/10/2026 | I0-10 | P-07 |
| T-10 | Crear la cuenta del proveedor de email en región UE. En desarrollo alcanza con Mailpit | 31/01/2027 | I6-I1 | P-04 |
| T-11 | Registrar el dominio y configurar SPF, DKIM y DMARC | 31/01/2027 | I6-I1 | P-03 |
| T-12 | Elegir el proveedor de hosting del piloto | 31/01/2027 | I6-I1 | P-01 |
| T-13 | Cuenta de desarrollador de Google Play (Nelson) y resguardo de la clave de subida | 31/01/2027 | I6-I2 | P-06, P-08 |
| T-14 | Textos legales: términos y condiciones, política de privacidad y consentimiento informado del piloto. En desarrollo se usa una versión provisoria `0` de los términos | 31/01/2027 | Piloto | RNF-LEG-01, RNF-PRI-08 |
| T-15 | **Consultar con el profesor los requisitos institucionales del piloto** (aval de un comité de ética, modelo de consentimiento). Hacerlo cuanto antes: si hace falta un comité, puede demorar semanas | 15/11/2026 | Piloto | SUP-14 |
| T-16 | Reclutar a las familias y pediatras del piloto | 14/02/2027 | Piloto | — |
| T-17 | Cada integrante actualiza el remoto de su copia local al repositorio renombrado: `git remote set-url origin https://github.com/NelsonLoto/pasitos.git` | Inmediato | — | P-02 |

Las tareas en **negrita** son las que más riesgo tienen de atrasar el proyecto si se dejan para
último momento.
