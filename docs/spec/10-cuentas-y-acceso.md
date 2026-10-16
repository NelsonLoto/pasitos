# 10 · Cuentas y acceso (`CTA`)

## Requerimientos funcionales

### RF-CTA-01 · Registro con email y contraseña
**Actores:** Visitante · **Incremento:** I1 · **Sin conexión:** no

El visitante crea una cuenta con nombre, apellido, email y contraseña, e indica si se registra como
tutor, como pediatra o ambos. Debe aceptar en forma expresa los términos y condiciones y la
política de privacidad (RN-CTA-02).

**Criterios de aceptación**
- **CA1.** Dado un email que no existe en el sistema, cuando el visitante completa los datos
  válidos y acepta los términos, entonces se crea la cuenta en estado *email sin verificar* y se
  envía el email de verificación (RF-CTA-02).
- **CA2.** Dado un email que ya pertenece a una cuenta, cuando el visitante intenta registrarse,
  entonces se informa que el email ya está en uso y se ofrece iniciar sesión o recuperar la
  contraseña.
- **CA3.** Dada una contraseña que no cumple RN-CTA-01, cuando el visitante intenta registrarse,
  entonces se indica qué regla no cumple y no se crea la cuenta.
- **CA4.** Dado que el visitante no marcó la aceptación de términos, cuando intenta registrarse,
  entonces el registro no avanza. La casilla nunca aparece premarcada.
- **CA5.** Dado que el visitante elige registrarse como pediatra, cuando completa el registro,
  entonces se le piden además los datos profesionales de RF-PED-01.

### RF-CTA-02 · Verificación de email
**Actores:** Visitante, Sistema · **Incremento:** I1 · **Sin conexión:** no

Toda cuenta creada con email y contraseña debe verificar su email antes de poder usar la app.

**Criterios de aceptación**
- **CA1.** Dada una cuenta con email sin verificar, cuando el usuario inicia sesión, entonces ve
  una pantalla que le pide verificar el email, con la opción de reenviar el mensaje, y no puede
  acceder al resto de la app.
- **CA2.** Dado un enlace de verificación vigente, cuando el usuario lo abre, entonces el email
  queda verificado y puede continuar.
- **CA3.** Dado un enlace vencido o ya usado, cuando el usuario lo abre, entonces se informa el
  problema y se ofrece reenviar la verificación.

### RF-CTA-03 · Registro e inicio de sesión con Google
**Actores:** Visitante · **Incremento:** I1 · **Sin conexión:** no

El visitante puede registrarse o iniciar sesión con su cuenta de Google. El email de Google se
considera verificado.

**Criterios de aceptación**
- **CA1.** Dado que es la primera vez que esa cuenta de Google ingresa, cuando el visitante
  completa el inicio con Google, entonces debe aceptar los términos (RN-CTA-02) e indicar si es
  tutor, pediatra o ambos antes de usar la app.
- **CA2.** Dado que el email de Google coincide con una cuenta existente con email verificado,
  cuando el visitante inicia sesión con Google, entonces ingresa a esa misma cuenta (RN-CTA-04).
- **CA3.** Dado que el visitante cancela el flujo de Google, cuando vuelve a la app, entonces
  permanece en la pantalla de inicio de sesión sin errores.

### RF-CTA-04 · Inicio de sesión
**Actores:** Visitante · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dados un email y una contraseña correctos, cuando el visitante inicia sesión,
  entonces accede a la app en el último modo que usó (tutor o pediatra).
- **CA2.** Dadas credenciales incorrectas, cuando el visitante inicia sesión, entonces se informa
  "Email o contraseña incorrectos" sin indicar cuál de los dos falló.
- **CA3.** Dados 5 intentos fallidos consecutivos sobre la misma cuenta, cuando se intenta un
  sexto, entonces la cuenta se bloquea temporalmente para el inicio con contraseña (RN-CTA-05).

### RF-CTA-05 · Recuperación de contraseña
**Actores:** Visitante · **Incremento:** I1 · **Sin conexión:** no

