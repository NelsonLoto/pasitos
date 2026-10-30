# 17 · Hábitos (`HAB`)

Registro simple de tomas, pañales y sueño durante los primeros 24 meses. Se usan **valores
estimados**: lo que interesa para el seguimiento es la frecuencia por día, no la duración exacta,
y no se le exige al tutor una precisión que no puede dar (D-36).

## Valores

| Hábito | Datos | Opciones |
|---|---|---|
| Toma de pecho | Lado y duración estimada | Izquierdo, derecho o ambos · Corta (menos de 10 min), media (10 a 20 min) o larga (más de 20 min) |
| Biberón | Cantidad aproximada y tipo de leche | 30, 60, 90, 120, 150 o 180 ml o más · Leche materna o fórmula |
| Pañal | Tipo | Pis, caca o ambos |
| Sueño | Tipo y duración estimada | Siesta o noche · Menos de 1 h, de 1 a 3 h o más de 3 h |

Todos los registros llevan fecha y hora, que se completan con el momento de la carga y pueden
corregirse.

## Requerimientos funcionales

### RF-HAB-01 · Registrar una toma
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí para el tutor

**Criterios de aceptación**
- **CA1.** Dado un niño menor de 24 meses, cuando el tutor elige "Pecho", un lado y una duración,
  entonces la toma se guarda con la hora actual en no más de tres toques desde la pantalla de
  hábitos.
- **CA2.** Dado "Biberón", cuando el tutor elige cantidad y tipo de leche, entonces la toma se
  guarda con la hora actual.
- **CA3.** Dada una carga tardía, cuando el tutor corrige la hora, entonces la toma se guarda con
  esa hora (RN-HAB-02).

### RF-HAB-02 · Registrar un pañal
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí para el tutor

**Criterios de aceptación**
- **CA1.** Dado un niño menor de 24 meses, cuando el tutor elige pis, caca o ambos, entonces el
  pañal se guarda con la hora actual.

### RF-HAB-03 · Registrar sueño
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí para el tutor

**Criterios de aceptación**
- **CA1.** Dado un niño menor de 24 meses, cuando el tutor elige siesta o noche y una duración
  estimada, entonces el sueño se guarda con la hora indicada.

### RF-HAB-04 · Registro diario
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí (lectura)

**Criterios de aceptación**
- **CA1.** Dado un día, cuando el usuario abre Hábitos, entonces ve los registros de ese día en
  orden horario, con su autor, y puede pasar al día anterior o siguiente.
- **CA2.** Dado el día de hoy, cuando el usuario abre Hábitos, entonces ve arriba los contadores
  del día: tomas, pañales y siestas.

### RF-HAB-05 · Resumen de hábitos
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí (lectura)

**Criterios de aceptación**
- **CA1.** Dado un niño con registros, cuando el usuario abre el resumen, entonces ve para los
  últimos 7 días el promedio diario de tomas (de pecho y biberones), de pañales (pis y caca) y de
  siestas, y los totales aproximados de biberón (ml) y sueño (horas).
- **CA2.** Dados valores calculados a partir de estimaciones, cuando se muestran, entonces llevan
  la leyenda "aproximado" (RN-HAB-04).
- **CA3.** Dado el pediatra de cabecera, cuando abre al paciente, entonces accede al mismo
  resumen.

### RF-HAB-06 · Editar o eliminar un registro
**Actores:** Tutor, Pediatra · **Incremento:** I5 · **Sin conexión:** sí para el tutor

**Criterios de aceptación**
- **CA1.** Dado un registro propio, cuando el usuario lo edita o elimina, entonces el registro
  diario y el resumen se actualizan.
- **CA2.** Dado un registro de otra persona, cuando el usuario lo abre, entonces lo ve en modo
  lectura.

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-HAB-01 | La carga de hábitos está disponible hasta que el niño cumple 24 meses de edad cronológica. Después la sección se oculta del menú, pero el historial sigue disponible desde el perfil del niño y en el informe PDF. |
| RN-HAB-02 | La fecha y hora de un registro no pueden ser futuras ni anteriores al nacimiento. |
| RN-HAB-03 | Los hábitos no generan eventos en la línea de tiempo ni notificaciones. |
| RN-HAB-04 | **Totales aproximados.** Para estimar totales se usa el valor medio de cada rango (por ejemplo, de 1 a 3 h cuenta como 2 h). Para los rangos abiertos se usa el límite conocido: "menos de 1 h" cuenta como 0,5 h, "más de 3 h" como 3 h y "180 ml o más" como 180 ml. |
| RN-HAB-05 | Los hábitos no se usan para calcular el estado del niño ni para generar alertas. |
