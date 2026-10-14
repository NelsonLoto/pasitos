# 12 · Crecimiento (`CRE`)

Este módulo concentra las reglas clínicas más sensibles de Pasitos. Todas las marcadas con
⚕️ PVM pueden implementarse con los valores iniciales indicados, pero sus parámetros viven en la
configuración del administrador (RF-ADM-04, RF-ADM-05) y no en el código.

## Requerimientos funcionales

### RF-CRE-01 · Registrar una medición
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** sí para el tutor (RNF-OFF-02)

| Dato | Unidad y precisión | Observaciones |
|---|---|---|
| Fecha de la medición | dd/mm/aaaa | Obligatoria. Por defecto, hoy |
| Peso | kg, hasta 3 decimales | |
| Longitud o talla | cm, 1 decimal | Con la forma de medición: acostado o de pie (RN-CRE-03) |
| Perímetro cefálico | cm, 1 decimal | Solo se ofrece hasta los 5 años (RN-CRE-01) |

Debe cargarse al menos uno de los tres valores. El sistema registra automáticamente quién cargó la
medición y desde qué perfil (familia o profesional).

**Criterios de aceptación**
- **CA1.** Dada una medición válida, cuando el usuario la guarda, entonces aparece en el
  historial y, desde I2, se muestran sus percentiles y el estado actualizado.
- **CA2.** Dada una fecha anterior al nacimiento o posterior a hoy, cuando el usuario guarda,
  entonces se rechaza con el motivo.
- **CA3.** Dado un valor fuera de los límites de plausibilidad (RN-CRE-11), cuando el usuario
  guarda, entonces se le pide confirmar ("¿Seguro que el peso es 35 kg?"); si confirma, se guarda
  marcado como valor atípico confirmado.
- **CA4.** Dado que el mismo autor ya cargó una medición en esa fecha, cuando intenta cargar
  otra, entonces se le ofrece editar la existente (RN-CRE-12).
- **CA5.** Dado un tutor sin conexión, cuando guarda una medición, entonces queda en el
  dispositivo marcada como pendiente de sincronizar y se sincroniza al recuperar conexión.
- **CA6.** Dado un perfil finalizado, cuando el usuario lo abre, entonces no se ofrece cargar
  mediciones.

### RF-CRE-02 · Editar o eliminar una medición
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** sí para el tutor

**Criterios de aceptación**
- **CA1.** Dada una medición cargada por el usuario, cuando la edita o la elimina (con
  confirmación), entonces el historial, las curvas y el estado se recalculan.
- **CA2.** Dada una medición cargada por otra persona, cuando el usuario la abre, entonces la ve
  en modo lectura, con su autor.
- **CA3.** Dada una medición incorporada al cerrar una consulta, cuando el pediatra intenta
  editarla, entonces no puede: la consulta cerrada es inalterable (RN-CON-04).

### RF-CRE-03 · Historial de mediciones
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** sí

**Criterios de aceptación**
- **CA1.** Dado un niño con mediciones, cuando el usuario abre el historial, entonces las ve de
  la más reciente a la más antigua, con fecha, edad a la fecha de medición, valores, IMC y quién
  las cargó ("Familia" o el nombre del profesional).
- **CA2.** Dado un prematuro con edad corregida aplicable, cuando se muestra la edad, entonces
  se indican ambas edades ("4 meses · 2 meses corregidos").
- **CA3.** Desde I2: dado el historial, cuando se muestra cada medición, entonces incluye
  percentil y nivel de estado por indicador, según el perfil que mira (RF-CRE-09).

### RF-CRE-04 · Cálculo del IMC
**Actores:** Sistema · **Incremento:** I2

**Criterios de aceptación**
- **CA1.** Dada una medición con peso y longitud o talla, cuando se guarda, entonces se calcula
  el IMC según RN-CRE-04 y se muestra con 1 decimal.
- **CA2.** Dada una medición sin peso o sin longitud o talla, cuando se muestra, entonces el IMC
  aparece como "—".

