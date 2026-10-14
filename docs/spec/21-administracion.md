# 21 · Administración (`ADM`)

El administrador opera un **panel web** (D-59). Valida pediatras y mantiene los parámetros
clínicos y el contenido sin necesidad de publicar una versión nueva de la app (principio P5). No
accede a datos de salud de los niños (principio P6).

En I2 e I3 alcanza con una versión mínima: tablas cargadas por importación y una pantalla de
validación de pediatras. El panel completo se termina en I6.

## Requerimientos funcionales

### RF-ADM-01 · Acceso al panel
**Actores:** Administrador · **Incremento:** I3 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dada una cuenta de administrador, cuando inicia sesión en el panel con email y
  contraseña, entonces accede a las funciones de administración.
- **CA2.** Dado que no existe registro público de administradores, cuando se necesita una cuenta
  nueva, entonces la crea otro administrador desde el panel.
- **CA3.** Dada una cuenta de administrador, cuando intenta iniciar sesión en la app móvil,
  entonces no puede.
- **CA4.** Dada una sesión del panel inactiva por 30 minutos, cuando el administrador vuelve,
  entonces debe iniciar sesión de nuevo.

### RF-ADM-02 · Validación de pediatras
**Actores:** Administrador · **Incremento:** I3 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dados perfiles pendientes o en revalidación, cuando el administrador abre Validaciones,
  entonces los ve ordenados por antigüedad, con nombre, apellido, email, jurisdicción y número de
  matrícula.
- **CA2.** Dado un perfil, cuando el administrador lo verifica en forma manual en el buscador
  público de REFEPS/SISA y lo aprueba, entonces el perfil pasa a *validado* y se notifica al
  pediatra (N-12).
- **CA3.** Dado un perfil, cuando el administrador lo rechaza, entonces debe indicar el motivo, que
  se envía al pediatra.
- **CA4.** Dada una aprobación o un rechazo, cuando se registra, entonces queda constancia de qué
  administrador lo hizo y cuándo.

### RF-ADM-03 · Suspensión y rehabilitación de pediatras
**Actores:** Administrador · **Incremento:** I3 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un pediatra validado, cuando el administrador lo suspende indicando el motivo,
  entonces se aplica RN-PED-11.
- **CA2.** Dado un pediatra suspendido, cuando el administrador lo rehabilita, entonces vuelve a
  *validado* (los vínculos terminados no se restauran).

### RF-ADM-04 · Tablas de referencia de crecimiento
**Actores:** Administrador · **Incremento:** I2 (importación) e I6 (gestión en el panel) · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un archivo con una tabla de referencia (indicador, referencia, sexo, edad y valores
  L, M y S o percentiles), cuando se importa, entonces se valida su formato y se informan las
  filas con errores sin importar nada parcial.
- **CA2.** Dada una tabla importada, cuando se publica, entonces queda como versión vigente
  (RN-ADM-04) y los cálculos la usan desde ese momento (RN-CRE-13).
- **CA3.** Dada una tabla publicada, cuando se consulta en el panel, entonces se ve su origen
  (OMS o SAP), versión, fecha de publicación y quién la validó.

### RF-ADM-05 · Umbrales clínicos
**Actores:** Administrador · **Incremento:** I2 (carga inicial) e I6 (gestión en el panel) · **Sin conexión:** no

Parámetros configurables: umbrales de los niveles de estado y de la clasificación técnica por
indicador, referencia y tramo de edad (RN-CRE-07); cantidad de líneas para el cruce de
percentiles (RN-CRE-08); límites de plausibilidad y rangos de entrada (RN-CRE-11).

**Criterios de aceptación**
- **CA1.** Dado un cambio de umbral, cuando el administrador lo guarda, entonces debe indicar quién
  lo validó (nombre del pediatra) y la referencia de esa validación (RN-ADM-02).
- **CA2.** Dados umbrales inconsistentes (por ejemplo, superpuestos), cuando el administrador
  intenta guardarlos, entonces se rechazan indicando el problema.

### RF-ADM-06 · Calendario Nacional de Vacunación
**Actores:** Administrador · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado el calendario, cuando el administrador crea una versión nueva, entonces define
  vacunas, dosis, edad indicada, anticipación y tolerancia de cada dosis, contenido informativo
  (RF-VAC-07) y fecha de vigencia.
- **CA2.** Dada una versión publicada, cuando llega su fecha de vigencia, entonces se aplica según
  RN-VAC-03.

### RF-ADM-07 · Campañas de vacunación
**Actores:** Administrador · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dada una campaña nueva, cuando el administrador la publica con vacuna, rango de edad
  destinatario y fechas de inicio y fin, entonces aparece a los niños de ese rango (RF-VAC-06).

### RF-ADM-08 · Consejos
**Actores:** Administrador · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un consejo nuevo o editado con los datos de RN-CJO-01, cuando el administrador lo
  guarda, entonces queda como borrador hasta que lo publique.
- **CA2.** Dado un consejo sin fuente, cuando el administrador intenta publicarlo, entonces no
  puede.
- **CA3.** Dado un consejo publicado, cuando el administrador lo despublica, entonces deja de
  mostrarse en la app.

### RF-ADM-09 · Catálogo de hitos del desarrollo
**Actores:** Administrador · **Incremento:** I5 (carga inicial) e I6 (gestión en el panel) · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado el catálogo, cuando el administrador crea o edita un hito, entonces define
  descripción, área (motor, lenguaje, social, cognitivo), edad esperada, edad límite y fuente.

### RF-ADM-10 · Cronograma de controles
**Actores:** Administrador · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado el cronograma, cuando el administrador lo edita, entonces define las edades de
  control que usa RN-NOT-02 para sugerir el próximo control.

### RF-ADM-11 · Textos del estado para tutores
**Actores:** Administrador · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dados los niveles de estado por indicador, cuando el administrador edita sus textos para
  tutores, entonces el panel le recuerda la regla de lenguaje no diagnóstico (RN-CRE-10) y exige
  indicar quién validó el texto.

### RF-ADM-12 · Registro de accesos
**Actores:** Administrador · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un reclamo, cuando el administrador consulta el registro filtrando por usuario,
  identificador interno del niño o rango de fechas, entonces ve quién accedió, cuándo, qué acción
  hizo (ver, cargar, modificar, eliminar, exportar) y sobre qué tipo de dato.
- **CA2.** Dado el registro, cuando el administrador lo consulta, entonces nunca ve los valores de
  salud registrados (RN-ADM-01).

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-ADM-01 | El administrador no accede a datos de salud de los niños (mediciones, vacunas, eventos, hábitos, consultas). Para resolver un reclamo trabaja con identificadores internos y el registro de accesos (D-55). |
| RN-ADM-02 | Todo cambio en un parámetro clínico (tablas, umbrales, calendario, hitos, cronograma, textos del estado) registra quién lo hizo, cuándo, el valor anterior y el nuevo, y el pediatra que lo validó. |
| RN-ADM-03 | Los cambios publicados en el panel se aplican en la app sin publicar una versión nueva. La app los obtiene en la siguiente sincronización. |
| RN-ADM-04 | Las tablas de referencia, el calendario y los umbrales son **versionados**: hay una sola versión vigente por cada uno y las anteriores se conservan para consulta. |
| RN-ADM-05 | Para validar una matrícula, el administrador la consulta en forma manual en el buscador público de REFEPS/SISA. El pediatra no sube imágenes de documentos (D-55). |
