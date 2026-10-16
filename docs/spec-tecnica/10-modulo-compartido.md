# 10 · Módulo compartido (`shared/`)

Biblioteca Kotlin/JVM pura que usan la app y el backend (ADR-01). Garantiza que un percentil
calculado sin conexión en el teléfono sea idéntico al que calcula el servidor, y que los casos de
control se prueben una sola vez (riesgo R-01).

## 1. Contenido

| Paquete | Responsabilidad | Requerimientos |
|---|---|---|
| `growth` | Edades, selección de referencia, ajuste de longitud y talla, IMC, puntaje Z, percentil, estado, cruce de percentiles, plausibilidad | RF-CRE-04 a 08, RN-CRE-01 a 14, RN-HIJ-04 y 05 |
| `vaccination` | Estado de cada dosis, campañas vigentes, dosis para la carga retroactiva | RN-VAC-01 a 09, RF-VAC-04, RF-VAC-06 |
| `time` | Zona horaria de Argentina, "hoy", formato de edades, próximo control según cronograma | RNF-LOC-03, RN-HIJ-04, RN-NOT-02 |
| `api` | DTOs de requests y responses, enums y códigos de error de la API | [12 · API](12-api.md) |

Todo el código de `shared` es **puro**: recibe datos y parámetros y devuelve resultados, sin
acceder a red, base de datos ni reloj del sistema (la fecha de "hoy" se recibe como parámetro).
Eso lo hace trivial de probar.

## 2. Motor de crecimiento

### Entradas y salidas

```kotlin
data class ChildGrowthProfile(
    val sex: Sex,                       // FEMALE, MALE
    val birthDate: LocalDate,
    val gestationalWeeks: Int,          // 22..42; "No lo sé" llega como 40
)

data class MeasurementInput(
    val date: LocalDate,
    val weightKg: Double?,
    val lengthCm: Double?,
    val lengthPosition: LengthPosition?, // LYING, STANDING
    val headCircumferenceCm: Double?,
)

class GrowthEngine(private val reference: ReferenceData) {
    fun evaluate(child: ChildGrowthProfile, m: MeasurementInput): MeasurementEvaluation
    fun crossing(previous: MeasurementEvaluation, current: MeasurementEvaluation): Set<Indicator>
    fun summary(evaluations: List<MeasurementEvaluation>, today: LocalDate): GrowthSummary
}
```

`MeasurementEvaluation` contiene, por cada indicador aplicable: referencia usada, puntaje Z,
percentil, si el valor es aproximado, nivel de estado, clasificación técnica, clave del texto para
tutores y si supera los límites de plausibilidad. Incluye además el IMC, la longitud o talla
ajustada y la edad usada.

### Algoritmo

| Paso | Regla | Detalle |
|---|---|---|
| 1. Edad | RN-HIJ-04, RN-HIJ-05 | `ageDays` = días entre nacimiento y fecha de medición. Si `gestationalWeeks < 37` y la edad cronológica es menor a 24 meses: `ageDays -= (40 − gestationalWeeks) × 7`. Si el resultado es negativo, la medición **no es evaluable** (RN-CRE-14) |
| 2. Meses | — | `ageMonths = ageDays / 30.4375` (convención de la OMS) |
| 3. Referencia | RN-CRE-02 | `ageMonths < 60` → OMS 2006; si no, SAP |
| 4. Indicadores | RN-CRE-01 | Solo los aplicables a la referencia, la edad y los valores presentes. Peso para la longitud: longitud de 45 a 110 cm y `ageDays < 731`; peso para la talla: talla de 65 a 120 cm y `ageDays ≥ 731` |
| 5. Ajuste de longitud | RN-CRE-03 | `ageDays < 731` y medido de pie: +0,7 cm. `ageDays ≥ 731` y medido acostado: −0,7 cm |
| 6. IMC | RN-CRE-04 | `weightKg / (lengthAjustadaCm / 100)²` |
| 7. Parámetros | RN-CRE-05 | Tablas OMS por día de edad (0 a 1856 días) o por longitud o talla en pasos de 0,1 cm: interpolación lineal entre filas. Tablas SAP: interpolación lineal de L, M y S entre las edades publicadas |
| 8. Puntaje Z | RN-CRE-05 | `Z = ((X / M)^L − 1) / (L × S)`; si `L = 0`, `Z = ln(X / M) / S`. Para las tablas marcadas con `restrictedTails` (indicadores de peso de la OMS), si `|Z| > 3` se aplica el ajuste de la OMS con las distancias entre ±2 y ±3 DE |
| 9. Sin LMS | RN-CRE-05, PVM-14 | Si una tabla SAP solo publica percentiles: el percentil se interpola entre los publicados y el puntaje Z se calcula como la inversa de la normal estándar de ese percentil, marcado como aproximado |
| 10. Percentil | RN-CRE-06 | `percentil = Φ(Z) × 100`, con Φ la distribución normal estándar (precisión ≥ 1e-7). Se muestra entero de 1 a 99, "< 1" o "> 99" |
| 11. Plausibilidad | RN-CRE-11 | Si `Z` está fuera de los límites del indicador, `requiresConfirmation = true`. Los rangos absolutos de entrada se validan antes de calcular |
| 12. Estado | RN-CRE-07 | Se busca el umbral por indicador, referencia y edad; el nivel es el del rango de Z en el que cae. Valores iniciales: `CONSULT` si Z < −1,881 (P3) o Z > 1,881 (P97) |
| 13. Cruce | RN-CRE-08 | Entre dos evaluaciones consecutivas del mismo indicador **con la misma referencia**, se cuentan las líneas principales de percentil de la referencia que quedan estrictamente entre ambos percentiles. Si la cuenta alcanza el valor configurado (inicial: 2), el indicador sube a `MONITOR` |
| 14. Resumen | RN-CRE-07, RN-CRE-09 | Se toma la última fecha con mediciones; si ese día hay medición de un pediatra, se usa esa. El resumen es el nivel más severo de sus indicadores (`CONSULT` > `MONITOR` > `EXPECTED`). Si la última medición tiene más de 6 meses: `NO_RECENT_DATA` (SUP-18) |

