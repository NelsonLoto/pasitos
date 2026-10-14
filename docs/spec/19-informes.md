# 19 · Informes (`INF`)

El informe de crecimiento está pensado para que el tutor lo lleve al pediatra y este valide el
seguimiento; no es necesario que el tutor lo interprete en detalle. Por eso es siempre completo
(D-47).

## Requerimientos funcionales

### RF-INF-01 · Informe de crecimiento generado por un tutor
**Actores:** Tutor · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un niño, cuando el tutor elige generar el informe, entonces puede elegir el período
  (todo el historial, últimos 12 meses o un rango de fechas) y nada más: las secciones son siempre
  las de RN-INF-01.
- **CA2.** Dado el período elegido, cuando se genera, entonces el PDF incluye percentil y puntaje Z
  de cada indicador y el estado en tres niveles, sin clasificación técnica ni términos
  diagnósticos (RN-INF-02).
- **CA3.** Dado un período sin mediciones, cuando se genera, entonces las secciones vacías indican
  "Sin registros en el período" en lugar de omitirse.

### RF-INF-02 · Informe de crecimiento generado por el pediatra
**Actores:** Pediatra de cabecera · **Incremento:** I6 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado un paciente, cuando el pediatra genera el informe, entonces el PDF tiene el mismo
  contenido que el del tutor más la clasificación técnica de cada indicador (RN-INF-02).
- **CA2.** Dado el informe del pediatra, cuando se genera, entonces no incluye observaciones de
  ninguna consulta (para eso existe RF-INF-03).

### RF-INF-03 · Exportación del registro de controles
**Actores:** Pediatra · **Incremento:** I6 · **Sin conexión:** no

El pediatra exporta sus propias consultas de un paciente, incluidas las observaciones, para
entregarlas cuando se las soliciten por fuera de la app (D-11).

**Criterios de aceptación**
- **CA1.** Dado un paciente actual o anterior, cuando el pediatra elige exportar, entonces elige un
  rango de fechas y obtiene un PDF con sus consultas cerradas en ese rango: todos los pasos,
  observaciones y notas aclaratorias.
- **CA2.** Dado el PDF, cuando se genera, entonces incluye en cada página la leyenda del registro
  complementario (RN-INF-04) y los datos del pediatra (nombre y matrícula).
- **CA3.** Dada la exportación, cuando se completa, entonces queda registrada con pediatra,
  paciente, rango y fecha y hora (RN-INF-05).
- **CA4.** Dado un pediatra, cuando intenta exportar consultas de otro pediatra, entonces no
  puede.

### RF-INF-04 · Compartir o guardar el PDF
**Actores:** Tutor, Pediatra · **Incremento:** I6 · **Sin conexión:** no (para generar)

**Criterios de aceptación**
- **CA1.** Dado un PDF generado, cuando el usuario elige compartir, entonces se abre el menú nativo
  de Android para enviarlo por la app que elija. Pasitos no envía emails con el informe.
- **CA2.** Dado un PDF generado, cuando el usuario elige guardar, entonces se guarda en el
  almacenamiento del dispositivo con un nombre que incluye el nombre del niño y la fecha.

## Reglas de negocio

### RN-INF-01 · Contenido del informe de crecimiento

Secciones, en este orden:

1. **Encabezado**: Pasitos, fecha y hora de generación, quién lo generó (tutor o pediatra) y
   período.
2. **Datos del niño**: nombre, sexo, fecha de nacimiento, edad (y edad corregida si aplica),
   semanas de gestación, medidas al nacer, alergias y antecedentes.
3. **Pediatra de cabecera**: nombre y matrícula, si tiene.
4. **Estado actual**: resumen y estado por indicador, con la fecha de la medición en que se basa.
5. **Curvas de crecimiento**: una por indicador aplicable, con la trayectoria del período y la
   referencia usada (OMS o SAP).
6. **Tabla de mediciones**: fecha, edad, peso, longitud o talla (y forma de medición), perímetro
   cefálico, IMC, percentil y puntaje Z de cada indicador, y autor.
7. **Vacunación**: dosis aplicadas (fecha, lote y lugar si constan), pendientes, próximas y
   atrasadas; campañas y otras vacunas.
8. **Desarrollo**: hitos logrados con fecha y resultados de pruebas de pesquisa registradas en
   consultas.
9. **Eventos de salud** del período.
10. **Resumen de hábitos**, si el niño tuvo registros en el período.
11. **Controles**: fecha y profesional de cada consulta del período, y las indicaciones para la
    familia de las consultas visibles para quien genera el informe.
12. **Aviso legal** (RN-INF-04).

### Demás reglas

| ID | Regla |
|---|---|
| RN-INF-02 | El informe del tutor muestra percentil, puntaje Z y nivel de estado. El informe del pediatra agrega la clasificación técnica. Ninguno incluye observaciones (D-48). |
| RN-INF-03 | El único filtro es el período. No se pueden quitar secciones. |
| RN-INF-04 | Todo PDF incluye la leyenda: "Este informe es orientativo y complementario. No reemplaza la evaluación médica ni constituye una historia clínica." |
| RN-INF-05 | Cada exportación del registro de controles queda registrada (pediatra, paciente, rango, fecha y hora) y puede consultarse en el registro de accesos (RF-ADM-12). |
| RN-INF-06 | Un PDF se genera en menos de 10 segundos (RNF-REN-03). |
