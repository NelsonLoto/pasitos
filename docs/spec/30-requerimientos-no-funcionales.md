# 30 · Requerimientos no funcionales

Aplican desde I0 salvo que se indique otro incremento.

## Seguridad (`SEG`)

| ID | Requerimiento |
|---|---|
| RNF-SEG-01 | Toda comunicación entre la app, el panel y el servidor viaja cifrada (TLS 1.2 o superior). |
| RNF-SEG-02 | Los datos se almacenan cifrados en el servidor y en las copias de seguridad. En el dispositivo, los datos descargados y los pendientes de sincronizar se guardan cifrados con claves protegidas por el sistema operativo. |
| RNF-SEG-03 | Las contraseñas se almacenan con un algoritmo de hash adaptativo con sal. Nunca se guardan ni se registran en texto plano. |
| RNF-SEG-04 | Las sesiones son revocables desde el servidor. Cerrar sesión revoca la sesión; cambiar o recuperar la contraseña revoca todas las de la cuenta. |
| RNF-SEG-05 | Los permisos de [01 · Actores y permisos](01-actores-y-permisos.md) se verifican en el servidor en cada operación. Ocultar un dato en la interfaz no alcanza: un dato al que el usuario no tiene acceso (por ejemplo, las observaciones de una consulta) nunca se envía a su dispositivo. |
| RNF-SEG-06 | **Registro de accesos.** Se registra quién, cuándo, qué acción (ver, cargar, modificar, eliminar, exportar) y sobre qué tipo de dato y qué niño, para todo acceso de un pediatra a datos de un niño y para toda modificación o exportación. Se conserva al menos 2 años (valor propuesto) y solo lo consulta el administrador (RF-ADM-12). |
| RNF-SEG-07 | **Protección contra abuso.** Bloqueo temporal por intentos fallidos de inicio de sesión (RN-CTA-05). Límite de búsquedas por matrícula por tutor (valor propuesto: 20 por hora) para impedir recorrer el padrón de pediatras. |
| RNF-SEG-08 | Con el bloqueo de la app activado (RF-CTA-08), las pantallas con datos de salud no se muestran en la vista previa de aplicaciones recientes del sistema. |
| RNF-SEG-09 | Copias de seguridad diarias y cifradas de la base de datos. La restauración se prueba al menos una vez antes del piloto. |

## Privacidad (`PRI`)

| ID | Requerimiento |
|---|---|
| RNF-PRI-01 | Los datos de salud son **datos sensibles** según la Ley 25.326 y solo se tratan con consentimiento expreso de su titular o representante legal. |
| RNF-PRI-02 | Se obtienen y registran (con versión y fecha) estos consentimientos: términos y política de privacidad (RN-CTA-02), declaración de representación del niño (RF-HIJ-01, RF-HIJ-06, RF-PED-12) y compromiso de confidencialidad del pediatra (RF-PED-01). |
| RNF-PRI-03 | **Minimización.** No se recolectan DNI del niño ni fotos. Los eventos de salud y antecedentes son generales. Los avatares son predefinidos (D-23, D-42, D-43). |
| RNF-PRI-04 | **Derechos del titular.** Acceso: el tutor ve todos los datos del niño en la app y puede obtenerlos en el informe PDF. Rectificación: edición de los datos propios. Supresión: eliminación de cuenta (RF-CTA-10) y de hijo (RF-HIJ-09). Los pedidos que lleguen por fuera de la app se responden dentro de los plazos de la Ley 25.326 (10 días corridos para acceso, 5 días hábiles para rectificación o supresión). |
| RNF-PRI-05 | Las notificaciones push no incluyen datos clínicos (RN-NOT-04). |
| RNF-PRI-06 | El administrador no accede a datos de salud de los niños (RN-ADM-01). |
| RNF-PRI-07 | Antes de salir a producción, la base de datos debe inscribirse en el Registro Nacional de Bases de Datos de la AAIP. Queda fuera del alcance de la tesis. |
| RNF-PRI-08 | **Prueba piloto.** Se realiza con datos ficticios que no corresponden a personas reales (D-65). La app del piloto muestra un aviso permanente: "Versión de prueba: no cargues datos de niños reales". Los únicos datos personales reales son el nombre y el email de las cuentas de los participantes, que se rigen por los términos y la política de privacidad. Al terminar el piloto se eliminan todas las cuentas y datos de ese entorno (SUP-22). |
| RNF-PRI-09 | Si los datos se alojan fuera de Argentina, el país de destino debe tener un nivel de protección adecuado o deben existir garantías contractuales (art. 12 de la Ley 25.326). Se decide en el diseño técnico (SUP-23). |
| RNF-PRI-10 | Los datos no se comparten con terceros ni se usan con fines comerciales o publicitarios. Cualquier uso para investigación requiere anonimización y consentimiento específico. La app es gratuita y sin publicidad (D-63). |

