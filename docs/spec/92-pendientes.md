# 92 · Pendientes

Cuando un pendiente se resuelve, se actualiza la regla afectada, se marca aquí como **Resuelto**
con fecha y quién lo validó, y se registra el cambio de versión en el [README](README.md).

## Pendientes de validación médica

Para llevar a la reunión con pediatras. Todas estas reglas pueden implementarse con el valor
inicial indicado, porque sus parámetros son configurables (principio P5).

| ID | Tema | Pregunta para los pediatras | Valor inicial en la SPEC | Regla | Estado |
|---|---|---|---|---|---|
| PVM-01 | Cobertura de la referencia SAP | ¿Qué tabla de IMC para la edad se usa desde los 5 años: SAP u OMS 2007? ¿La referencia SAP cubre peso para la edad en todo el tramo de 5 a 15 años, incluida la pubertad? | El indicador no se calcula en los tramos sin tabla | RN-CRE-01 | Abierto |
| PVM-02 | Indicadores adicionales | ¿Se suman velocidad de crecimiento, percentiles de tensión arterial u otros indicadores? | Solo los de RN-CRE-01; la tensión arterial se registra sin percentil | RN-CRE-01, SUP-09 | Abierto |
| PVM-03 | Cambio de referencia a los 5 años | ¿El cambio OMS → SAP es exactamente a los 60 meses? ¿Cómo se muestra y se interpreta el salto de percentil? | Corte en 60 meses, marca visual en el gráfico | RN-CRE-02, RF-CRE-06 | Abierto |
| PVM-04 | Umbrales del estado | ¿Qué rangos de percentil o puntaje Z corresponden a cada nivel (Dentro de lo esperado, A controlar, Consultar con el pediatra) por indicador y edad? ¿Qué clasificación técnica ve el pediatra? | Consultar: por debajo de P3 o por encima de P97. A controlar: solo cruce de percentiles | RN-CRE-07 | Abierto |
| PVM-05 | Cruce de percentiles | ¿Cuántas líneas de percentil deben cruzarse para marcar "A controlar"? ¿Hace falta un tiempo mínimo entre mediciones? | 2 líneas, sin tiempo mínimo | RN-CRE-08 | Abierto |
| PVM-06 | Plausibilidad | ¿Son adecuados los límites de puntaje Z para pedir confirmación de un valor? | Banderas de la OMS | RN-CRE-11 | Abierto |
| PVM-07 | Edad corregida | ¿Se corrige en todos los nacidos con menos de 37 semanas? ¿Hasta los 24 meses o más en los muy prematuros? | Menos de 37 semanas, hasta los 24 meses | RN-HIJ-05 | Abierto |
| PVM-08 | Hitos del desarrollo | ¿Qué hitos, con qué edad esperada y qué edad límite, y de qué fuente? | Sin catálogo; lo carga el administrador | RN-EVT-05 | Abierto |
| PVM-09 | Pesquisas por edad | ¿Qué pesquisas corresponden a cada edad en la consulta guiada? | Sin definir | RF-CON-06 | Abierto |
| PVM-10 | Textos del estado | ¿Los textos para tutores son claros y no alarmantes? | Sin definir; los carga el administrador | RN-CRE-10 | Abierto |
| PVM-11 | Contenido de consejos | Revisión del contenido antes de publicarlo | — | RN-CJO-04 | Abierto |
| PVM-12 | Vacunas | ¿Qué tolerancia corresponde a cada dosis antes de considerarla atrasada? | 30 días; anticipación de 7 días | RN-VAC-01 | Abierto |
| PVM-13 | Cronograma de controles | Confirmar las edades de control contra el cronograma vigente del Ministerio | Cronograma 2025 resumido en RN-NOT-02 | RN-NOT-02 | Abierto |
| PVM-14 | Formato de las tablas SAP | ¿El libro publica los parámetros L, M y S o solo percentiles? Define si el puntaje Z es exacto o aproximado | Interpolación entre percentiles si no hay L, M y S | RN-CRE-05 | Abierto |

## Supuestos a confirmar

Decisiones de detalle que la SPEC toma para quedar completa, pero que no se trataron en el
relevamiento. Si no se objetan, quedan confirmadas al aprobarse el PR de esta versión.