### RF-CRE-05 · Percentil y puntaje Z
**Actores:** Sistema · **Incremento:** I2

Para cada medición y cada indicador aplicable a la edad (RN-CRE-01), el sistema calcula el
puntaje Z y el percentil con la referencia que corresponde (RN-CRE-02) y el método de RN-CRE-05.

**Criterios de aceptación**
- **CA1.** Dados casos de control tomados de las tablas OMS y SAP (valores exactos de las
  tablas para una edad y sexo), cuando el sistema calcula, entonces obtiene el percentil
  publicado (con tolerancia de redondeo a la unidad).
- **CA2.** Dado un indicador que no aplica a la edad, cuando se muestra la medición, entonces ese
  indicador no aparece (por ejemplo, perímetro cefálico a los 7 años).
- **CA3.** Dado un prematuro, cuando se calcula, entonces se usa la edad corregida mientras
  aplique (RN-HIJ-05).
- **CA4.** Dada una medición con edad corregida negativa (antes del término), cuando se guarda,
  entonces se registra pero no se calcula percentil ni se ubica en las curvas (RN-CRE-14).

### RF-CRE-06 · Curvas de crecimiento
**Actores:** Tutor, Pediatra · **Incremento:** I2 · **Sin conexión:** sí (con datos descargados)

**Criterios de aceptación**
- **CA1.** Dado un niño con mediciones, cuando el usuario abre las curvas, entonces puede elegir
  el indicador y ve las líneas de percentil que publica la referencia correspondiente, con la
  trayectoria del niño superpuesta.
- **CA2.** Dado un niño de 5 años o más, cuando ve un indicador con datos antes y después de los
  60 meses, entonces el gráfico marca visualmente el cambio de referencia ("OMS | SAP").
  ⚕️ PVM-03
- **CA3.** Dadas mediciones de la familia y del pediatra, cuando se grafican, entonces se
  distinguen por la forma del marcador (no solo por el color) y una leyenda lo explica.
- **CA4.** Dado un punto de la trayectoria, cuando el usuario lo toca, entonces ve fecha, edad,
  valor, percentil y autor.
- **CA5.** Dado cualquier gráfico, cuando el usuario elige "Ver como tabla", entonces ve los
  mismos datos en forma de tabla (RNF-ACC-05).
- **CA6.** Dado un rango de edad amplio, cuando el usuario cambia el tramo (0–2 años, 2–5 años,
  5–15 años), entonces el eje se ajusta a ese tramo.

### RF-CRE-07 · Estado del niño
**Actores:** Sistema, Tutor, Pediatra · **Incremento:** I2 · **Sin conexión:** sí

**Criterios de aceptación**
- **CA1.** Dado un niño con al menos una medición evaluable, cuando el usuario abre el inicio o
  las curvas, entonces ve el estado de cada indicador y un resumen general, con la fecha de la
  medición en la que se basa (RN-CRE-07).
- **CA2.** Dado un estado "Consultar con el pediatra", cuando el tutor lo ve, entonces el mensaje
  sugiere consultar a su pediatra (por su nombre, si tiene uno vinculado) y no usa términos
  diagnósticos (RN-CRE-10).
- **CA3.** Dada una última medición con más de 6 meses de antigüedad, cuando se muestra el
  resumen, entonces aparece "Sin datos recientes" e invita a cargar una medición (SUP-18).
- **CA4.** Dado que el resumen pasa a "Consultar con el pediatra", cuando se guarda la medición
  que lo provoca, entonces se notifica a los tutores (RF-NOT-03, N-07).

### RF-CRE-08 · Detección de cruce de percentiles
**Actores:** Sistema · **Incremento:** I2

**Criterios de aceptación**
- **CA1.** Dadas dos mediciones consecutivas del mismo indicador cuyo percentil cruza la cantidad
  de líneas configurada (RN-CRE-08), cuando se guarda la segunda, entonces ese indicador queda en
  "A controlar" aunque el percentil esté dentro del rango esperado.