### Datos de referencia

El backend sirve un paquete versionado que la app guarda en Room (ver
[14 · Sincronización](14-sincronizacion.md#5-datos-de-referencia-y-catálogos)):

```kotlin
@Serializable
data class ReferenceData(
    val version: String,                       // cambia con cada publicación del administrador
    val tables: List<ReferenceTableDto>,       // OMS y SAP
    val thresholds: List<ThresholdDto>,        // niveles y clasificación técnica
    val crossing: CrossingConfigDto,           // líneas principales por referencia y cantidad mínima
    val plausibility: List<PlausibilityDto>,   // límites de Z y rangos absolutos por indicador
)

@Serializable
data class ReferenceTableDto(
    val source: ReferenceSource,               // WHO_2006, SAP
    val indicator: Indicator,                  // WEIGHT_FOR_AGE, LENGTH_FOR_AGE, HEAD_CIRC_FOR_AGE,
                                               // WEIGHT_FOR_LENGTH, WEIGHT_FOR_HEIGHT, BMI_FOR_AGE
    val sex: Sex,
    val axis: Axis,                            // AGE_DAYS, AGE_MONTHS, LENGTH_CM
    val restrictedTails: Boolean,
    val rows: List<ReferenceRowDto>,           // x, l, m, s o percentiles publicados
)
```

Las tablas OMS se cargan desde los archivos oficiales de la OMS (tablas expandidas por día con
L, M y S). Las SAP, desde la transcripción del libro (doble transcripción, riesgo R-01).

## 3. Estados de vacunas

```kotlin
fun doseStatuses(
    birthDate: LocalDate,
    calendar: List<CalendarDoseDto>,   // versión vigente
    applied: List<AppliedDoseDto>,
    today: LocalDate,
): List<DoseWithStatus>
```

- La fecha indicada es `birthDate + ageDays` de la dosis, con **edad cronológica** (RN-VAC-08).
- Los estados siguen RN-VAC-01 con la anticipación y la tolerancia de cada dosis.
- Una dosis aplicada se identifica por su `doseCode` estable (por ejemplo, `PENTA-1`), que no
  cambia entre versiones del calendario (RN-VAC-03).
- Campañas: vigente si `today` está entre sus fechas y la edad del niño en su rango (RF-VAC-06).

## 4. Tiempo

- La zona horaria es `America/Argentina/Buenos_Aires`. `ArgentinaClock.today()` existe solo en
  la app y el backend; `shared` recibe la fecha como parámetro.
- `formatAge(birthDate, date, gestationalWeeks)` aplica RN-HIJ-04 ("12 días", "4 meses y 1 día",
  "3 años y 2 meses"). En prematuros agrega lo que pide RN-HIJ-05: antes de la fecha de término,
  cuánto falta para esa fecha ("14 días · faltan 8 semanas para su fecha de término"); desde
  entonces y hasta los 24 meses, la edad corregida ("4 meses · 2 meses corregidos", RF-CRE-03,
  CA2). La fecha de término se calcula con la fecha de nacimiento y las semanas de gestación.
- `nextControlDate(birthDate, schedule, lastFixedDate, today)` aplica RN-NOT-02.

## 5. Contratos de la API

- Todos los DTOs de [12 · API](12-api.md) viven en `com.pasitos.shared.api`, anotados con
  `@Serializable`. La app y el backend usan las mismas clases: si un campo cambia, ambos dejan de
  compilar hasta adaptarse.
- Dentro de `/api/v1` solo se admiten cambios **compatibles**: agregar campos opcionales o
  endpoints nuevos. Renombrar o quitar campos requiere coordinar ambos lados en el mismo PR.
- Los errores son `ErrorResponse` con un código `ErrorCode`, un enum compartido que viaja en
  `snake_case` ([11 · Backend](11-backend.md#5-errores)). Una versión vieja de la app que recibe
  un código que no conoce lo lee como `UNKNOWN` en vez de fallar: agregar un código es un cambio
  compatible.

## 6. Pruebas

| Tipo | Contenido |
|---|---|
| Casos de control OMS | Para cada indicador, ambos sexos y al menos 5 edades por tramo: el valor publicado en las columnas −3, −2, 0, +2 y +3 DE debe dar ese puntaje Z (tolerancia 0,01) |
| Casos de control SAP | Para cada tabla transcripta: el valor de cada percentil publicado debe dar ese percentil (tolerancia de redondeo a la unidad) |
| Bordes | 730 y 731 días; 60 meses; ajuste de 0,7 cm; prematuro antes y después del término y a los 24 meses; Z de ±3 a ±5 con colas restringidas; `L = 0` |
| Propiedades | El puntaje Z crece con el valor medido; el percentil está siempre entre 0 y 100 |
| Estados y cruces | Un caso por nivel, por cruce entre referencias distintas y por prioridad de la medición del pediatra |
| Vacunas | Cada estado de RN-VAC-01 en sus bordes; cambio de versión del calendario; carga retroactiva |
