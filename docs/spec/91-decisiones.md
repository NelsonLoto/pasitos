# 91 · Registro de decisiones

Decisiones tomadas con el stakeholder durante el relevamiento funcional del 13 y 14 de octubre de
2026, en cuatro rondas de preguntas. Cada decisión se tomó a partir de una recomendación con sus
alternativas; aquí se registran la decisión y su motivo para que puedan revisarse más adelante.

## Ronda 1 · Alcance, actores y vínculos

| ID | Decisión | Motivo |
|---|---|---|
| D-01 | Esta SPEC reemplaza al documento funcional v1.0 como referencia. Lo ya construido se conserva y lo nuevo se adapta a la SPEC. | El alcance cambió y el v1.0 quedó desactualizado. |
| D-02 | Hábitos y notificaciones push siguen en el alcance. | Los recordatorios tienen alto valor y bajo costo; los hábitos pasan a I5. |
| D-03 | El pediatra informa su matrícula y un administrador la valida manualmente antes de habilitarlo. La validación automática contra REFEPS/SISA queda para después de la tesis. | Un pediatra accede a datos de salud de menores; la autodeclaración no es suficiente. |
| D-04 | Por ahora solo pediatras. | Se evaluará sumar otros profesionales más adelante. |
| D-05 | Existe el rol Administrador con un panel de gestión. | Validar pediatras y mantener contenido y parámetros sin publicar versiones nuevas. |
| D-06 | Una misma cuenta puede tener perfil de tutor y de pediatra. | Una pediatra puede ser madre; evita cuentas duplicadas. |
| D-07 | Un niño puede tener varios tutores; uno es principal e invita a los demás. | Evita que cada tutor cree su propio perfil del mismo niño. |
| D-08 | El vínculo lo inicia el tutor y el pediatra lo acepta o rechaza. | El tutor es quien da acceso a los datos de su hijo; el profesional decide si lo atiende. |
| D-09 | El tutor busca al pediatra por matrícula, no por nombre. | No exponer un directorio de profesionales. |
| D-10 | El pediatra vinculado ve todo el historial y puede cargar mediciones, vacunas, eventos y hábitos. | Seguimiento completo por el profesional de cabecera. |
| D-11 | Las observaciones son propias del pediatra: los tutores no las ven ni las editan. El pediatra puede exportarlas cuando se las soliciten. | Son el registro profesional del pediatra. |
| D-12 | El tutor puede revocar el acceso; lo cargado por el pediatra no se borra. | Control del tutor sobre los datos sin perder el historial. |
| D-13 | Un niño tiene un solo pediatra a la vez. Si cambia, queda el historial de atenciones y datos. | Centralizar la atención del niño sano en un profesional (principio P2). |
| D-14 | El pediatra puede invitar al tutor a reclamar el perfil de un paciente propio. | Evitar perfiles duplicados cuando el tutor se suma después. |
| D-15 | La invitación a un pediatra no registrado queda pendiente, se puede reenviar y vence a los 15 días como máximo. | Plazo acotado para no retener datos de terceros. |
| D-16 | La consulta es un registro complementario, no una historia clínica legal. Una consulta cerrada solo admite notas aclaratorias. | Evitar el régimen de la Ley 27.706 (firma digital, interoperabilidad) manteniendo un registro confiable. |
| D-17 | Referencias de crecimiento: OMS de 0 a 5 años y SAP desde los 5. Las actualizaciones las validan pediatras a través del administrador. | Criterio usado en Argentina, informado por el stakeholder. |

## Ronda 2 · Seguimiento, crecimiento y consulta guiada

