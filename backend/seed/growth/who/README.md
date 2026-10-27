# Tablas originales de crecimiento de la OMS (T-02, #56)

Archivos oficiales expandidos para los seis indicadores previstos por Pasitos, en ambos sexos.
Se conservan como XLSX originales, con su fuente y un manifiesto de integridad.

## Alcance

- Descargar las tablas expandidas con L, M y S por día o por longitud/talla.
- Registrar URL, fecha, tamaño y SHA-256; comprobar cobertura y estructura de los originales.
- Entregar el material a I2-D1, que convierte a CSV y construye los casos de control.

La conversión a CSV, el motor de percentiles y su integración en la app pertenecen a I2.

## Fuentes oficiales

- [Peso para la edad](https://www.who.int/tools/child-growth-standards/standards/weight-for-age).
- [Longitud/talla para la edad](https://www.who.int/tools/child-growth-standards/standards/length-height-for-age).
- [Perímetro cefálico para la edad](https://www.who.int/tools/child-growth-standards/standards/head-circumference-for-age).
- [Peso para la longitud/talla](https://www.who.int/tools/child-growth-standards/standards/weight-for-length-height).
- [IMC para la edad](https://www.who.int/toolkits/child-growth-standards/standards/body-mass-index-for-age-bmi-for-age).

Se selecciona la sección **Expanded tables for constructing national health cards**, no las
planillas mensuales ni las tablas simplificadas. Se usan los archivos z-score que incluyen LMS.

Estado inicial: descarga e inspección en curso. No hay carga automática en PostgreSQL ni en la app.
