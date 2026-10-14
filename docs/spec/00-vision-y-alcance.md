# 00 · Visión y alcance

## 1. Contexto

En Argentina, el **control del niño sano** es la consulta pediátrica periódica que acompaña el
crecimiento y el desarrollo de niños y niñas sin estar motivada por una enfermedad. Su frecuencia
la define el Ministerio de Salud de la Nación (mensual en el primer año, más espaciada después) e
incluye la evaluación del crecimiento contra curvas de referencia, el seguimiento del Calendario
Nacional de Vacunación, la evaluación del desarrollo y pesquisas según la edad.

Hoy esa información vive en la libreta de salud en papel y en la historia clínica de cada
consultorio. Los tutores no tienen una forma simple de seguir la evolución entre consultas, y el
pediatra recibe datos incompletos cuando la familia cambia de profesional o mide al niño en casa.

Pasitos se desarrolla como **tesis** por un equipo de 4 personas, con encuentros quincenales de
seguimiento con el profesor a cargo. Se prevé una **prueba piloto con datos reales**.

## 2. Objetivo

Brindar a tutores y pediatras una herramienta móvil para registrar y seguir el crecimiento de
niños y niñas de **0 a 15 años**, que:

1. Calcule percentiles, puntaje Z, IMC y un **estado** del niño según las referencias adoptadas en
   Argentina: OMS de 0 a 5 años y Sociedad Argentina de Pediatría (SAP) desde los 5 años.
2. Gestione el Calendario Nacional de Vacunación con el estado de cada dosis.
3. Registre eventos, hitos del desarrollo y hábitos en una línea de tiempo.
4. Acompañe al tutor con consejos por edad y recordatorios de controles y vacunas.
5. Vincule a cada niño con **un único pediatra de cabecera**, que puede ver su historial y
   conducir el control con una **consulta guiada**.
6. Genere informes PDF completos que sirvan al pediatra para validar el seguimiento.

## 3. Principios rectores

Guían las decisiones de diseño y la resolución de ambigüedades en esta SPEC.

| # | Principio | Consecuencia |
|---|---|---|
| P1 | **Complementa, no reemplaza, al pediatra.** | Ningún mensaje al tutor diagnostica; ante dudas, la app deriva al pediatra. Avisos legales visibles. |
| P2 | **Centralizar la atención en un pediatra de cabecera.** | Un niño tiene un solo pediatra vinculado a la vez. No se incluyen otras especialidades. |
| P3 | **No es una historia clínica.** | El registro de controles es complementario. Las observaciones de un pediatra no las ve ningún otro usuario. Se evita el régimen de la Ley 27.706. |
| P4 | **Lenguaje claro y no alarmante para los tutores.** | El tutor ve franjas en lenguaje simple; los términos técnicos y diagnósticos quedan para el pediatra. |
| P5 | **Los parámetros clínicos no viven en el código.** | Tablas, umbrales, calendario, hitos, cronograma y consejos los gestiona el administrador, sin publicar una versión nueva de la app. |
| P6 | **Privacidad por diseño.** | Datos mínimos: sin DNI del niño, sin fotos, sin detalle clínico innecesario. El administrador no ve datos de salud de los niños. |

## 4. Alcance de la tesis

