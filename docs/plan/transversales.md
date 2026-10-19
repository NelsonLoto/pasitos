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
| T-09 | Crear la organización de Sentry en región UE. Se postergó el 19/10/2026 (antes vencía el 25/10/2026): hasta el piloto Sentry casi no aporta | 31/01/2027 | I0-10 (en I6) | P-07 |
| T-10 | Crear la cuenta del proveedor de email en región UE. En desarrollo alcanza con Mailpit | 31/01/2027 | I6-I1 | P-04 |
| T-11 | Registrar el dominio y configurar SPF, DKIM y DMARC | 31/01/2027 | I6-I1 | P-03 |
| T-12 | Elegir el proveedor de hosting del piloto | 31/01/2027 | I6-I1 | P-01 |
| T-13 | Cuenta de desarrollador de Google Play (Nelson) y resguardo de la clave de subida | 31/01/2027 | I6-I2 | P-06, P-08 |
| T-14 | Textos legales: términos y condiciones y política de privacidad (Google Play exige la política para publicar). En desarrollo se usa una versión provisoria `0` de los términos | 31/01/2027 | I6-I2, piloto | RNF-LEG-01 |
| T-15 | ✅ **No aplica (14/10/2026).** El piloto usa datos ficticios, así que no hace falta consultar requisitos institucionales ni comité de ética | — | — | D-65, SUP-14 |
| T-16 | Convocar a los participantes del piloto (tutores y pediatras que prueben la app con datos ficticios) y preparar la guía que reciben | 14/02/2027 | Piloto | RNF-PRI-08 |
| T-17 | ✅ **Hecho (14/10/2026).** Se avisó al equipo y se envió el comando para actualizar el remoto al repositorio renombrado | — | — | P-02 |
| T-18 | **Diseñar los datos ficticios** para demos y piloto: niños de distintas edades (incluido un prematuro y uno mayor de 5 años), con trayectorias de crecimiento que muestren cada nivel de estado y un cruce de percentiles, vacunas al día, atrasadas y con carga retroactiva, y varios tutores. Se cargan con un script en `backend/seed/demo/` | 25/11/2026 (para la demo de I2); completo antes del piloto | Demos de I2 en adelante, piloto | D-65 |

Las tareas en **negrita** son las que más riesgo tienen de atrasar el proyecto si se dejan para
último momento.
