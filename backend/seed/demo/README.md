# Datos ficticios para demos y piloto (T-18, #70)

Cinco niños inventados y tres cuentas ficticias para demos desde I2 y el piloto (D-65).
No corresponden a personas reales. Los nombres llevan el apellido **Demo T18** y los emails
usan `demo.pasitos.invalid`. No se envían correos, no se crean sesiones ni se simula Google.

## Escenarios

| Clave | Edad al cargar | Diseño de crecimiento | Vacunas y tutores |
|---|---|---|---|
| `esperado` (Alma) | 6 meses | Trayectoria propuesta para **Dentro de lo esperado** | Al día; principal y secundario, ambos con mediciones propias |
| `cruce` (Bruno) | 12 meses | Trayectoria propuesta para **A controlar**, por cruce de al menos dos líneas en una referencia | Atrasadas: solo dos dosis al nacimiento registradas; dos tutores |
| `consulta` (Ciro) | 18 meses | Trayectoria propuesta para **Consultar con el pediatra**, por peso sobre p97 | Carga retroactiva: aplicaciones históricas y una sin fecha; dos tutores |
| `prematuro` (Dara) | 4 meses; 32 semanas al nacer | Medición a los 14 días, antes del término, y a los 2 y 4 meses; edad corregida | Vacunas con edad cronológica, al día; dos tutores |
| `escolar` (Ema) | 6 años | Mediciones a los 59, 60 y 72 meses para el cambio OMS/SAP | Al día; otro tutor, aislado de los cuatro perfiles anteriores |

Las edades son relativas al día de la **primera carga**, calculado con `ArgentinaClock`.
Se guardan fechas concretas: los niños y las mediciones envejecen normalmente después.
Cada niño tiene medidas al nacer y tres mediciones posteriores. Desde 60 meses se genera talla
de pie y no se carga perímetro cefálico. Las vacunas se toman del calendario publicado vigente,
respetando cohortes; no se inventa un calendario ni se registran aplicaciones futuras.

### Validación de crecimiento pendiente en I2

Los valores de `scenarios.json` son propuestas para demostrar los estados, **no casos clínicos
validados**. Este checkout solo tiene validaciones de mediciones; todavía no tiene el motor de
percentiles ni las tablas OMS/SAP. Por eso el cargador guarda mediciones, no percentiles ni estados.

Antes de dar por aceptada la cobertura de crecimiento de T-18 para la demo de I2:

- [ ] Ejecutar los escenarios con las tablas y el motor de I2-D1, I2-D2 e I2-S2.
- [ ] Confirmar los tres niveles por indicador y el resumen con los umbrales vigentes; ajustar los
  valores ficticios si fuera necesario, sin alterar los umbrales para hacer pasar una demo.
- [ ] Comprobar que `cruce` cruza al menos dos líneas de la misma referencia y termina en
  **A controlar**. No evaluar cruce entre las mediciones de 59 y 60 meses de `escolar`.
- [ ] Comprobar que el punto de `prematuro` a los 14 días tiene edad corregida negativa, aparece
  en el historial y no recibe percentil ni se ubica en la curva.
- [ ] Revisar plausibilidad con I2: confirmar valores atípicos solo si corresponde, sin eludir
  la confirmación ni marcar automáticamente todos los valores como confirmados.
- [ ] Ejecutar la aceptación manual de las pantallas en dispositivo y comunicar el resultado
  a las tareas dependientes de crecimiento y al piloto.

Para la demo de I2 vence el **25/11/2026**; la revisión completa debe hacerse antes del piloto.
Estos pendientes no justifican implementar aquí el motor de crecimiento ni funciones de I3.

## Carga local

Requiere JDK 21, PostgreSQL con las migraciones actuales y la configuración habitual del backend
(`backend/.env` o variables de entorno). No cambia versiones ni agrega dependencias o migraciones.

Usá una base dedicada a datos ficticios. `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`
seleccionan la conexión; `DEMO_DATABASE` confirma exactamente el nombre configurado en `DB_NAME`.
Confirmar ese nombre no cambia la conexión ni crea una base nueva.

Desde `backend/`, primero cargá el calendario de referencia si la base es nueva:

```powershell
.\gradlew.bat seedReferenceData
```

Vista previa en Windows, sin conexión a la base ni contraseña:

```powershell
.\seed\demo\cargar.ps1 -Preview
```

