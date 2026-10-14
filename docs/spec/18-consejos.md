# 18 · Consejos (`CJO`)

Contenido de orientación para tutores según la edad del niño, basado en fuentes oficiales
(Ministerio de Salud de la Nación, SAP, OMS). El administrador lo carga en forma paulatina
(RF-ADM-08).

## Requerimientos funcionales

### RF-CJO-01 · Consejos para la edad
**Actores:** Tutor · **Incremento:** I5 · **Sin conexión:** sí (con contenido descargado)

**Criterios de aceptación**
- **CA1.** Dado un niño, cuando el tutor abre Consejos, entonces ve los consejos publicados cuyo
  rango de edad incluye la edad del niño (corregida si aplica, SUP-10), agrupados por categoría
  (RN-CJO-02).
- **CA2.** Dada la lista, cuando el tutor filtra por categoría, entonces solo ve esa categoría.
- **CA3.** Dado un tramo de edad sin consejos cargados todavía, cuando el tutor abre Consejos,
  entonces ve un mensaje que indica que se sumará contenido, sin errores.
- **CA4.** Dado un consejo que el tutor todavía no abrió, cuando aparece en la lista, entonces se
  marca como nuevo.

### RF-CJO-02 · Detalle de un consejo
**Actores:** Tutor · **Incremento:** I5 · **Sin conexión:** sí (con contenido descargado)

**Criterios de aceptación**
- **CA1.** Dado un consejo, cuando el tutor lo abre, entonces ve título, cuerpo, categoría, la
  fuente con su enlace y el aviso de RN-CJO-03.

### RF-CJO-03 · "¿Qué se viene?"
**Actores:** Tutor · **Incremento:** I5 · **Sin conexión:** sí (con contenido descargado)

**Criterios de aceptación**
- **CA1.** Dado un niño, cuando el tutor abre "¿Qué se viene?", entonces ve los consejos marcados
  como anticipatorios del tramo de edad siguiente (por ejemplo, a los 5 meses: qué esperar a los
  6 meses, como el inicio de la alimentación complementaria).

### RF-CJO-04 · Aviso de consejos nuevos
**Actores:** Sistema · **Incremento:** I6

**Criterios de aceptación**
- **CA1.** Dado un niño que entra en un tramo de edad con consejos, o un consejo nuevo publicado
  para su edad, cuando el sistema lo detecta, entonces envía la notificación N-08 (como máximo
  una por semana por niño, RN-NOT-05).

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-CJO-01 | Cada consejo tiene título, cuerpo, categoría, edad desde y hasta (en meses), fuente obligatoria (nombre y enlace), si es anticipatorio ("¿Qué se viene?") y estado (borrador o publicado). Solo los publicados se muestran. |
| RN-CJO-02 | Categorías: alimentación, sueño, desarrollo, prevención de accidentes, salud bucal, pantallas, vacunas y adolescencia. El administrador puede agregar categorías. |
| RN-CJO-03 | Todo consejo muestra el aviso: "Esta información es orientativa y no reemplaza la consulta con tu pediatra." |
| RN-CJO-04 | Los consejos solo los ven los tutores. El pediatra no recomienda consejos puntuales dentro de la app; sus recomendaciones van en las indicaciones para la familia (RF-CON-07). ⚕️ El contenido debe validarse antes de publicarse (PVM-11). |
