# 18 · Informes PDF

Se generan en el backend (ADR-10): una sola implementación, siempre con las tablas y umbrales
vigentes. Requieren conexión, como define RNF-OFF-06.

## Proceso

```mermaid
flowchart LR
    req[GET /children/id/growth-report] --> perm[AccessPolicy]
    perm --> data[Datos del período]
    data --> eval[GrowthEngine de shared]
    eval --> svg[Curvas en SVG]
    svg --> html[Plantilla Thymeleaf]
    html --> pdf[openhtmltopdf]
    pdf --> resp[application/pdf]
```

1. Se verifican permisos: tutor o pediatra de cabecera para el informe de crecimiento; pediatra
   autor para el registro de controles (RF-INF-03, CA4).
2. Se leen los datos del período y se evalúan con el mismo motor de la app.
3. Cada curva se dibuja como SVG con un generador propio en Kotlin: ejes, líneas de percentil de
   la referencia, marca del cambio OMS → SAP y trayectoria con marcadores distintos por autor
   (RF-CRE-06).
4. La plantilla HTML (Thymeleaf) arma las secciones de RN-INF-01 en orden.
5. openhtmltopdf convierte el HTML a PDF A4 con fuentes embebidas (Inter y Baloo 2 del Design
   System).
6. Se responde con `Content-Disposition: attachment; filename="Pasitos_<Nombre>_<aaaa-mm-dd>.pdf"`.

## Variantes

| Informe | Plantilla | Diferencias |
|---|---|---|
| Crecimiento, generado por tutor | `growth-report.html` | Sin clasificación técnica (RN-INF-02) |
| Crecimiento, generado por pediatra | `growth-report.html` | Con clasificación técnica |
| Registro de controles | `consultation-record.html` | Todas las consultas cerradas del autor en el rango, con observaciones y notas; leyenda en cada página; registro en `audit_log` con acción `EXPORT` (RN-INF-05) |

## Requisitos

- Generación en menos de 10 segundos (RNF-REN-03). Los PDFs no se guardan en el servidor: se
  generan, se envían y se descartan.
- Leyenda legal en el pie de cada página (RN-INF-04).
- Secciones vacías con "Sin registros en el período" (RF-INF-01, CA3).
- Pruebas: generación con datos de ejemplo de cada sección, verificación de que el PDF del tutor
  no contiene textos de clasificación técnica y de que ningún informe de crecimiento contiene
  `private_notes`.