**Criterios de aceptación**
- **CA1.** Dado cualquier email, cuando el visitante pide recuperar la contraseña, entonces se
  muestra el mismo mensaje exista o no la cuenta ("Si el email está registrado, te enviamos un
  enlace"), para no revelar qué emails están registrados.
- **CA2.** Dado un enlace de recuperación vigente, cuando el usuario define una contraseña que
  cumple RN-CTA-01, entonces la contraseña se actualiza y se cierran todas las sesiones abiertas
  de la cuenta.
- **CA3.** Dado un enlace vencido (RN-CTA-06) o ya usado, cuando el usuario lo abre, entonces se
  informa el problema y se ofrece pedir uno nuevo.

### RF-CTA-06 · Sesión persistente
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** sí

La sesión se mantiene abierta mientras la app se use al menos una vez cada 30 días (RN-CTA-07).

**Criterios de aceptación**
- **CA1.** Dado un usuario con sesión iniciada que vuelve a abrir la app dentro de los 30 días,
  cuando la abre, entonces ingresa sin volver a escribir sus credenciales.
- **CA2.** Dado un usuario que no abrió la app durante más de 30 días, cuando la abre, entonces
  debe iniciar sesión de nuevo.
- **CA3.** Dado un usuario sin conexión con sesión iniciada, cuando abre la app, entonces accede a
  los datos descargados (RNF-OFF-01).

### RF-CTA-07 · Cierre de sesión
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** sí

**Criterios de aceptación**
- **CA1.** Dado un usuario con sesión iniciada, cuando cierra sesión, entonces la sesión queda
  revocada en el servidor y se borran del dispositivo los datos descargados de la cuenta.
- **CA2.** Dado que hay registros cargados sin conexión pendientes de sincronizar, cuando el
  usuario intenta cerrar sesión, entonces se le advierte que se perderán y debe confirmar.
- **CA3.** Dado un usuario sin conexión, cuando cierra sesión, entonces los datos del dispositivo
  se borran igual y la sesión se revoca en el servidor apenas el dispositivo recupera la conexión
  (D-67).

### RF-CTA-08 · Bloqueo de la app con huella o PIN
**Actores:** Tutor, Pediatra · **Incremento:** I6 · **Sin conexión:** sí

El usuario puede activar un bloqueo que pide huella digital (o el método biométrico del
dispositivo) o un PIN al abrir la app. Es opcional para todos y la app lo recomienda a los
pediatras al validar su perfil.

**Criterios de aceptación**
- **CA1.** Dado un usuario con el bloqueo activado, cuando abre la app o vuelve a ella después de
  5 minutos en segundo plano, entonces debe desbloquearla antes de ver cualquier dato.
- **CA2.** Dado un usuario que olvidó su PIN, cuando elige "Olvidé mi PIN", entonces se cierra la
  sesión y debe iniciar sesión con sus credenciales, tras lo cual puede definir un PIN nuevo.
- **CA3.** Dado un dispositivo sin biometría, cuando el usuario activa el bloqueo, entonces solo
  se ofrece el PIN.

### RF-CTA-09 · Perfiles de la cuenta y selector de modo
**Actores:** Tutor, Pediatra · **Incremento:** I1 (perfil de tutor) e I3 (perfil de pediatra) · **Sin conexión:** sí

**Criterios de aceptación**
- **CA1.** Dada una cuenta con un solo perfil, cuando el usuario usa la app, entonces no ve el
  selector de modo.
- **CA2.** Dada una cuenta con perfil de tutor, cuando el usuario solicita agregar el perfil de
  pediatra desde la configuración, entonces completa los datos de RF-PED-01 y el perfil queda
  pendiente de validación.
- **CA3.** Dada una cuenta con ambos perfiles, cuando el usuario cambia de modo, entonces la
  navegación, los datos y las notificaciones visibles corresponden al modo elegido.

### RF-CTA-10 · Eliminación de cuenta
**Actores:** Tutor, Pediatra · **Incremento:** I6 · **Sin conexión:** no

El usuario puede eliminar su cuenta desde la app. Es un requisito de Google Play.

**Criterios de aceptación**
- **CA1.** Dado un usuario que elige eliminar su cuenta, cuando inicia el proceso, entonces se le
  muestra un resumen de las consecuencias según RN-CTA-08 (qué hijos se eliminan, qué hijos quedan
  con otro tutor, qué pacientes se desvinculan) y debe confirmar escribiendo una palabra clave o
  reingresando su contraseña.
- **CA2.** Dada la confirmación, cuando se procesa la eliminación, entonces se aplican las reglas
  de RN-CTA-08, se cierran todas las sesiones y se notifica a los usuarios afectados (otros
  tutores, tutores de pacientes vinculados).
- **CA3.** Dada una cuenta eliminada, cuando alguien intenta iniciar sesión con ese email,
  entonces el sistema se comporta como con un email no registrado.

### RF-CTA-11 · Edición de datos personales
**Actores:** Tutor, Pediatra · **Incremento:** I1 · **Sin conexión:** no

El usuario puede modificar su nombre, apellido, email y contraseña.

**Criterios de aceptación**
- **CA1.** Dado un cambio de email, cuando el usuario lo confirma, entonces el email nuevo debe
  verificarse (RF-CTA-02) antes de reemplazar al anterior, y se avisa al email anterior.
- **CA2.** Dado un cambio de contraseña, cuando el usuario lo confirma, entonces debe ingresar la
  contraseña actual y la nueva debe cumplir RN-CTA-01.
- **CA3.** Dado un pediatra validado que cambia su nombre o apellido, cuando lo confirma,
  entonces su perfil profesional pasa a *pendiente de revalidación* según RN-PED-02.

## Reglas de negocio

| ID | Regla |
|---|---|
| RN-CTA-01 | La contraseña tiene al menos 8 caracteres y contiene al menos una letra y un número. |
| RN-CTA-02 | La aceptación de términos y política de privacidad es expresa (casilla no premarcada). Se registran la versión de los términos aceptados y la fecha y hora de aceptación. Si los términos cambian, el usuario debe aceptar la nueva versión al ingresar. |
| RN-CTA-03 | El email es único en el sistema y se usa como identificador de la cuenta. |
| RN-CTA-04 | Si un inicio con Google trae un email que coincide con una cuenta existente con email verificado, se ingresa a esa cuenta y queda asociado el inicio con Google. Si la cuenta existente no verificó su email, se pide verificarlo primero. |
| RN-CTA-05 | Tras 5 intentos fallidos consecutivos de inicio con contraseña, la cuenta se bloquea 15 minutos para ese método. El inicio con Google y la recuperación de contraseña siguen disponibles. Valores propuestos (SUP-05). |
| RN-CTA-06 | Los enlaces de verificación de email vencen a las 24 horas y los de recuperación de contraseña a los 60 minutos. Valores propuestos (SUP-04). |
| RN-CTA-07 | La sesión vence tras 30 días sin uso de la app. Cualquier uso renueva el plazo. |
| RN-CTA-08 | **Eliminación de cuenta.** Si la cuenta tiene perfil de tutor: cada hijo del que es el único tutor se elimina con todos sus datos (RN-HIJ-08); en cada hijo con otros tutores, el usuario deja de ser tutor y, si era principal, el rol pasa al tutor secundario más antiguo. Si la cuenta tiene perfil de pediatra: sus pacientes propios sin tutor se eliminan; sus vínculos activos terminan y se notifica a los tutores; las mediciones, vacunas, eventos y hábitos que cargó permanecen en el historial de cada niño identificados como "cargado por un profesional"; sus consultas, observaciones y notas se eliminan. |
| RN-CTA-09 | Las consultas cerradas de un pediatra sobre un niño eliminado por su tutor se conservan en el registro de ese pediatra, con nombre, apellido y fecha de nacimiento del niño como únicos datos de identificación (D-52). |
