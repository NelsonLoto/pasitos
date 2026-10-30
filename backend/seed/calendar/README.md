# Calendario Nacional de Vacunación

Datos iniciales del calendario ([SPEC funcional 13](../../../docs/spec/13-vacunacion.md), D-68). En
`local` y `ci` los carga la tarea `seedReferenceData` (I1-B7); en el piloto, el administrador los
gestiona desde el panel (RF-ADM-06).

## Fuente

Transcripto de los documentos oficiales del Ministerio de Salud de la Nación, publicados el
10/03/2026:

- [Calendario Nacional de Vacunación 2026](https://www.argentina.gob.ar/sites/default/files/2026-03-10_calendario_nacional_vacunacion_70x50_web.pdf) (póster, PDF).
- [Carnet Unificado de Vacunación, niños y adolescentes](https://www.argentina.gob.ar/sites/default/files/2026-03-10_or_carnet_unificado_vacunacion_ninos-adolescentes.pdf) (PDF).
- Página oficial: [argentina.gob.ar/salud/vacunas](https://www.argentina.gob.ar/salud/vacunas).

`calendario-nacional-2026-01-01.csv` es la versión vigente **desde el 01/01/2026**, cuando la 2ª
dosis de triple viral pasó a los 15-18 meses para los nacidos desde el 01/07/2024.

⚕️ **Pendiente de validación médica** (PVM-12, PVM-15 a PVM-17 de la
[SPEC funcional](../../../docs/spec/92-pendientes.md)).

## Columnas

| Columna | Significado |
|---|---|
| `vaccine` | Nombre de la vacuna, como se muestra en la app |
| `prevents` | Qué previene. Sale de las notas del calendario oficial; en Hepatitis B, Rotavirus, Meningococo, Hepatitis A, Varicela y VPH, que no tienen nota, es el nombre de la enfermedad |
| `source` | Fuente de la información, que la app muestra en el detalle de la vacuna (RF-VAC-07) |
| `dose_code` | Identificador estable de la dosis (RN-VAC-03). **Nunca se cambia ni se reutiliza**: con él se reconocen las dosis aplicadas en todas las versiones |
| `dose_label` | Nombre de la dosis ("1ª dosis", "Refuerzo") |
| `age_months` | Edad indicada en meses: la fecha indicada es el nacimiento más esa cantidad de meses (recién nacido 0, 5 años 60, 11 años 132) |
| `lead_days` | Anticipación: desde cuántos días antes la dosis es "Próxima" (RN-VAC-01) |
| `tolerance_days` | Tolerancia: cuántos días después de la fecha indicada sigue "Pendiente" antes de pasar a "Atrasada" |
| `born_from`, `born_until` | Rango de fechas de nacimiento al que corresponde la fila, ambos inclusive; vacío es sin límite (RN-VAC-10) |
| `sort_order` | Orden en el calendario |

Cada fila es una fila de `calendar_doses` ([SPEC técnica 13](../../../docs/spec-tecnica/13-base-de-datos.md)).
Las vacunas (`vaccine`, `prevents`, `source`) van a `vaccines`. Cada vacuna está una sola vez: una
versión nueva actualiza su "qué previene" y su fuente.

Los campos con comas van entre comillas. El archivo puede tener fin de línea LF o CRLF.

## Cómo se carga

Con la base local levantada (`docker compose up -d db`), desde `backend/`:

```bash
./gradlew seedReferenceData
```

Usa la misma configuración que el backend (`backend/.env` o las variables de entorno) y aplica las
migraciones antes. Carga cada archivo cuya fecha de vigencia todavía no esté en la base, como
versión publicada, en una sola transacción: si una fila no se puede leer o `calendarProblems()`
encuentra un problema, no carga nada de ese archivo. Se puede correr más de una vez.

## Criterios de la transcripción

- **27 dosis con edad fija** para niños y adolescentes. La 2ª de triple viral (`SRP-2`) tiene dos
  filas: a los 15 meses para los nacidos desde el 01/07/2024 y a los 5 años para los nacidos hasta
  el 30/06/2024.
- **Anticipación y tolerancia:** 7 y 30 días, salvo las dosis con ventana en el calendario
  oficial: las de "15 a 18 meses" tienen 120 días de tolerancia (hasta los 18 meses, más 30) y las
  de 5 y 11 años ("en el año en que cumplen"), 365.
- **Quedan fuera** y se registran como otras vacunas (RF-VAC-05):
  - la antigripal, que es anual (de los 6 a los 24 meses);
  - las regionales, solo para zonas de riesgo: fiebre amarilla y fiebre hemorrágica argentina;
  - los esquemas de "iniciar o completar", que son de recuperación (RN-VAC-05);
  - las dosis de adultos, embarazadas y personal de salud.
- **Rotavirus** tiene edad máxima: la 1ª antes de las 14 semanas y 6 días, y la 2ª antes de las 24
  semanas. Su tolerancia de 30 días queda dentro de ese límite (PVM-16).

## Cómo se actualiza

Cuando el Ministerio publique un calendario nuevo:

1. Crear un archivo nuevo con la fecha de vigencia en el nombre (`calendario-nacional-AAAA-MM-DD.csv`).
   No se edita uno que ya se cargó: las versiones anteriores explican las dosis aplicadas.
2. Mantener los `dose_code` de las dosis que siguen; usar códigos nuevos solo para dosis nuevas.
3. Si una dosis cambia de edad solo para algunas cohortes, usar `born_from` y `born_until`, sin
   superponer rangos del mismo código. `calendarProblems()` (`shared/vaccination`) lo verifica.
4. Citar en este README los documentos oficiales usados.
