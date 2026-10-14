# 11 · Hijos y tutores (`HIJ`)

## Requerimientos funcionales

### RF-HIJ-01 · Alta de hijo
**Actores:** Tutor · **Incremento:** I1 · **Sin conexión:** no

El tutor da de alta a un hijo con estos datos:

| Dato | Obligatorio | Observaciones |
|---|---|---|
| Nombre y apellido | Sí | |
| Sexo | Sí | Femenino o masculino (RN-HIJ-02) |
| Fecha de nacimiento | Sí | RN-HIJ-01 |
| Semanas de gestación al nacer | Sí | Admite "No lo sé" (RN-HIJ-03) |
| Peso, longitud y perímetro cefálico al nacer | No | Recomendados; la app explica para qué sirven |
| Avatar | No | Se elige entre avatares predefinidos; no se suben fotos (RN-HIJ-11) |
| Declaración de representación | Sí | "Soy madre, padre o representante legal de este niño o niña" |

Quien da de alta al niño queda como su **tutor principal**.

**Criterios de aceptación**
- **CA1.** Dados todos los datos obligatorios válidos y la declaración marcada, cuando el tutor
  confirma, entonces se crea el perfil y queda seleccionado como hijo activo.
- **CA2.** Dado un niño que por su edad ya debería tener dosis aplicadas del Calendario Nacional,
  cuando se crea el perfil, entonces se ofrece la carga retroactiva de vacunas (RF-VAC-04).
- **CA3.** Dada una fecha de nacimiento futura o que corresponde a 15 años o más, cuando el tutor
  confirma, entonces se rechaza con un mensaje que explica el motivo.
- **CA4.** Dadas medidas al nacer fuera de los límites de plausibilidad (RN-CRE-11), cuando el
  tutor confirma, entonces se le pide revisar el valor antes de guardarlo.
- **CA5.** Dado que el tutor elige "No lo sé" en semanas de gestación, cuando confirma, entonces
  el niño se considera nacido a término (RN-HIJ-03).
- **CA6.** Dado que el alta se completó (desde I3), cuando el tutor ve la confirmación, entonces
  se le ofrece vincular a su pediatra (RF-PED-03).

### RF-HIJ-02 · Edición del perfil
**Actores:** Tutor · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un cambio de fecha de nacimiento, sexo o semanas de gestación, cuando el tutor
  confirma, entonces se le advierte que cambiarán los percentiles, los estados y el calendario de
  vacunas, y debe confirmar de nuevo.
- **CA2.** Dado el cambio confirmado, cuando el tutor vuelve a ver el seguimiento, entonces todos
  los cálculos reflejan los datos nuevos.
- **CA3.** Dado un cambio en el perfil, cuando se guarda, entonces los demás tutores y el
  pediatra de cabecera ven los datos actualizados.

### RF-HIJ-03 · Alergias y antecedentes
**Actores:** Tutor · **Incremento:** I1 · **Sin conexión:** no

El perfil incluye dos campos de texto breve y opcionales: **alergias** y **antecedentes
relevantes**. Se registran de forma general, sin estructura clínica (D-42).

**Criterios de aceptación**
- **CA1.** Dado el perfil de un niño, cuando el tutor completa alergias o antecedentes, entonces
  se muestran en el perfil, al pediatra de cabecera y en el informe PDF.
- **CA2.** Dado un texto que supera el límite de RN-HIJ-10, cuando el tutor intenta guardarlo,
  entonces se le indica la cantidad máxima de caracteres.

### RF-HIJ-04 · Selector de hijo activo
**Actores:** Tutor · **Incremento:** I1 · **Sin conexión:** sí

**Criterios de aceptación**
- **CA1.** Dado un tutor con más de un hijo, cuando abre el selector, entonces ve a cada hijo con
  su avatar, nombre y edad, y al elegir uno toda la app pasa a mostrar sus datos.
- **CA2.** Dado un tutor con un solo hijo, cuando usa la app, entonces el selector solo ofrece
  agregar otro hijo.
- **CA3.** Dado un tutor que cierra y vuelve a abrir la app, cuando ingresa, entonces el hijo
  activo es el último que eligió.

### RF-HIJ-05 · Invitar a otro tutor
**Actores:** Tutor principal · **Incremento:** I1 · **Sin conexión:** no

El tutor principal invita por email a otra persona a ser tutor del niño.

**Criterios de aceptación**
- **CA1.** Dado un email que pertenece a una cuenta, cuando el principal envía la invitación,
  entonces el invitado recibe una notificación en la app y un email.
- **CA2.** Dado un email sin cuenta, cuando el principal envía la invitación, entonces el
  invitado recibe un email para registrarse; al registrarse con ese email ve la invitación.
- **CA3.** Dada una invitación pendiente, cuando pasan 15 días sin respuesta, entonces vence
  (RN-HIJ-07) y el principal puede reenviarla.
- **CA4.** Dado un email que ya es tutor del niño, cuando el principal intenta invitarlo,
  entonces se le informa que ya es tutor.