## Funcionamiento sin conexión (`OFF`) — Incremento I5

El mecanismo de sincronización se construye en I0 e I1; en I5 se completan la experiencia sin
conexión y sus pruebas (ver [SPEC técnica](../spec-tecnica/14-sincronizacion.md)).

| ID | Requerimiento |
|---|---|
| RNF-OFF-01 | **Lectura.** Sin conexión, el usuario puede consultar los datos sincronizados de sus hijos o pacientes: perfil, mediciones, curvas, estado, vacunas, línea de tiempo, hábitos, consultas visibles y consejos descargados. |
| RNF-OFF-02 | **Carga.** Sin conexión, el tutor puede crear, editar y eliminar sus propias mediciones, hábitos, eventos e hitos. Los percentiles y estados de las mediciones cargadas sin conexión se calculan en el dispositivo con las tablas y umbrales descargados. |
| RNF-OFF-03 | **Sincronización.** Al recuperar la conexión, los cambios pendientes se sincronizan en segundo plano y sin intervención del usuario. También hay una acción "Sincronizar ahora". Reintentar una sincronización nunca duplica registros. |
| RNF-OFF-04 | **Visibilidad.** Los registros pendientes de sincronizar se marcan como tales y la app muestra la hora de la última sincronización. |
| RNF-OFF-05 | **Conflictos.** Como cada registro solo lo edita su autor, los conflictos solo pueden darse entre dispositivos del mismo usuario: prevalece el último cambio. Si al sincronizar el usuario ya no tiene acceso al niño (fue quitado como tutor o el perfil se eliminó), los cambios pendientes se descartan y se le informa. |
| RNF-OFF-06 | Requieren conexión, con un mensaje claro si no la hay: cuentas (salvo cerrar sesión, RF-CTA-07), alta y edición de hijos, tutores, vacunas, vínculos e invitaciones, consulta guiada, informes, preferencias y todo el panel de administración. |

## Rendimiento (`REN`)

| ID | Requerimiento |
|---|---|
| RNF-REN-01 | Las pantallas que traen datos del servidor se muestran en menos de 2 segundos en el 95 % de los casos, con una conexión móvil 4G. |
| RNF-REN-02 | Las curvas de crecimiento se dibujan en menos de 2 segundos aun con el historial completo de 0 a 15 años. |
| RNF-REN-03 | Un informe PDF se genera en menos de 10 segundos. |
| RNF-REN-04 | El percentil, el puntaje Z y el estado de una medición se muestran en menos de 1 segundo desde que se completa el valor. |
| RNF-REN-05 | La capacidad se dimensiona para la prueba piloto (SUP-13), con una arquitectura que permita crecer sin rediseño. |

## Usabilidad (`USA`)

