# Datos ficticios de demostración (T-18, #70)

Diseño y carga para las demos desde I2 y el piloto con niños inventados (D-65).
No corresponde a personas reales ni sirve para validar criterios clínicos.

## Escenarios previstos

| Clave | Edad al cargar | Caso |
|---|---|---|
| esperado | 6 meses | Trayectoria propuesta para Dentro de lo esperado; vacunas al día |
| cruce | 12 meses | Trayectoria propuesta para A controlar por cruce de percentiles; vacunas atrasadas |
| consulta | 18 meses | Trayectoria propuesta para Consultar con el pediatra; dosis retroactivas, una sin fecha |
| prematuro | 4 meses, nacido a las 32 semanas | Mediciones antes y después del término; edad corregida |
| escolar | 6 años | Mediciones antes y después de 60 meses; talla de pie y sin perímetro cefálico desde los 5 años |

Se incluyen dos tutores para demostrar el acceso compartido y una cuenta independiente para
comprobar el aislamiento de los registros.

## Estado de la tarea

En implementación: archivo de datos, cargador explícito, instrucciones y pruebas PostgreSQL.
El motor de crecimiento y las tablas OMS/SAP todavía no están disponibles en este checkout.
Los tres niveles y el cruce son objetivos de los escenarios, pendientes de comprobar en I2;
no se guardan percentiles ni clasificaciones inventadas en la base de datos.

Referencias: [tarea transversal](../../../docs/plan/transversales.md),
[crecimiento](../../../docs/spec/12-crecimiento.md),
[D-65](../../../docs/spec/91-decisiones.md).
