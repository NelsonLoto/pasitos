# 15 · Consulta guiada (`CON`)

La consulta guiada ordena la carga de datos del control del niño sano para agilizar la atención.
Cada consulta cerrada forma parte del **registro de controles** del pediatra.

> **El registro de controles no es una historia clínica.** Es un registro complementario del
> pediatra, no reemplaza la historia clínica de su consultorio ni cumple los requisitos de la
> Ley 27.706 (firma digital, interoperabilidad). La app lo indica en la pantalla de la consulta y
> en toda exportación (D-16).

## Pasos

Los pasos se presentan **siempre en el mismo orden** para que el pediatra organice su tiempo de
consulta. Los que no corresponden a la edad del niño se omiten sin cambiar el orden de los demás
(D-33).

| # | Paso | Se muestra | Obligatorio | Contenido |
|---|---|---|---|---|
| 1 | Datos de la consulta | Siempre | Sí | Fecha (por defecto hoy; admite fechas pasadas, SUP-16), tipo (control programado o consulta extra), acompañante (madre, padre, otro tutor, otro) |
| 2 | Medidas | Siempre | Sí: al menos un valor | Peso, longitud o talla con forma de medición, perímetro cefálico hasta los 5 años. IMC, percentiles, puntaje Z y clasificación técnica calculados en el momento (RF-CON-03) |
| 3 | Signos vitales | Desde los 3 años | No | Tensión arterial sistólica y diastólica (mmHg). Sin cálculo de percentil (SUP-09) |
| 4 | Alimentación | Siempre | No | Menores de 2 años: lactancia (exclusiva, mixta, fórmula, no recibe) e inicio de alimentación complementaria. Mayores: comentario breve |
| 5 | Desarrollo | Hasta los 6 años | No | Hitos esperados para la edad (RF-CON-05) y resultado de la prueba de pesquisa si se aplicó |
| 6 | Pesquisas | Según edad ⚕️ PVM-09 | No | Audición, visión, cadera, salud bucal (RF-CON-06) |
| 7 | Vacunas | Siempre | No | Estado del calendario y registro de dosis aplicadas en la consulta (RF-CON-04) |
| 8 | Examen físico | Siempre | No | "Normal" o "Con hallazgos" con descripción breve |
| 9 | Indicaciones y próximo control | Siempre | No | Indicaciones para la familia (texto) y fecha del próximo control (RF-CON-07) |
| 10 | Observaciones | Siempre | No | Texto privado del pediatra (RF-CON-08) |

Agregar, quitar o reordenar pasos es un cambio de esta SPEC, no una configuración (D-35).

## Requerimientos funcionales

### RF-CON-01 · Iniciar una consulta
**Actores:** Pediatra · **Incremento:** I4 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un paciente vinculado o propio, cuando el pediatra elige "Nueva consulta",
  entonces se abre el paso 1 con la fecha de hoy.
- **CA2.** Dado un niño con el que el pediatra no tiene vínculo activo, cuando intenta iniciar una
  consulta, entonces no puede.
- **CA3.** Dado un paciente con una consulta en borrador del mismo pediatra, cuando elige "Nueva
  consulta", entonces se le ofrece retomar el borrador o descartarlo.
- **CA4.** Dado un pediatra sin conexión, cuando intenta iniciar una consulta, entonces se le
  informa que necesita conexión.

### RF-CON-02 · Navegación entre pasos
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dado un niño de 8 meses, cuando el pediatra recorre la consulta, entonces no ve el
  paso 3 y los demás mantienen su número y su orden.
- **CA2.** Dado cualquier paso no obligatorio, cuando el pediatra elige "Omitir", entonces avanza
  al siguiente y el paso queda marcado como omitido.
- **CA3.** Dada la consulta, cuando el pediatra abre el índice de pasos, entonces ve cuáles
  completó, cuáles omitió y cuáles faltan, y puede saltar a cualquiera.
- **CA4.** Dado el paso 2 sin ningún valor, cuando el pediatra intenta cerrar la consulta,
  entonces se le indica que el paso de medidas es obligatorio.

### RF-CON-03 · Paso de medidas
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dados valores cargados en el paso, cuando el pediatra completa un campo, entonces ve en
  el momento el percentil, el puntaje Z, la clasificación técnica y la comparación con la medición
  anterior del niño.
- **CA2.** Dados valores fuera de los límites de plausibilidad, cuando el pediatra avanza,
  entonces se aplica la confirmación de RN-CRE-11.

### RF-CON-04 · Paso de vacunas
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dado el paso, cuando se abre, entonces muestra primero las dosis atrasadas, pendientes
  y próximas del niño.
- **CA2.** Dada una dosis aplicada en la consulta, cuando el pediatra la registra, entonces queda
  con la fecha de la consulta por defecto y se aplican las reglas de RF-VAC-02.

### RF-CON-05 · Paso de desarrollo
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dado un niño menor de 6 años, cuando se abre el paso, entonces se listan los hitos
  esperados para su edad (catálogo de RF-ADM-09) con lo que marcó la familia, y el pediatra puede
  marcar cada uno como logrado o no logrado.