- **CA2.** Dadas dos mediciones calculadas con referencias distintas (antes y después de los 60
  meses), cuando se comparan, entonces no se evalúa cruce entre ellas.

### RF-CRE-09 · Visualización según el perfil
**Actores:** Tutor, Pediatra · **Incremento:** I2

| Información | Tutor | Pediatra |
|---|---|---|
| Valor medido e IMC | ✅ | ✅ |
| Percentil (número entero) | ✅ | ✅ |
| Puntaje Z (2 decimales) | ❌ (sí en el PDF) | ✅ |
| Nivel de estado en lenguaje simple | ✅ | ✅ |
| Clasificación técnica (por ejemplo, "talla baja") | ❌ | ✅ |
| Referencia usada (OMS o SAP) | ✅ | ✅ |

**Criterios de aceptación**
- **CA1.** Dado un tutor, cuando ve cualquier pantalla de crecimiento, entonces no aparecen el
  puntaje Z ni la clasificación técnica.
- **CA2.** Dado el pediatra de cabecera, cuando ve las mismas pantallas, entonces ve además el
  puntaje Z y la clasificación técnica.

### RF-CRE-10 · Guía de medición según la edad
**Actores:** Tutor · **Incremento:** I1

**Criterios de aceptación**
- **CA1.** Dado un niño menor de 2 años, cuando el tutor carga longitud, entonces la forma
  "acostado" viene seleccionada y se muestra una ayuda breve sobre cómo medir acostado.
- **CA2.** Dado un niño de 2 años o más, cuando el tutor carga talla, entonces la forma "de pie"
  viene seleccionada y se muestra la ayuda correspondiente.
- **CA3.** Dado un niño de 5 años o más, cuando el tutor carga talla, entonces solo se ofrece
  "de pie".

## Reglas de negocio

### RN-CRE-01 · Indicadores y referencias

| Indicador | 0 a 59 meses | 60 meses a 15 años |
|---|---|---|
| Peso para la edad | OMS 2006 | SAP ⁽¹⁾ |
| Longitud o talla para la edad | OMS 2006 | SAP |
| Perímetro cefálico para la edad | OMS 2006 | No se registra |
| Peso para la longitud o talla | OMS 2006 ⁽²⁾ | No aplica |
| IMC para la edad | OMS 2006 | ⚕️ PVM-01: tabla SAP u OMS 2007 |

- **OMS 2006**: Patrones de Crecimiento Infantil de la OMS, adoptados por el Ministerio de Salud
  de la Nación (2007) y por la SAP (2008) para niños hasta los 5 años.
- **SAP**: tablas de la *Guía para la Evaluación del Crecimiento Físico* del Comité Nacional de
  Crecimiento y Desarrollo de la SAP, transcriptas por el equipo (D-25, riesgo R-01).
- ⁽¹⁾ Si la referencia SAP no cubre algún tramo de edad para un indicador, ese indicador no se
  calcula en ese tramo. ⚕️ PVM-01
- ⁽²⁾ Peso para la longitud: 45 a 110 cm, menores de 2 años. Peso para la talla: 65 a 120 cm, de
  2 años en adelante. Fuera de esos rangos, el indicador no se calcula.
- La lista de indicadores puede ampliarse tras la validación médica (por ejemplo, velocidad de
  crecimiento o percentiles de tensión arterial). ⚕️ PVM-02

### Demás reglas

