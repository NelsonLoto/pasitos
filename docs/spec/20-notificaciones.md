# 20 · Notificaciones (`NOT`)

## Tipos de notificación

| ID | Destinatario | Disparador | Momento | Texto de ejemplo |
|---|---|---|---|---|
| N-01 | Tutores | Próximo control | 3 días antes de la fecha (RN-NOT-02) | "Se acerca el control de Sofi: el jueves 23/10." |
| N-02 | Tutores | Dosis próxima | 7 días antes de la fecha indicada | "Sofi tiene una vacuna próxima. Revisá el calendario." |
| N-03 | Tutores | Dosis atrasada | Cuando la dosis pasa a "Atrasada" | "Sofi tiene una vacuna atrasada. Consultá con tu pediatra o en el vacunatorio." |
| N-04 | Tutores | Solicitud al pediatra aceptada, rechazada o vencida | Al ocurrir | "La Dra. Pérez aceptó ser la pediatra de Sofi." |
| N-05 | Tutor invitado | Invitación de otro tutor o de un pediatra a reclamar un perfil | Al ocurrir | "Te invitaron a seguir a Sofi en Pasitos." |
| N-06 | Tutores | Consulta cerrada con indicaciones para la familia | Al cerrarse | "La Dra. Pérez dejó indicaciones del control de Sofi." |
| N-07 | Tutores | El resumen del estado pasa a "Consultar con el pediatra" | Al guardarse la medición | "Hay novedades en el seguimiento de Sofi. Abrí Pasitos para verlas." |
| N-08 | Tutores | Consejos nuevos para la edad del niño | Como máximo uno por semana por niño | "Hay consejos nuevos para la edad de Sofi." |
| N-09 | Tutores | Novedades de tutores y vínculos: invitación aceptada, rechazada o vencida; tutor quitado; rol transferido; vínculo finalizado por el pediatra o el sistema; perfil finalizado | Al ocurrir | "Martín aceptó la invitación para seguir a Sofi." |
| N-10 | Pediatra | Nueva solicitud de vínculo | Al ocurrir | "Tenés una nueva solicitud de paciente." |
| N-11 | Pediatra | Vínculo finalizado (revocación, cambio de pediatra, eliminación del niño) o perfil reclamado por un tutor | Al ocurrir | "Un paciente dejó de estar vinculado con vos." |
| N-12 | Pediatra | Perfil profesional validado o rechazado | Al ocurrir | "Tu matrícula fue validada. Ya podés recibir pacientes." |
| N-13 | Administrador | Pediatra pendiente de validación | Al ocurrir, solo en el panel web | — |

## Requerimientos funcionales

### RF-NOT-01 · Notificaciones push y centro de notificaciones
**Actores:** Tutor, Pediatra · **Incremento:** I6 · **Sin conexión:** sí (lectura del centro)

**Criterios de aceptación**
- **CA1.** Dada una notificación generada, cuando el usuario tiene ese tipo activado, entonces
  recibe una notificación push en el teléfono.
- **CA2.** Dada cualquier notificación generada, cuando el usuario abre el centro de
  notificaciones, entonces la ve listada (aunque tenga desactivado el push de ese tipo), con
  fecha, marcada como no leída hasta abrirla.
- **CA3.** Dada una notificación, cuando el usuario la toca, entonces la app abre la pantalla
  relacionada, en el modo (tutor o pediatra) y con el hijo activo que corresponde.

### RF-NOT-02 · Preferencias de notificación
**Actores:** Tutor, Pediatra · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dada la configuración, cuando el usuario abre Notificaciones, entonces ve los tipos de
  su perfil con un interruptor cada uno, todos activados por defecto.
- **CA2.** Dado un tipo desactivado, cuando se genera una notificación de ese tipo, entonces no
  llega el push, pero aparece en el centro de notificaciones.
- **CA3.** Dadas las anticipaciones de N-01 y N-02, cuando el usuario abre la configuración,
  entonces se muestran como información, sin poder cambiarlas (D-50).

### RF-NOT-03 · Generación de notificaciones
**Actores:** Sistema · **Incremento:** I6

**Criterios de aceptación**
- **CA1.** Dado cada disparador de la tabla de tipos, cuando ocurre, entonces se genera la
  notificación para los destinatarios indicados, una sola vez por evento.
- **CA2.** Dado un perfil finalizado, cuando llega la fecha de un control o una dosis, entonces no
  se genera notificación (RN-NOT-06).
- **CA3.** Dado un niño con varios tutores, cuando se genera una notificación para tutores,
  entonces la reciben todos.

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-NOT-01 | Si una cuenta tiene ambos perfiles, recibe las notificaciones de los dos; cada una indica a qué modo pertenece. |
| RN-NOT-02 | **Fecha del próximo control.** Si el pediatra fijó una fecha en la última consulta cerrada, se usa esa (D-50). Si no, es la siguiente edad del cronograma de controles posterior a hoy. Cronograma inicial (basado en el *Cronograma de controles en salud para niños* del Ministerio de Salud, 2025): a los 5 días de vida (48 a 72 h desde la llegada al hogar), entre los 7 y 14 días, al mes, mensual hasta los 12 meses, cada 3 meses de 1 a 2 años, cada 4 meses de 2 a 3 años, cada 6 meses de 3 a 5 años y anual de 5 a 15 años. Lo gestiona el administrador (RF-ADM-10). ⚕️ PVM-13 |
| RN-NOT-03 | Las anticipaciones son fijas: 3 días para controles y 7 días para vacunas. |
| RN-NOT-04 | **Privacidad.** El texto de una notificación push puede incluir el nombre de pila del niño, pero nunca valores, percentiles, estados ni datos clínicos, porque puede verse con la pantalla bloqueada. |
| RN-NOT-05 | Se envía como máximo una notificación N-08 por semana por niño. |
| RN-NOT-06 | No se generan notificaciones para perfiles finalizados ni por registros de hábitos. |
| RN-NOT-07 | Los emails transaccionales (verificación, recuperación de contraseña, invitaciones) no son notificaciones configurables: se envían siempre. |
