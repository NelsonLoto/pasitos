# Tablas originales de crecimiento de la OMS (T-02, #56)

Doce archivos XLSX originales descargados el **27/10/2026** de la Organización Mundial de la
Salud: seis indicadores de Pasitos, para niñas y niños. Contienen **17.260 filas de datos** con
L, M y S, además de las columnas z-score publicadas. Se conservan los bytes originales,
los nombres de archivo, las hojas, las columnas y todas las filas.

## Alcance y entrega a I2-D1

T-02 reúne las fuentes oficiales; I2-D1 convierte estos originales a CSV y arma los casos de
control por indicador, sexo y edad. Este paquete no importa datos en PostgreSQL ni en Room y
no cambia el cálculo o la presentación de percentiles de la app.

| Contenido | Uso |
|---|---|
| `originals/*.xlsx` | Fuentes oficiales intactas, con sus nombres de descarga |
| `manifest.json` | Indicador, sexo, página de origen, URL exacta, fecha UTC, cabeceras HTTP, tamaño, SHA-256 y estructura de cada tabla |
| `verify.py` | Verificación local de integridad y cobertura, y recuperación opcional de originales ausentes |

El manifiesto usa las claves de indicador previstas por la SPEC técnica. `LENGTH_FOR_AGE`
corresponde a longitud/talla para la edad; el archivo oficial `lhfa` contiene ambos tramos.
Las claves `FEMALE` y `MALE` corresponden respectivamente a los originales `girls` y `boys`.

## Fuentes y cobertura

Se eligió la sección **Expanded tables for constructing national health cards**, archivos
**z-scores**, de cada página oficial. Estos XLSX incluyen LMS y las columnas SD; no son las
tablas mensuales ni las tablas simplificadas para uso en campo. Descargar también la versión
por percentiles duplicaría los mismos parámetros LMS y no es necesario para T-02.

| Indicador | Fuente oficial | Eje original | Filas por sexo | Unidad de la medida |
|---|---|---|---:|---|
| Peso para la edad | [OMS: weight-for-age](https://www.who.int/tools/child-growth-standards/standards/weight-for-age) | Día 0 a 1856, paso 1 | 1.857 | kg |
| Longitud/talla para la edad | [OMS: length/height-for-age](https://www.who.int/tools/child-growth-standards/standards/length-height-for-age) | Día 0 a 1856, paso 1 | 1.857 | cm |
| Perímetro cefálico para la edad | [OMS: head circumference-for-age](https://www.who.int/tools/child-growth-standards/standards/head-circumference-for-age) | Día 0 a 1856, paso 1 | 1.857 | cm |
| IMC para la edad | [OMS: BMI-for-age](https://www.who.int/toolkits/child-growth-standards/standards/body-mass-index-for-age-bmi-for-age) | Día 0 a 1856, paso 1 | 1.857 | kg/m² |
| Peso para la longitud | [OMS: weight-for-length/height](https://www.who.int/tools/child-growth-standards/standards/weight-for-length-height) | Longitud 45 a 110 cm, paso 0,1 | 651 | kg |
| Peso para la talla | [OMS: weight-for-length/height](https://www.who.int/tools/child-growth-standards/standards/weight-for-length-height) | Talla 65 a 120 cm, paso 0,1 | 551 | kg |

Los enlaces directos a cada descarga, incluidos sus parámetros de revisión `sfvrsn`, están en
`manifest.json`. `last_modified` conserva la cabecera del servidor, si existe; no se interpreta
como fecha de publicación de la referencia clínica.

## Estructura de los originales

Cada archivo tiene una hoja y 13 columnas:

```text
Day o Length o Height, L, M, S, SD4neg, SD3neg, SD2neg, SD1neg, SD0, SD1, SD2, SD3, SD4
```

`Day` se expresa en días; `Length` y `Height`, en centímetros. Las columnas `SD` contienen las
medidas publicadas para los distintos puntajes Z. En el manifiesto `measurement_unit` registra
la unidad de esas medidas y de M; `L` y `S` se conservan como parámetros del original.

Excel representa algunos números con pequeñas diferencias binarias. No se corrigieron ni
redondearon los XLSX. La verificación del eje admite una diferencia menor a 0,00000001 respecto
a la grilla esperada; para comparar M con SD0 se contempla el redondeo publicado a tres decimales.

Los originales diarios llegan a 1856 días. Conservarlos completos no modifica RN-CRE-02:
Pasitos selecciona la referencia según la edad y las reglas de la SPEC; desde los 60 meses
corresponde SAP. No recortar, suavizar ni mezclar los originales OMS con tablas SAP en esta tarea.

## Verificación reproducible

Desde la raíz del repositorio, con **Python 3.10 o superior**, sin instalar paquetes:

```sh
python backend/seed/growth/who/verify.py
```

El comando solo lee los archivos existentes y comprueba:

- Las doce combinaciones de indicador y sexo, sin duplicados ni faltantes.
- Tamaño y SHA-256 de cada archivo respecto del manifiesto, antes de abrir su contenido.
- Integridad ZIP/XLSX, nombre de hoja y columnas esperadas.
- Todas las filas del eje, sin huecos ni duplicados, con el paso de días o de 0,1 cm correspondiente.
- Valores numéricos finitos; M y S positivos; columnas SD ordenadas y coherencia de SD0 con M.
- Ausencia de fórmulas y errores de Excel en las celdas de las tablas originales.

Si falta un original, se puede recuperar desde su URL oficial fijada:

```sh
python backend/seed/growth/who/verify.py --download-missing
```

Esta opción descarga únicamente archivos ausentes. Exige HTTPS de `cdn.who.int` y valida el
contenido contra el tamaño y SHA-256 registrados antes de guardarlo. No reemplaza archivos
existentes, aunque estén alterados. Si la OMS cambia una descarga, el comando falla: revisar la
fuente y su nueva versión antes de actualizar el manifiesto; no aceptar automáticamente otro hash.

Los SHA-256 fueron calculados sobre las descargas de esta entrega. Detectan cambios posteriores;
no son sumas publicadas ni firmas digitales emitidas por la OMS.

## Validaciones realizadas

- Los doce XLSX se abrieron e inspeccionaron con la herramienta de planillas, sin exportarlos
  ni modificar sus bytes. Coincidieron hojas, encabezados, rangos y extremos de los ejes.
- `verify.py` recorrió los doce originales y las 17.260 filas LMS completas.
- Las pruebas de rechazo de un archivo alterado y de un original ausente, y la recuperación de
  un faltante, se ejecutan sobre copias descartables para conservar intacta esta colección.

Esto verifica la entrega de fuentes de T-02. Las pruebas numéricas del motor y los casos de
control con los valores SD corresponden a I2-D1 e I2-S1; no se consideran ejecutados aquí.

## Atribución y referencias

Fuente: **World Health Organization, WHO Child Growth Standards**, tablas expandidas z-score,
consultadas y descargadas el 27/10/2026. Los archivos de terceros conservan sus condiciones
originales; esta entrega no les asigna la licencia del código de Pasitos. Consultar las
[instrucciones de la OMS](https://www.who.int/tools/child-growth-standards/standards) y sus
[condiciones de uso](https://www.who.int/about/policies/publishing/copyright).

- [Tarea T-02](../../../../docs/plan/transversales.md).
- [Entrega siguiente I2-D1](../../../../docs/plan/I2-a-I6-preliminar.md).
- [Formato técnico de datos de referencia](../../../../docs/spec-tecnica/10-modulo-compartido.md#datos-de-referencia).