- **CA2.** Dado el paso, cuando el pediatra registra una prueba de pesquisa del desarrollo,
  entonces elige la prueba (PRUNAPE, IODI u otra) y el resultado (pasa o no pasa).
- **CA3.** Dado un hito que el pediatra marca como logrado, cuando se cierra la consulta,
  entonces se registra como hito logrado por el pediatra (RF-EVT-05).

### RF-CON-06 · Paso de pesquisas
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dado el paso, cuando se abre, entonces muestra solo las pesquisas que corresponden a la
  edad del niño según la configuración vigente (⚕️ PVM-09). Cada una admite: normal, alterada (a
  derivar) o no evaluada, y una nota breve opcional.

### RF-CON-07 · Indicaciones para la familia y próximo control
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dado el paso 9, cuando se abre, entonces la fecha del próximo control viene sugerida
  según el cronograma de controles para la edad del niño (RN-NOT-02), y el pediatra puede
  cambiarla.
- **CA2.** Dadas indicaciones escritas, cuando se cierra la consulta, entonces los tutores las ven
  en el detalle de la consulta y reciben una notificación (N-06).
- **CA3.** Dada una fecha de próximo control, cuando se cierra la consulta, entonces esa fecha
  reemplaza a la del cronograma para el recordatorio (N-01).

### RF-CON-08 · Observaciones privadas
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dado el paso 10, cuando el pediatra escribe observaciones, entonces se le indica con un
  ícono y un texto que solo él puede verlas.
- **CA2.** Dada una consulta cerrada con observaciones, cuando cualquier otro usuario (tutor,
  otro pediatra, administrador) accede a la consulta, entonces las observaciones no se muestran
  ni se envían al dispositivo de ese usuario.

### RF-CON-09 · Borrador de consulta
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dada una consulta en curso, cuando el pediatra sale de ella o la app se cierra,
  entonces lo cargado queda guardado como borrador.
- **CA2.** Dado un borrador, cuando el pediatra lo retoma, entonces vuelve al último paso visitado
  con todo lo cargado.
- **CA3.** Dado un borrador, cuando el pediatra lo descarta (con confirmación), entonces se elimina
  y nada de lo cargado se incorpora al historial.
- **CA4.** Dado un borrador, cuando un tutor mira el historial del niño, entonces no ve nada de lo
  cargado en él (SUP-08).

### RF-CON-10 · Cerrar la consulta
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dada una consulta con el paso 2 completo, cuando el pediatra la cierra, entonces ve un
  resumen y debe confirmar.
- **CA2.** Dada la confirmación, cuando se cierra, entonces la consulta queda inalterable
  (RN-CON-04), las mediciones, vacunas e hitos se incorporan al historial del niño con autoría del
  pediatra, y se genera el evento "Control con [pediatra]" en la línea de tiempo.

### RF-CON-11 · Nota aclaratoria
**Actores:** Pediatra · **Incremento:** I4

**Criterios de aceptación**
- **CA1.** Dada una consulta cerrada propia, cuando el pediatra agrega una nota aclaratoria,
  entonces la nota queda fechada, debajo del contenido original, sin modificarlo.
- **CA2.** Dada una nota aclaratoria, cuando el pediatra elige su visibilidad, entonces puede ser
  privada (como las observaciones) o visible para la familia (como las indicaciones).

### RF-CON-12 · Ver consultas
**Actores:** Pediatra, Tutor · **Incremento:** I4 · **Sin conexión:** sí (lectura)

**Criterios de aceptación**
- **CA1.** Dado el pediatra, cuando abre las consultas de un paciente, entonces ve completas las
  propias y, de las de otros pediatras, solo fecha y profesional (RN-PED-05).
- **CA2.** Dado un tutor, cuando abre una consulta, entonces ve fecha, profesional, tipo, datos
  objetivos cargados (medidas, vacunas, hitos, resultados de pesquisas y examen físico como
  "normal" o "con hallazgos"), indicaciones para la familia, notas aclaratorias visibles y fecha
  del próximo control. No ve observaciones (RN-CON-02).

### RF-CON-13 · Exportar el registro de controles
**Actores:** Pediatra · **Incremento:** I6 · **Sin conexión:** no

Ver RF-INF-03.

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-CON-01 | Solo el pediatra de cabecera puede iniciar consultas sobre un niño. |
| RN-CON-02 | **Visibilidad.** Las observaciones (paso 10) y las notas aclaratorias privadas son visibles solo para el pediatra autor. El resto de la consulta cerrada es visible para los tutores. Otros pediatras ven solo fecha y profesional. Del examen físico (paso 8) y de las pesquisas (paso 6), el tutor ve el resultado pero no la descripción ni las notas del pediatra (SUP-21). |
| RN-CON-03 | Lo cargado en un borrador no se incorpora al historial del niño ni es visible para otros hasta que la consulta se cierra (SUP-08). |
| RN-CON-04 | Una consulta cerrada es **inalterable**: no se edita ni se elimina, y los registros que incorporó tampoco. Solo admite notas aclaratorias del mismo pediatra (D-16). |
| RN-CON-05 | Si el vínculo termina mientras hay un borrador, el borrador se descarta. |
| RN-CON-06 | Los textos de indicaciones y observaciones admiten hasta 2.000 caracteres cada uno (valor propuesto). |
