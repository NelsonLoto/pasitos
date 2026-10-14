# 16 · Eventos y línea de tiempo (`EVT`)

## Tipos de evento

| Origen | Tipo | Se genera o carga cuando… | Ícono |
|---|---|---|---|
| Automático | Medición | Se guarda una medición | Predefinido |
| Automático | Vacuna | Se registra una dosis aplicada (calendario, campaña u otra vacuna) | Predefinido |
| Automático | Control | Se cierra una consulta (solo fecha y profesional) | Predefinido |
| Automático | Cambio de pediatra | Empieza o termina un vínculo | Predefinido |
| Automático | Hito logrado | Se marca un hito del desarrollo como logrado (RF-EVT-05) | Predefinido |
| Manual | Dentición | El usuario lo carga (por ejemplo, "Primer diente") | A elección entre predefinidos |
| Manual | Salud | El usuario lo carga: enfermedad, internación u otro, de forma general (D-42) | A elección entre predefinidos |
| Manual | Momento | El usuario lo carga (por ejemplo, "Primer día de jardín") | A elección entre predefinidos |

Los eventos no admiten fotos. Cada tipo trae un ícono por defecto y los manuales permiten elegir
otro de un conjunto predefinido de íconos o emojis (D-43).

## Requerimientos funcionales

### RF-EVT-01 · Línea de tiempo
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí (lectura)

**Criterios de aceptación**
- **CA1.** Dado un niño, cuando el usuario abre la línea de tiempo, entonces ve todos sus eventos
  en orden cronológico descendente, cada uno con ícono, fecha, edad del niño en esa fecha, título
  y autor.
- **CA2.** Dada la línea de tiempo, cuando el usuario filtra por uno o varios tipos, entonces solo
  ve esos tipos.
- **CA3.** Dado un evento automático, cuando el usuario lo toca, entonces navega al registro de
  origen (medición, dosis, consulta).

### RF-EVT-02 · Eventos automáticos
**Actores:** Sistema · **Incremento:** I5

**Criterios de aceptación**
- **CA1.** Dado un registro que genera evento según la tabla de tipos, cuando se guarda, entonces
  aparece su evento en la línea de tiempo con la fecha del registro.
- **CA2.** Dado un registro de origen que se edita o elimina, cuando se guarda el cambio, entonces
  el evento se actualiza o desaparece (RN-EVT-02).

### RF-EVT-03 · Registrar un evento manual
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí para el tutor

| Dato | Obligatorio | Observaciones |
|---|---|---|
| Tipo | Sí | Dentición, Salud o Momento |
| Subtipo | Solo en Salud | Enfermedad, internación u otro |
| Fecha | Sí | RN-EVT-04 |
| Título | Sí | Hasta 60 caracteres |
| Nota | No | Hasta 280 caracteres (RN-EVT-01) |
| Ícono | No | Por defecto, el del tipo |

**Criterios de aceptación**
- **CA1.** Dados los datos obligatorios válidos, cuando el usuario guarda, entonces el evento
  aparece en la línea de tiempo con su autor.
- **CA2.** Dado un tutor sin conexión, cuando guarda un evento, entonces queda pendiente de
  sincronizar (RNF-OFF-02).

### RF-EVT-04 · Editar o eliminar un evento manual
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí para el tutor

**Criterios de aceptación**
- **CA1.** Dado un evento manual propio, cuando el usuario lo edita o elimina (con confirmación),
  entonces la línea de tiempo se actualiza.
- **CA2.** Dado un evento de otra persona o automático, cuando el usuario lo abre, entonces no ve
  opciones de edición.

### RF-EVT-05 · Hitos del desarrollo
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí para el tutor

El administrador gestiona un catálogo de hitos con su edad esperada y su edad límite
(RF-ADM-09).

**Criterios de aceptación**
- **CA1.** Dado un niño, cuando el usuario abre Desarrollo, entonces ve los hitos de su tramo de
  edad (edad corregida si aplica) y los ya logrados, con su fecha.
- **CA2.** Dado un hito, cuando el usuario lo marca como logrado e indica la fecha (por defecto,
  hoy), entonces se genera el evento "Hito logrado".
- **CA3.** Dado un hito no logrado cuando el niño supera su edad límite, cuando el tutor abre
  Desarrollo, entonces ve junto al hito el mensaje "Comentalo en el próximo control" (RN-EVT-05).
- **CA4.** Dado un hito marcado por error, cuando su autor lo desmarca, entonces el evento
  desaparece.

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-EVT-01 | Los eventos de salud se registran de forma general: tipo, subtipo, fecha, título breve y nota opcional de hasta 280 caracteres. No tienen campos clínicos estructurados (diagnóstico, medicación, dosis) (D-42). |
| RN-EVT-02 | Los eventos automáticos no se editan directamente: reflejan su registro de origen. |
| RN-EVT-03 | Todos los eventos son visibles para los tutores y el pediatra de cabecera. El evento "Control" muestra solo fecha y profesional a quien no tiene acceso al contenido de esa consulta. |
| RN-EVT-04 | La fecha de un evento no puede ser anterior al nacimiento ni posterior a hoy. |
| RN-EVT-05 | **Hitos.** El catálogo, la edad esperada y la edad límite de cada hito los define el administrador con validación médica. Que un hito no se haya logrado nunca genera alertas ni notificaciones: solo el mensaje "Comentalo en el próximo control". La evaluación formal del desarrollo la hace el pediatra en la consulta (RF-CON-05). ⚕️ PVM-08 |
| RN-EVT-06 | Un perfil finalizado no admite eventos nuevos. |