| ID | Decisión | Motivo |
|---|---|---|
| D-18 | Desarrollo en 6 incrementos de alrededor de 2 semanas alineados con los encuentros quincenales, sin un MVP recortado. | El entregable de la tesis debe cubrir todo el alcance. |
| D-19 | La matrícula se identifica con jurisdicción y número. | Los números de matrícula se repiten entre jurisdicciones. |
| D-20 | Al cambiar de pediatra, el anterior pierde todo acceso y conserva solo sus propias consultas en lectura. | Coherente con un único pediatra de cabecera. |
| D-21 | El pediatra nuevo no ve las observaciones del anterior; solo los datos objetivos y la fecha y profesional de cada consulta. | Que un pediatra vea observaciones de otro acerca la app a una historia clínica. |
| D-22 | Al reclamar un perfil, el tutor elige si es un hijo nuevo o uno que ya tiene; en ese caso se unifican los historiales y el pediatra que invitó pasa a ser el de cabecera. | Resolver duplicados sin perder datos. |
| D-23 | No se solicita el DNI del niño. | Minimización de datos sensibles; el seguimiento se resuelve por tutores y pediatra. |
| D-24 | Se acepta el riesgo de perfiles duplicados sin DNI; solo se unifican por invitación. | Consecuencia de D-23. |
| D-25 | Las tablas SAP se transcriben del libro disponible para el equipo. | No hay versión digital. |
| D-26 | Indicadores según RN-CRE-01; la lista puede cambiar tras las consultas con pediatras. | La SPEC es un documento vivo. |
| D-27 | La app indica la forma de medir según la edad y aplica la corrección de 0,7 cm de la OMS. | Las referencias usan longitud hasta los 2 años y talla después. |
| D-28 | Prematuros: semanas de gestación y medidas al nacer en el alta; edad corregida hasta los 24 meses; curvas específicas fuera de alcance. | La edad cronológica distorsiona los percentiles de un prematuro. |
| D-29 | El tutor ve percentil y franja en lenguaje simple; el pediatra además el percentil exacto y el puntaje Z. | Información útil para cada perfil. |
| D-30 | Estado en tres niveles, sin términos diagnósticos para tutores, con cruce de percentiles y umbrales configurables. | Evitar alarmar o tranquilizar de más (principio P4). |
| D-31 | El gráfico marca el cambio de referencia a los 5 años y el cruce de percentiles ignora ese salto. Sujeto a validación. | El cambio OMS → SAP puede mover el percentil sin cambio real. |
| D-32 | Cada medición indica su autor, solo el autor la edita, y el mismo día prevalece la del pediatra para el estado. | Las mediciones domésticas son menos precisas. |
| D-33 | Consulta guiada de 10 pasos en orden fijo; los que no corresponden a la edad se omiten sin alterar el orden; solo el paso de medidas es obligatorio; hay borradores. | Facilitar la memoria del profesional y organizar el tiempo de consulta. |
| D-34 | Indicaciones para la familia (visibles para el tutor) separadas de las observaciones (privadas). | El tutor necesita las indicaciones; las observaciones son del pediatra. |
| D-35 | Los parámetros clínicos los gestiona el administrador; los pasos de la consulta están en el código y se cambian por SPEC. | Cada paso tiene datos y validaciones propias. |

## Ronda 3 · Vacunas, eventos, hábitos, consejos, informes y notificaciones

| ID | Decisión | Motivo |
|---|---|---|
| D-36 | Hábitos con valores estimados, sin horarios exactos de inicio y fin ni cronómetro. | Interesa la frecuencia; la precisión exacta no es realista para los padres. |
| D-37 | Vacunas: Calendario Nacional, campañas del administrador y "otras vacunas" manuales. Grupos de riesgo fuera de alcance. | Los grupos de riesgo requieren criterio clínico caso por caso. |
| D-38 | Estados Próxima, Pendiente, Atrasada y Aplicada, sin esquemas de recuperación. | Ante un atraso se deriva al pediatra o al vacunatorio. |
| D-39 | Carga retroactiva con la pregunta "¿Tiene las vacunas al día para su edad?". | Evitar cargar decenas de dosis una por una. |
| D-40 | Fecha obligatoria (salvo carga retroactiva); lote y lugar opcionales, con el lote recomendado; sin fotos del carnet. | Datos útiles sin almacenar imágenes de documentos. |
| D-41 | Un calendario nuevo aplica a las dosis futuras; las registradas no cambian. | Respetar lo ya aplicado. |
| D-42 | Eventos de salud, alergias y antecedentes de forma general, sin detalle clínico. | No es una historia clínica, pero conviene registrar el evento. |
| D-43 | Sin fotos; íconos o emojis predefinidos por tipo de evento. | Distinguir eventos sin datos sensibles. |
| D-44 | Hitos del desarrollo como lista por edad, con un mensaje suave si no se logran a la edad límite. | No alarmar; la evaluación formal es del pediatra. |
| D-45 | Hábitos hasta los 24 meses; el pediatra ve un resumen. | Etapa en la que el registro es útil. |
| D-46 | Consejos cargados en forma paulatina por el administrador, con fuente, por categoría y edad, con "¿Qué se viene?". El pediatra no recomienda consejos dentro de la app. | Las recomendaciones del pediatra van en las indicaciones. |
| D-47 | El informe es siempre completo; solo se filtra por fechas. | Su lector es el pediatra, que debe validar el seguimiento completo. |
| D-48 | El informe del tutor incluye percentil exacto y puntaje Z sin términos diagnósticos; el del pediatra agrega la clasificación técnica. | Útil para el pediatra sin contradecir D-30. |
| D-49 | La exportación del registro de controles queda auditada. | Incluye observaciones privadas. |
| D-50 | Notificaciones según la tabla de tipos, activables por tipo, con anticipación fija; la fecha de control fijada por el pediatra prevalece sobre el cronograma. | Simplicidad y prioridad del criterio profesional. |