| ID | Regla |
|---|---|
| RN-CRE-02 | La referencia se elige según la edad del niño **a la fecha de la medición** (corregida si aplica): menos de 60 meses, OMS; 60 meses o más, SAP. ⚕️ PVM-03 |
| RN-CRE-03 | **Longitud y talla.** Menores de 24 meses se miden acostados (longitud); de 24 meses en adelante, de pie (talla). Si un menor de 24 meses se midió de pie, se suman 0,7 cm antes de calcular; si un niño de 24 meses o más se midió acostado, se restan 0,7 cm (criterio OMS). Se guarda el valor medido y se informa que el cálculo usa el valor ajustado. Desde los 60 meses solo se admite "de pie". |
| RN-CRE-04 | **IMC** = peso (kg) / longitud o talla (m)². Usa la longitud o talla ajustada según RN-CRE-03. Requiere peso y longitud o talla en la misma medición. |
| RN-CRE-05 | **Método de cálculo.** Cuando la referencia publica los parámetros L, M y S para el sexo y la edad, el puntaje Z es Z = ((X / M)^L − 1) / (L × S), interpolando L, M y S a la edad exacta en días. Para valores extremos se aplica el método indicado por la OMS. El percentil es la proporción de la distribución normal estándar por debajo de Z. Si una tabla SAP no publica L, M y S, el percentil se obtiene interpolando entre los percentiles publicados y el puntaje Z se informa como aproximado. ⚕️ PVM-14 |
| RN-CRE-06 | **Presentación.** El percentil se muestra como entero entre 1 y 99; por debajo se muestra "< 1" y por encima "> 99". El puntaje Z se muestra con 2 decimales. |
| RN-CRE-07 | **Estado.** Cada indicador evaluable de la última medición recibe uno de tres niveles: **Dentro de lo esperado**, **A controlar** o **Consultar con el pediatra**. Los umbrales se configuran por indicador, referencia y tramo de edad. Valores iniciales: "Consultar con el pediatra" por debajo del percentil 3 o por encima del 97 (criterio preliminar del documento v1.0); "A controlar" cuando hay cruce de percentiles (RN-CRE-08) y en los rangos que definan los pediatras. El **resumen general** es el nivel más severo entre los indicadores de la última medición. La clasificación técnica que ve el pediatra se configura con los mismos umbrales. ⚕️ PVM-04 |
| RN-CRE-08 | **Cruce de percentiles.** Se compara cada medición con la anterior del mismo indicador. Si entre ambas el percentil cruza la cantidad configurada de líneas de percentil de la referencia (valor inicial: 2), el indicador pasa a "A controlar". No se evalúa entre mediciones calculadas con referencias distintas. ⚕️ PVM-05 |
| RN-CRE-09 | Si en una misma fecha hay una medición de la familia y una del pediatra, ambas se muestran en historial y curvas, pero el estado se calcula con la del pediatra. |
| RN-CRE-10 | **Lenguaje para tutores.** Los textos del estado para tutores no usan términos diagnósticos ("obesidad", "desnutrición", "talla baja"), los configura el administrador (RF-ADM-11) y siempre incluyen el recordatorio: "Este estado orienta, no diagnostica. Ante cualquier duda, consultá con tu pediatra." ⚕️ PVM-10 |
| RN-CRE-11 | **Plausibilidad.** Si el puntaje Z de un valor queda fuera de los límites configurados, se pide confirmación antes de guardar. Valores iniciales (banderas de la OMS): peso para la edad Z < −6 o > +5; longitud o talla para la edad Z < −6 o > +6; peso para la longitud o talla, IMC y perímetro cefálico Z < −5 o > +5. Además, cada campo tiene un rango absoluto de entrada configurable. ⚕️ PVM-06 |
| RN-CRE-12 | Cada autor puede cargar como máximo una medición por niño y por fecha. |
| RN-CRE-13 | Los percentiles y estados se calculan siempre con la versión vigente de tablas y umbrales. Si el administrador publica una versión nueva, los valores mostrados se actualizan. Los informes PDF ya generados no cambian. |
| RN-CRE-14 | Las medidas al nacer se muestran como primer punto del historial y de las curvas. En un prematuro, toda medición con edad corregida negativa se registra y se muestra en el historial, pero no se calcula percentil ni se ubica en las curvas (curvas de prematuros fuera de alcance). |
