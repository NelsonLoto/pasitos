# 01 · Actores y permisos

## 1. Actores

| Actor | Descripción |
|---|---|
| **Visitante** | Persona sin sesión iniciada. Solo puede registrarse, iniciar sesión o recuperar su contraseña. |
| **Tutor** | Madre, padre o representante legal de un niño. Da de alta a sus hijos, carga datos, consulta el seguimiento y gestiona el vínculo con el pediatra. |
| **Tutor principal** | El tutor que dio de alta al niño (o a quien se le transfirió el rol). Además de lo que hace cualquier tutor, gestiona los demás tutores, el vínculo con el pediatra y la eliminación del perfil. Hay exactamente uno por niño. |
| **Tutor secundario** | Tutor invitado por el principal. Carga y consulta datos del niño con los mismos permisos de carga que el principal. |
| **Pediatra** | Profesional con matrícula validada por el administrador. Atiende pacientes vinculados por sus tutores y pacientes propios, y conduce la consulta guiada. |
| **Pediatra de cabecera** | El pediatra con vínculo activo con un niño. Hay como máximo uno por niño (RN-PED-01). |
| **Pediatra anterior** | Pediatra cuyo vínculo con el niño terminó. Solo conserva acceso de lectura a sus propias consultas. |
| **Administrador** | Integrante del equipo que opera el panel web: valida pediatras y gestiona parámetros clínicos y contenido. No accede a datos de salud de los niños. |
| **Sistema** | Actor automático: calcula percentiles y estados, genera eventos automáticos, envía notificaciones, vence invitaciones y finaliza perfiles. |

## 2. Perfiles de una cuenta

Una cuenta pertenece a una persona y puede tener uno o ambos perfiles (D-06):

- **Perfil de tutor**: se activa al registrarse como tutor o al dar de alta el primer hijo.
- **Perfil de pediatra**: se solicita desde el registro o desde la configuración, y queda
  *pendiente de validación* hasta que el administrador lo aprueba (RF-PED-01, RF-ADM-02).

Si la cuenta tiene ambos perfiles, la app muestra un selector de modo (RF-CTA-09). Los datos de
un modo no se mezclan con los del otro: por ejemplo, los hijos de una pediatra no aparecen en su
lista de pacientes salvo que ella misma se vincule como su pediatra.

Las cuentas de administrador son independientes: no se crean desde la app y no pueden tener
perfil de tutor ni de pediatra (RF-ADM-01).

## 3. Matriz de permisos sobre los datos de un niño

✅ permitido · ❌ no permitido · ✏️ solo sobre los registros propios

| Acción | Tutor principal | Tutor secundario | Pediatra de cabecera | Pediatra anterior | Administrador |
|---|---|---|---|---|---|
| Ver perfil del niño (datos básicos, nacimiento, alergias, antecedentes) | ✅ | ✅ | ✅ | ❌ ¹ | ❌ |
| Editar perfil del niño | ✅ | ✅ | ❌ | ❌ | ❌ |
| Ver mediciones, vacunas, eventos, hábitos e hitos | ✅ | ✅ | ✅ | ❌ | ❌ |
| Cargar mediciones, vacunas, eventos, hábitos e hitos | ✅ | ✅ | ✅ | ❌ | ❌ |
| Editar o eliminar esos registros | ✏️ | ✏️ | ✏️ | ❌ | ❌ |
| Ver percentil exacto y puntaje Z en la app | ❌ ² | ❌ ² | ✅ | ❌ | ❌ |
| Ver la clasificación técnica del estado | ❌ | ❌ | ✅ | ❌ | ❌ |
| Ver el estado en lenguaje simple | ✅ | ✅ | ✅ | ❌ | ❌ |
| Iniciar y cerrar consultas guiadas | ❌ | ❌ | ✅ | ❌ | ❌ |
| Ver consultas: fecha y profesional | ✅ | ✅ | ✅ | ✏️ | ❌ |
| Ver consultas: datos cargados e indicaciones para la familia | ✅ | ✅ | ✏️ ³ | ✏️ | ❌ |
| Ver observaciones de una consulta | ❌ | ❌ | ✏️ | ✏️ | ❌ |
| Exportar el registro de controles | ❌ | ❌ | ✏️ | ✏️ | ❌ |
| Generar el informe de crecimiento | ✅ | ✅ | ✅ | ❌ | ❌ |
| Invitar o quitar tutores, transferir el rol principal | ✅ | ❌ ⁴ | ❌ | ❌ | ❌ |
| Solicitar, cambiar o revocar el pediatra | ✅ | ❌ | ❌ | ❌ | ❌ |
| Eliminar el perfil del niño (solo si es su único tutor; si hay otros, equivale a dejar de ser tutor, RF-HIJ-09) | ✅ | ❌ | ❌ ⁵ | ❌ | ❌ |
| Ver el historial de vínculos con pediatras | ✅ | ✅ | ❌ | ❌ | ❌ |
| Ver el registro de accesos | ❌ | ❌ | ❌ | ❌ | ✅ ⁶ |

1. El pediatra anterior ve nombre, apellido y fecha de nacimiento del niño únicamente dentro de
   sus propias consultas (RN-PED-04).
2. El tutor ve percentil y franja en la app; el percentil exacto y el puntaje Z aparecen en el
   informe PDF que genera (RN-INF-02).
3. De las consultas de otros pediatras, el pediatra de cabecera ve fecha, profesional y los datos
   objetivos que se incorporaron al historial (mediciones, vacunas), pero no las indicaciones ni
   las observaciones (RN-PED-05).
4. Un tutor secundario puede abandonar el perfil por su cuenta (RF-HIJ-07).
5. El pediatra puede eliminar sus pacientes propios mientras no tengan tutor (RN-PED-08).
6. El registro de accesos identifica quién accedió, cuándo y a qué tipo de dato, sin mostrar los
   datos de salud (RF-ADM-12).

La asignación de la gestión del pediatra y de los tutores solo al tutor principal es un supuesto a
confirmar (SUP-01).

## 4. Permisos del pediatra fuera de un niño concreto

| Acción | Pediatra pendiente de validación | Pediatra validado | Pediatra suspendido |
|---|---|---|---|
| Aparecer en la búsqueda por matrícula | ❌ | ✅ | ❌ |
| Recibir y aceptar solicitudes de vínculo | ❌ ⁷ | ✅ | ❌ |
| Dar de alta pacientes propios | ❌ | ✅ | ❌ |
| Ver su lista de pacientes | ❌ | ✅ | ❌ |
| Usar su perfil de tutor, si lo tiene | ✅ | ✅ | ✅ |

7. Las solicitudes que llegaron por una invitación (RF-PED-09) quedan en espera hasta que el
   pediatra es validado.