## Ronda 4 · Requerimientos no funcionales y cierre

| ID | Decisión | Motivo |
|---|---|---|
| D-51 | Consentimiento expreso según la Ley 25.326; la inscripción ante la AAIP queda para producción. | Datos de salud de menores. |
| D-52 | Reglas de eliminación de cuenta y de hijo según RN-CTA-08 y RN-HIJ-08; las consultas cerradas se conservan en el registro del pediatra. | Exigencia de Google Play y derecho de supresión, sin destruir el registro profesional. |
| D-53 | Verificación de email, contraseña de al menos 8 caracteres con letras y números, sesión de 30 días sin uso y bloqueo opcional con huella o PIN. | Equilibrio entre seguridad y uso cotidiano. |
| D-54 | El registro de accesos solo lo consulta el administrador; el tutor ve el historial de vínculos. | Trazabilidad ante reclamos. |
| D-55 | El administrador no accede a datos de los niños; valida matrículas en REFEPS/SISA sin que el pediatra suba documentos. | Mínimo privilegio y minimización. |
| D-56 | Sin conexión: lectura de lo descargado y carga de mediciones, hábitos y eventos con sincronización. | Uso real de los padres, con riesgo de conflictos bajo. |
| D-57 | Objetivos de rendimiento de RNF-REN; se prevé una prueba piloto con usuarios (con datos ficticios, según D-65). | Experiencia fluida; validar el producto con usuarios. |
| D-58 | Android 8.0 o superior; teléfonos en vertical. | Cubre casi todos los dispositivos en uso; es la base actual del proyecto. |
| D-59 | Panel de administración web; tablas SAP cargadas por importación. | Las tareas del administrador son de escritorio. |
| D-60 | Requerimientos de accesibilidad de RNF-ACC. | Usuarios diversos, incluidos padres con baja visión. |
| D-61 | Español de Argentina con voseo, unidades métricas y fechas dd/mm/aaaa. | Público argentino. |
| D-62 | A los 15 años el perfil pasa a finalizado; el acceso del adolescente queda fuera de alcance. | Fin del rango de la app. |
| D-63 | Gratuita y sin publicidad. | La publicidad en una app de salud infantil es éticamente cuestionable y complica la privacidad. |
| D-64 | La SPEC se escribe en Markdown en `docs/spec/`, un archivo por módulo, versionada con ramas y Pull Requests. | Revisión por PR y referencia directa desde cada incremento. |

## Posteriores al relevamiento

| ID | Decisión | Motivo |
|---|---|---|
| D-67 | Cerrar sesión funciona sin conexión: los datos del dispositivo se borran al instante y la sesión se revoca en el servidor cuando vuelve la conexión (16/10/2026). | Quien presta o entrega el teléfono sin señal tiene que poder sacar sus datos en el momento, y la sesión no puede quedar viva en el servidor. |
| D-66 | Antes de su fecha de término, un prematuro no muestra edad corregida (sería negativa) sino cuánto falta para esa fecha (16/10/2026). | Es más útil para la familia que ocultar el dato. La fecha se calcula con las semanas de gestación del alta, sin pedir otro dato. |
| D-65 | La prueba piloto se hace con **datos ficticios**: niños inventados que no corresponden a ninguna persona real (14/10/2026). Reemplaza la idea de un piloto con datos reales de D-57. | Evita exponer datos de salud de menores, y con ello el aval de un comité de ética y el consentimiento informado específico (SUP-14 deja de aplicar). |