### RF-HIJ-06 · Aceptar o rechazar la invitación de tutor
**Actores:** Tutor (invitado) · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dada una invitación vigente, cuando el invitado la acepta y marca la declaración de
  representación, entonces pasa a ser tutor secundario del niño y el principal recibe una
  notificación.
- **CA2.** Dada una invitación vigente, cuando el invitado la rechaza, entonces el principal
  recibe una notificación y la invitación queda cerrada.
- **CA3.** Dada una invitación vencida, cuando el invitado intenta aceptarla, entonces se le
  informa que venció y que debe pedir una nueva.

### RF-HIJ-07 · Quitar un tutor o abandonar el perfil
**Actores:** Tutor principal, Tutor secundario · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un tutor secundario, cuando el principal lo quita, entonces deja de ver al niño y
  recibe una notificación.
- **CA2.** Dado un tutor secundario, cuando abandona el perfil, entonces deja de ver al niño y el
  principal recibe una notificación.
- **CA3.** Dado cualquier caso anterior, cuando el tutor deja de serlo, entonces los registros
  que cargó permanecen en el historial del niño con su autoría.

### RF-HIJ-08 · Transferir el rol de tutor principal
**Actores:** Tutor principal · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un niño con al menos un tutor secundario, cuando el principal elige a uno y
  confirma, entonces ese tutor pasa a ser principal, el anterior pasa a secundario y ambos
  reciben una notificación.

### RF-HIJ-09 · Eliminar un hijo
**Actores:** Tutor · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un tutor que es el único tutor del niño, cuando elige eliminarlo, entonces se le
  muestran las consecuencias (RN-HIJ-08), debe confirmar escribiendo el nombre del niño, y el
  perfil se elimina.
- **CA2.** Dado un niño con otros tutores, cuando un tutor elige eliminarlo, entonces se le
  informa que el perfil seguirá existiendo para los demás tutores y que él dejará de ser tutor;
  si era principal, el rol pasa al tutor secundario más antiguo.
- **CA3.** Dado un niño con pediatra de cabecera, cuando el perfil se elimina, entonces el
  vínculo termina y el pediatra recibe una notificación.

### RF-HIJ-10 · Perfil finalizado a los 15 años
**Actores:** Sistema · **Incremento:** I6 · **Sin conexión:** —

**Criterios de aceptación**
- **CA1.** Dado un niño que cumple 15 años, cuando el sistema procesa la fecha, entonces el perfil
  pasa a *finalizado* (RN-HIJ-09) y los tutores reciben un aviso.
- **CA2.** Dado un perfil finalizado, cuando un tutor lo abre, entonces puede consultar todo el
  historial y generar el informe PDF, pero no ve opciones de carga.
- **CA3.** Dado un perfil finalizado, cuando llega una fecha de control o vacuna, entonces no se
  envían notificaciones.

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-HIJ-01 | Al darse de alta, la fecha de nacimiento no puede ser futura y el niño debe tener menos de 15 años. |
| RN-HIJ-02 | El sexo se registra como femenino o masculino porque las tablas de referencia de crecimiento están separadas por sexo. Es obligatorio. |
| RN-HIJ-03 | Semanas de gestación: número entero entre 22 y 42. "No lo sé" equivale a 40 semanas (SUP-15). Un niño nacido con menos de 37 semanas se considera **prematuro**. |
| RN-HIJ-04 | **Edad cronológica**: tiempo transcurrido desde la fecha de nacimiento. Se muestra en días durante el primer mes, en meses y días hasta los 2 años, y en años y meses después. |
| RN-HIJ-05 | **Edad corregida** (solo prematuros): edad cronológica menos (40 − semanas de gestación) semanas. Se usa en lugar de la cronológica para crecimiento, hitos y consejos hasta que el niño cumple 24 meses de edad cronológica. ⚕️ PVM-07 |
| RN-HIJ-06 | Cada niño tiene exactamente un tutor principal y cualquier cantidad de tutores secundarios. |
| RN-HIJ-07 | Las invitaciones a tutores vencen a los 15 días del envío. Reenviar genera una invitación nueva con un plazo nuevo (SUP-03). |
| RN-HIJ-08 | Al eliminarse el perfil de un niño se eliminan sus mediciones, vacunas, eventos, hábitos, hitos, invitaciones y vínculos. Las consultas cerradas se conservan en el registro del pediatra que las hizo (RN-CTA-09). |
| RN-HIJ-09 | Un perfil **finalizado** no admite cargas ni modificaciones, no genera notificaciones y termina su vínculo con el pediatra (que conserva sus consultas, como un pediatra anterior). Se puede consultar, exportar y eliminar. |
| RN-HIJ-10 | Alergias y antecedentes: máximo 280 caracteres cada uno (valor propuesto). |
| RN-HIJ-11 | El avatar del niño se elige entre un conjunto predefinido de ilustraciones. No se almacenan fotos (SUP-12). |