| ID | Requerimiento |
|---|---|
| RNF-USA-01 | Lenguaje claro y sin jerga para tutores; terminología técnica para pediatras. |
| RNF-USA-02 | Las acciones frecuentes son rápidas: un hábito se registra en no más de 3 toques desde su pantalla y una medición en una sola pantalla. |
| RNF-USA-03 | La consulta guiada muestra siempre el progreso (paso actual, completados, omitidos). |
| RNF-USA-04 | Los mensajes de error explican qué pasó y cómo seguir, sin códigos técnicos. |
| RNF-USA-05 | Los avisos de que la app no reemplaza la atención médica están visibles en el registro, el estado del niño, los consejos, los informes y la consulta. |
| RNF-USA-06 | Las pantallas sin datos muestran un estado vacío que explica cómo empezar; nunca muestran datos de ejemplo como si fueran reales. |

## Accesibilidad (`ACC`)

| ID | Requerimiento |
|---|---|
| RNF-ACC-01 | Contraste de color según WCAG 2.1 nivel AA (4,5:1 para texto normal). |
| RNF-ACC-02 | La app respeta el tamaño de letra del sistema hasta el 200 % sin cortar ni superponer contenido. |
| RNF-ACC-03 | Todos los elementos interactivos tienen una etiqueta para el lector de pantalla TalkBack, y los gráficos una descripción textual. |
| RNF-ACC-04 | Las áreas táctiles miden al menos 48 × 48 dp. |
| RNF-ACC-05 | Toda curva de crecimiento ofrece una vista alternativa en tabla. |
| RNF-ACC-06 | El estado y los estados de vacunas nunca se comunican solo con color: siempre con ícono y texto. |

## Compatibilidad (`COM`)

| ID | Requerimiento |
|---|---|
| RNF-COM-01 | Android 8.0 (API 26) o superior. |
| RNF-COM-02 | Diseñada para teléfonos en orientación vertical. En tablets funciona, sin diseño específico. |
| RNF-COM-03 | El panel de administración funciona en las dos últimas versiones de Chrome, Firefox y Edge de escritorio. |
| RNF-COM-04 | En dispositivos sin servicios de Google, la app funciona con email y contraseña; el inicio con Google y las notificaciones push no están disponibles, y se informa al usuario. |

## Localización (`LOC`)

| ID | Requerimiento |
|---|---|
| RNF-LOC-01 | Español de Argentina, con voseo. |
| RNF-LOC-02 | Unidades métricas: kg, cm, ml, mmHg. Coma como separador decimal en la interfaz (3,5 kg). |
| RNF-LOC-03 | Fechas dd/mm/aaaa y horas en formato de 24 h. Las fechas que dependen del día (edad, vencimientos, notificaciones, perfil finalizado) se calculan en la hora de Argentina (UTC−3). |

## Mantenibilidad y configurabilidad (`CFG`)

| ID | Requerimiento |
|---|---|
| RNF-CFG-01 | Tablas de referencia, umbrales, calendario, campañas, hitos, cronograma, textos del estado y consejos se gestionan desde el panel, sin publicar una versión nueva de la app (P5). |
| RNF-CFG-02 | Cada criterio de aceptación de esta SPEC tiene al menos una prueba que lo verifica. |
| RNF-CFG-03 | El cálculo de percentiles y puntaje Z tiene pruebas automatizadas con casos de control tomados de las tablas OMS y SAP para ambos sexos y edades representativas de cada tramo (riesgo R-01). |

## Aspectos legales (`LEG`)

| ID | Requerimiento |
|---|---|
| RNF-LEG-01 | La app publica términos y condiciones y política de privacidad, accesibles desde el registro y la configuración. |
| RNF-LEG-02 | Ningún texto de la app, del panel ni de los informes llama "historia clínica" al registro de controles (D-16, Ley 27.706). |
| RNF-LEG-03 | Para publicarse en Google Play: eliminación de cuenta desde la app y desde un enlace web, y cumplimiento de las políticas y declaraciones para apps de salud. La app está dirigida a adultos (tutores y pediatras), no a niños. |
