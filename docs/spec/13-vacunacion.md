# 13 · Vacunación (`VAC`)

El calendario de referencia es el **Calendario Nacional de Vacunación** vigente del Ministerio de
Salud de la Nación. Su contenido (vacunas, dosis, edades) lo gestiona el administrador
(RF-ADM-06) y puede actualizarse sin publicar una versión nueva de la app.

Para I1 se carga el **Calendario Nacional de Vacunación 2026** para niños y adolescentes: las 27
dosis con edad fija, transcriptas de los documentos oficiales del Ministerio de Salud
([calendario](https://www.argentina.gob.ar/sites/default/files/2026-03-10_calendario_nacional_vacunacion_70x50_web.pdf)
y [carnet](https://www.argentina.gob.ar/sites/default/files/2026-03-10_or_carnet_unificado_vacunacion_ninos-adolescentes.pdf),
10/03/2026). La antigripal (anual), las vacunas regionales (fiebre amarilla y fiebre hemorrágica
argentina) y los esquemas de "iniciar o completar" no forman parte del calendario: se registran
como otras vacunas (RF-VAC-05). D-68. ⚕️ PVM-15

## Requerimientos funcionales

### RF-VAC-01 · Calendario del niño con estados
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** sí (lectura)

**Criterios de aceptación**
- **CA1.** Dado un niño, cuando el usuario abre Vacunas, entonces ve las dosis del calendario
  agrupadas por edad indicada ("Al nacer", "2 meses", …), cada una con su estado (RN-VAC-01)
  representado con ícono y texto.
- **CA2.** Dado un niño con dosis atrasadas o próximas, cuando abre Vacunas, entonces arriba ve
  un resumen ("2 atrasadas · 1 próxima").
- **CA3.** Dado el calendario, cuando el usuario filtra por estado, entonces solo ve las dosis en
  ese estado.
- **CA4.** Dado un niño con "otras vacunas" o dosis de campañas, cuando abre Vacunas, entonces
  las ve en secciones separadas del calendario.

### RF-VAC-02 · Registrar una dosis aplicada
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** no

| Dato | Obligatorio | Observaciones |
|---|---|---|
| Fecha de aplicación | Sí, salvo carga retroactiva (RF-VAC-04) | RN-VAC-02 |
| Lote | No | La app recomienda cargarlo: "Figura en el carnet de vacunación" |
| Vacunatorio o lugar | No | Texto libre breve |

**Criterios de aceptación**
- **CA1.** Dada una dosis no aplicada, cuando el usuario la registra con datos válidos, entonces
  pasa a "Aplicada", se registra su autor y aparece en la línea de tiempo (RF-EVT-02).
- **CA2.** Dada una fecha anterior al nacimiento o posterior a hoy, cuando el usuario guarda,
  entonces se rechaza con el motivo.
- **CA3.** Dado que el usuario no cargó el lote, cuando guarda, entonces se guarda igual, sin
  bloquear ni insistir.
- **CA4.** Dada una dosis ya aplicada, cuando el usuario intenta registrarla de nuevo, entonces
  se le ofrece ver o editar el registro existente (RN-VAC-09).

### RF-VAC-03 · Editar o eliminar una dosis
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dada una dosis registrada por el usuario, cuando la edita o elimina (con
  confirmación), entonces el estado se recalcula.
- **CA2.** Dada una dosis registrada por otra persona, cuando el usuario la abre, entonces la ve
  en modo lectura, con su autor.

### RF-VAC-04 · Carga retroactiva de dosis
**Actores:** Tutor · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un niño recién dado de alta que por su edad ya debería tener dosis aplicadas,
  cuando el tutor ve la pregunta "¿Tiene las vacunas al día para su edad?" y responde que sí,
  entonces todas las dosis con edad indicada anterior a hoy quedan como aplicadas con "fecha no
  registrada" (RN-VAC-04).
- **CA2.** Dada la misma pregunta, cuando el tutor responde "No" o "No sé", entonces ve la lista
  de esas dosis y puede marcar una por una las que se aplicaron.
- **CA3.** Dada una dosis aplicada sin fecha, cuando el tutor la abre más tarde, entonces puede
  completar fecha, lote y lugar.
- **CA4.** Dado un niño ya existente, cuando el tutor elige "Marcar dosis anteriores" en Vacunas,
  entonces accede al mismo flujo.

### RF-VAC-05 · Registrar otra vacuna
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** no

Permite registrar vacunas que no están en el Calendario Nacional (por ejemplo, aplicadas en forma
privada): nombre, fecha, lote (opcional) y una nota breve (opcional).

**Criterios de aceptación**
- **CA1.** Dados un nombre y una fecha válidos, cuando el usuario guarda, entonces la vacuna
  aparece en "Otras vacunas" y en la línea de tiempo, sin estado calculado (RN-VAC-06).

### RF-VAC-06 · Campañas de vacunación
**Actores:** Sistema, Tutor, Pediatra · **Incremento:** I6 · **Sin conexión:** sí (lectura)

El administrador carga campañas (por ejemplo, antigripal) con vacuna, rango de edad destinatario
y fechas de inicio y fin (RF-ADM-07).

**Criterios de aceptación**
- **CA1.** Dada una campaña vigente cuyo rango de edad incluye al niño, cuando el usuario abre
  Vacunas, entonces ve la campaña en su sección, con sus fechas, y puede registrar la dosis como
  aplicada.
- **CA2.** Dada una campaña terminada sin dosis registrada, cuando el usuario abre Vacunas,
  entonces la campaña deja de mostrarse y no se marca como atrasada.

### RF-VAC-07 · Detalle de una vacuna
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** sí

**Criterios de aceptación**
- **CA1.** Dada una dosis del calendario, cuando el usuario la abre, entonces ve el nombre de la
  vacuna, qué previene, la edad indicada, el estado, los datos de aplicación si los hay, y la
  fuente de la información.

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-VAC-01 | **Estados de una dosis.** Cada dosis tiene una *fecha indicada* (fecha de nacimiento + edad indicada) y una *tolerancia* en días. **Aplicada**: tiene registro de aplicación. **Próxima**: no aplicada y faltan 7 días o menos para la fecha indicada. **Pendiente**: no aplicada, desde la fecha indicada hasta el fin de la tolerancia. **Atrasada**: no aplicada y pasó la tolerancia. Las dosis cuya fecha indicada está a más de 7 días se muestran como **Programada**, con su fecha, como dato informativo (SUP-19). La anticipación y la tolerancia por dosis las configura el administrador; tolerancia inicial propuesta: 30 días. En las dosis que el calendario oficial indica con una ventana, la tolerancia la cubre: 120 días en las de "15 a 18 meses" (hasta los 18 meses, más 30) y 365 en las de 5 y 11 años ("en el año en que cumplen"). La edad indicada se expresa en meses, como en el calendario oficial. ⚕️ PVM-12 |
| RN-VAC-02 | La fecha de aplicación no puede ser anterior al nacimiento ni posterior a hoy. |
| RN-VAC-03 | **Versiones del calendario.** Cada versión tiene fecha de vigencia. Las dosis aplicadas se conservan tal como se registraron y se siguen mostrando aunque la versión vigente ya no incluya esa dosis. Las dosis no aplicadas se recalculan con la versión vigente (D-41). |
| RN-VAC-04 | Una dosis "aplicada con fecha no registrada" cuenta como aplicada para todos los cálculos y se muestra con esa leyenda. |
| RN-VAC-05 | La app no calcula esquemas de recuperación. Ante una dosis atrasada muestra: "Esta dosis está atrasada. Consultá con tu pediatra o en el vacunatorio cómo completar el esquema." |
| RN-VAC-06 | Las "otras vacunas" no tienen estado ni generan notificaciones. |
| RN-VAC-07 | Los esquemas para grupos de riesgo están fuera de alcance. |
| RN-VAC-08 | El calendario se calcula con la **edad cronológica**, también en prematuros. |
| RN-VAC-09 | Cada dosis del calendario puede registrarse una sola vez por niño. |
| RN-VAC-10 | **Dosis por cohorte.** Una dosis puede corresponder solo a los nacidos en un rango de fechas, con una edad indicada propia para ese rango. Por ejemplo, la 2ª dosis de triple viral: a los 15-18 meses para los nacidos desde el 01/07/2024, y a los 5 años para los nacidos antes. Los rangos de una misma dosis no se superponen. ⚕️ PVM-15 |