Carga en Windows (reemplazá el ejemplo por el nombre de tu base ficticia ya configurada):

```powershell
.\seed\demo\cargar.ps1 -Database pasitos_demo
```

El script solicita una contraseña sin mostrarla. Las tres cuentas comparten esa contraseña para
la demo: mínimo 12 caracteres con letra y número, máximo 72 bytes UTF-8 por bcrypt. No uses una
contraseña personal. El script la pasa temporalmente al proceso y restaura el entorno al salir;
no la escribe en el repositorio ni en el historial de comandos. El cargador guarda solo su hash.

En otros sistemas se puede usar la misma tarea Gradle. Para la carga, proporcionar `DEMO_DATABASE`
y `DEMO_PASSWORD` por el entorno del proceso, sin incluir la contraseña en argumentos o archivos
versionados. La vista previa no requiere esas variables:

```sh
./gradlew seedReferenceData
./gradlew seedDemoData --args=--preview
./gradlew --no-daemon seedDemoData
```

Las cuentas se crean verificadas y con los consentimientos de las versiones configuradas,
exclusivamente como fixtures. Eso no representa aceptación legal por participantes reales.
El cargador es una herramienta explícita: no tiene endpoint y no se ejecuta al iniciar el backend.
En piloto debe invocarla el administrador únicamente contra la base destinada a datos ficticios,
con su configuración correspondiente; la gestión del calendario del piloto sigue su flujo propio.

## Acceso para la demo

| Cuenta | Perfiles visibles |
|---|---|
| `principal@demo.pasitos.invalid` | Alma, Bruno, Ciro y Dara; tutor principal |
| `secundario@demo.pasitos.invalid` | Los mismos cuatro; tutor secundario |
| `aislado@demo.pasitos.invalid` | Solo Ema; tutor principal |

Ingresar con **email y contraseña**. El vínculo entre tutores ya está cargado: este conjunto
sirve para demostrar el resultado del acceso compartido, no el envío y aceptación de invitaciones.
Cada autor puede editar sus mediciones; las del otro tutor deben verse como solo lectura.
Las cuentas `.invalid` no sirven para probar recepción real de emails ni Google Sign-In.

## Repetición y fallos

La carga de cuentas, consentimientos, vínculos, mediciones y dosis se hace en **una transacción**.
Un lock de PostgreSQL evita que dos ejecuciones del cargador dupliquen el conjunto.
Los UUID estables identifican los fixtures y los triggers existentes generan `sync_version`.

- Primera ejecución: valida el archivo y el calendario, y crea el conjunto completo.
- Si ya existen las tres cuentas y los cinco niños con sus UUID de demo: no cambia nada. Conserva
  fechas, contraseñas, consentimientos y las ediciones hechas durante la demo; no repone mediciones
  o dosis que alguien haya eliminado.
- Si falta una parte del conjunto o un email reservado pertenece a otra cuenta: falla sin
  sobrescribir ni vincular cuentas existentes. No hay borrado ni reparación automáticos.
- Si falla una inserción o se interrumpe el proceso antes del commit: PostgreSQL revierte la carga.
  Si el commit ocurrió y se perdió el mensaje de éxito, repetir no duplica.
- Las mediciones y dosis envejecen desde la primera carga. Para repetir desde cero o probar una
  versión modificada del archivo, usar otra base ficticia vacía y configurarla explícitamente.
  No borrar la base de trabajo ni la del piloto para reiniciar una demo.

## Verificación

Las pruebas del cargador usan PostgreSQL descartable con Testcontainers; no leen `backend/.env`
ni cargan datos sobre la base de desarrollo:

```sh
./gradlew test --tests 'com.pasitos.backend.seed.DemoSeederTest'
```

Cubren login y sincronización por HTTP, permisos de tutores, hashes, consentimientos, estados de vacunas, retroactividad,
repetición preservando ediciones, cargas concurrentes, rollback después de una inserción fallida,
colisiones y rechazos sin carga parcial, fechas de fin de mes,
bisiestos y restricciones de mediciones desde los 5 años. La aceptación visual y los resultados
del motor de crecimiento requieren la validación manual indicada arriba.

Referencias: [tarea transversal](../../../docs/plan/transversales.md),
[crecimiento](../../../docs/spec/12-crecimiento.md),
[decisiones D-65](../../../docs/spec/91-decisiones.md),
[calendario de referencia](../calendar/README.md).