| ID | Supuesto | Regla |
|---|---|---|
| SUP-01 | Solo el tutor principal gestiona tutores y el vínculo con el pediatra; los secundarios tienen los mismos permisos de carga. Si el principal deja de ser tutor, el rol pasa al secundario más antiguo. | 01 · Actores y permisos, RF-HIJ-09 |
| SUP-02 | Cada reenvío de una invitación a un pediatra genera un plazo nuevo de 15 días. | RF-PED-09 |
| SUP-03 | Las invitaciones a tutores y las de reclamo de perfil también vencen a los 15 días. | RN-HIJ-07, RF-PED-12 |
| SUP-04 | El enlace de verificación de email vence a las 24 horas y el de recuperación de contraseña a los 60 minutos. | RN-CTA-06 |
| SUP-05 | Tras 5 intentos fallidos, el inicio con contraseña se bloquea 15 minutos. | RN-CTA-05 |
| SUP-06 | Si el administrador suspende a un pediatra, sus vínculos terminan y sus pacientes propios quedan inaccesibles hasta que sea rehabilitado. | RN-PED-11 |
| SUP-07 | Al unificar perfiles, prevalecen los datos del perfil del tutor, y solo se unifican perfiles con igual fecha de nacimiento y sexo. | RN-PED-09 |
| SUP-08 | Lo cargado en un borrador de consulta no se incorpora al historial hasta cerrarla. | RN-CON-03 |
| SUP-09 | La tensión arterial se registra sin cálculo de percentil. | Paso 3 de la consulta |
| SUP-10 | Consejos e hitos usan la edad corregida cuando aplica. | RF-CJO-01, RF-EVT-05 |
| SUP-11 | Antes de unificar, el tutor ve un resumen de lo que se suma. | RF-PED-13 |
| SUP-12 | El avatar del niño se elige entre ilustraciones predefinidas. | RN-HIJ-11 |
| SUP-13 | Tamaño de la prueba piloto (cantidad de familias y pediatras), para dimensionar la infraestructura. | RNF-REN-05 |
| SUP-14 | Requisitos de la institución para un piloto con datos de salud de menores (aval de un comité de ética, modelo de consentimiento informado). A confirmar con el profesor. | RNF-PRI-08 |
| SUP-15 | Si el tutor no conoce las semanas de gestación, se asume un nacimiento a término (40 semanas). | RN-HIJ-03 |
| SUP-16 | La fecha de una consulta puede ser anterior a hoy, para cargarla después de atender. | Paso 1 de la consulta |
| SUP-17 | Si un pediatra validado cambia su nombre, apellido o matrícula, pasa a revalidación sin perder sus vínculos. | RN-PED-02 |
| SUP-18 | Si la última medición tiene más de 6 meses, el resumen del estado muestra "Sin datos recientes". | RF-CRE-07 |
| SUP-19 | Las dosis cuya fecha indicada está a más de 7 días se muestran como "Programada", como dato informativo además de los cuatro estados. | RN-VAC-01 |
| SUP-20 | Las solicitudes de vínculo a pediatras registrados también vencen a los 15 días sin respuesta. | RN-PED-06 |
| SUP-21 | Del examen físico y de las pesquisas, el tutor ve el resultado pero no la descripción ni las notas del pediatra. | RN-CON-02 |
| SUP-22 | Al terminar el piloto, los datos se eliminan salvo que el participante elija conservar su cuenta. | RNF-PRI-08 |
| SUP-23 | El país donde se alojan los datos se decide en el diseño técnico, cumpliendo el art. 12 de la Ley 25.326. | RNF-PRI-09 |
| SUP-24 | Valores propuestos menores: alergias y antecedentes hasta 280 caracteres; indicaciones y observaciones hasta 2.000; bloqueo de la app tras 5 minutos en segundo plano; sesión del panel de 30 minutos; registro de accesos conservado 2 años; 20 búsquedas por matrícula por hora. | RN-HIJ-10, RN-CON-06, RF-CTA-08, RF-ADM-01, RNF-SEG-06, RNF-SEG-07 |

## Otros pendientes

| Tema | Detalle |
|---|---|
| Métricas de éxito | Definir cómo se evalúa el piloto para la tesis (por ejemplo, uso sostenido, satisfacción de tutores y pediatras, tiempo de consulta). |
| Términos y política de privacidad | Redactar los textos legales antes del piloto. |
| Alineación del código existente | Revisar las 15 pantallas y el backend actual contra esta SPEC al planificar cada incremento. |