| Módulo | Incluye |
|---|---|
| Cuentas y acceso | Registro con email y contraseña o Google, verificación de email, recuperación de contraseña, sesión persistente, bloqueo con huella o PIN, perfiles de tutor y pediatra en una misma cuenta, eliminación de cuenta |
| Hijos y tutores | Alta con datos de nacimiento y prematurez, alergias y antecedentes, varios tutores por niño, perfil finalizado a los 15 años |
| Crecimiento | Mediciones (peso, longitud o talla, perímetro cefálico), IMC, percentil y puntaje Z, curvas, estado del niño, cruce de percentiles, edad corregida |
| Vacunación | Calendario Nacional con estados, carga de dosis, carga retroactiva, campañas, otras vacunas |
| Pediatras y vínculos | Perfil profesional con matrícula, validación por el administrador, búsqueda por matrícula, solicitud y aceptación de vínculo, cambio y revocación, invitación a pediatras no registrados, pacientes propios y reclamo del perfil por el tutor |
| Consulta guiada | Control paso a paso, indicaciones para la familia, observaciones privadas, borradores, notas aclaratorias |
| Eventos y línea de tiempo | Eventos automáticos y manuales, hitos del desarrollo |
| Hábitos | Tomas, pañales y sueño con valores estimados, de 0 a 24 meses |
| Consejos | Consejos por edad y categoría, "¿Qué se viene?" |
| Informes | Informe de crecimiento en PDF (tutor y pediatra), exportación del registro de controles |
| Notificaciones | Push y centro de notificaciones, preferencias por tipo |
| Administración | Panel web: validación de pediatras, tablas, umbrales, calendario, campañas, hitos, cronograma, consejos, auditoría |
| Sin conexión | Consulta de datos descargados y carga de mediciones, hábitos y eventos con sincronización posterior |

## 5. Fuera del alcance de la tesis

| Excluido | Motivo |
|---|---|
| iOS y web para tutores o pediatras | Android primero; otras plataformas quedan para después de la tesis |
| Profesionales distintos de pediatras (médicos de familia, especialistas, nutricionistas, enfermería) | Principio P2; se evaluará más adelante |
| Validación automática de matrícula contra REFEPS/SISA | Integración externa; en la tesis se valida en forma manual |
| Historia clínica en sentido legal (Ley 27.706: firma digital, interoperabilidad) | Principio P3 |
| Que un pediatra vea las observaciones de otro pediatra | Principio P3 |
| Curvas específicas para prematuros (Fenton, INTERGROWTH-21st) | Se usa edad corregida sobre las curvas generales |
| Esquemas de vacunación para grupos de riesgo y esquemas de recuperación | Requieren criterio clínico caso por caso |
| Fotos (del niño, de eventos o del carnet de vacunas) | Principio P6 |
| DNI del niño | Principio P6; los duplicados se resuelven por invitación (RF-PED-12) |
| Acceso del propio adolescente a la app | Fuera del rango de usuarios definido |
| Telemedicina, chat o mensajería entre tutor y pediatra | No forma parte del objetivo |
| Cronómetro de lactancia u horarios exactos de hábitos | Se usan valores estimados (D-36) |
| Que el pediatra recomiende consejos puntuales dentro de la app | Cubierto por las indicaciones para la familia (RF-CON-07) |
| Percentiles de tensión arterial y otros indicadores no listados en RN-CRE-01 | Pendiente de validación médica (PVM-02) |
| Inscripción de la base de datos ante la AAIP | Requisito de puesta en producción, no de la tesis (ver RNF-PRI-07) |
| Monetización y publicidad | La app es gratuita y sin publicidad |
| Soporte para otros países o estándares | El producto es específico de Argentina |

## 6. Plan de incrementos

El desarrollo se organiza en incrementos de alrededor de 2 semanas, alineados con los encuentros
quincenales: en cada encuentro se muestra el incremento terminado y funcionando. El orden respeta
las dependencias entre módulos. Antes de I1 hay un incremento técnico, **I0**, que no implementa
requerimientos funcionales propios: deja la base sobre la que se construyen todos los demás.

| Incremento | Contenido | Requerimientos |
|---|---|---|
| **I0 · Fundaciones técnicas** | Renombre a Pasitos, módulo compartido entre app y backend, nuevo esquema de base de datos, sesión persistente, base local cifrada con sincronización, integración continua e imagen Docker del backend | Sin RF propios. Detalle en la [SPEC técnica](../spec-tecnica/00-arquitectura.md#5-fundaciones-técnicas-incremento-i0) |
| **I1 · Base de tutores** | Cuentas, hijos, varios tutores por niño, mediciones y vacunas conectadas al backend | RF-CTA-01 a 07, RF-CTA-09, RF-CTA-11, RF-HIJ-01 a 09, RF-CRE-01 a 03, RF-CRE-10, RF-VAC-01 a 05, RF-VAC-07 |
| **I2 · Motor de crecimiento** | Tablas de referencia, percentiles, puntaje Z, IMC, curvas y estado | RF-CRE-04 a 09, RF-ADM-04, RF-ADM-05 (carga inicial por importación) |
| **I3 · Pediatras** | Perfil profesional, validación mínima por el administrador, vínculos, pacientes propios, reclamo y unificación | RF-PED-01 a 15, RF-ADM-01 a 03 |
| **I4 · Consulta guiada** | Control paso a paso y registro de controles | RF-CON-01 a 12 |
| **I5 · Seguimiento** | Eventos, línea de tiempo, hitos, hábitos, consejos y funcionamiento sin conexión | RF-EVT-01 a 05, RF-HAB-01 a 06, RF-CJO-01 a 03, RNF-OFF-01 a 06 |
| **I6 · Cierre** | Informes PDF, notificaciones, panel de administración completo, bloqueo con huella o PIN, eliminación de cuenta, perfil finalizado | RF-INF-01 a 04, RF-NOT-01 a 03, RF-CJO-04, RF-CON-13, RF-ADM-06 a 12, RF-CTA-08, RF-CTA-10, RF-HIJ-10, RF-VAC-06 |

Los requerimientos no funcionales de seguridad, privacidad, accesibilidad y localización aplican
desde I0. El mecanismo de sincronización que necesita el funcionamiento sin conexión se construye
en I0 e I1; en I5 se completa la experiencia sin conexión y sus pruebas.

## 7. Supuestos

- Los tutores y pediatras tienen un teléfono Android 8.0 o superior y conexión a internet al menos
  intermitente.
- El equipo dispone del libro de la SAP con las tablas de referencia y las transcribirá.
- Habrá pediatras disponibles para validar las reglas clínicas marcadas como ⚕️ PVM.
- El contenido de consejos se carga en forma paulatina a partir de fuentes oficiales (Ministerio de
  Salud de la Nación, SAP, OMS).
- Los supuestos de detalle que todavía requieren confirmación están en
  [92 · Pendientes](92-pendientes.md#supuestos-a-confirmar).

## 8. Restricciones

- Equipo de 4 personas y entrega de tesis con todo el alcance de la sección 4.
- Android nativo como primera plataforma.
- Español de Argentina como único idioma.
- Normativa aplicable: Ley 25.326 de Protección de Datos Personales (datos de salud como datos
  sensibles) y políticas de Google Play para apps de salud.

## 9. Riesgos

| ID | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| R-01 | Errores al transcribir las tablas SAP | Percentiles y estados incorrectos para mayores de 5 años | Doble transcripción independiente y comparación; pruebas con casos de control tomados del libro |
| R-02 | La validación médica llega tarde | Reglas clínicas implementadas con valores provisorios | Parámetros configurables (P5); marcas ⚕️ PVM visibles; validación planificada antes del piloto |
| R-03 | Perfiles duplicados de un mismo niño (sin DNI) | Historial partido entre dos perfiles | Reclamo y unificación por invitación del pediatra (RF-PED-12, RF-PED-13). Riesgo aceptado (D-24) |
| R-04 | Piloto con datos reales de menores | Exposición legal y ética | Cumplir RNF-PRI desde I1; confirmar requisitos institucionales (SUP-14) antes del piloto |
| R-05 | Alcance amplio para 4 personas | Incrementos incompletos | Incrementos con dependencias claras; revisión del avance en cada encuentro quincenal |
| R-06 | Discontinuidad de percentiles a los 5 años por el cambio OMS → SAP | Falsas alertas o confusión del tutor | Marca visual en curvas y exclusión de ese salto en el cruce de percentiles (RN-CRE-08); PVM-03 |
| R-07 | Conflictos de sincronización sin conexión | Datos perdidos o duplicados | Registros editables solo por su autor; sincronización idempotente (RNF-OFF) |
