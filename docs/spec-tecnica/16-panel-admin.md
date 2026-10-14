# 16 · Panel de administración y páginas públicas

El mismo backend sirve, además de la API, dos conjuntos de páginas HTML generadas en el servidor
con **Thymeleaf** y **HTMX** (ADR-09):

| Ruta | Para | Contenido |
|---|---|---|
| `/admin/*` | Administradores | Panel de administración (RF-ADM-01 a 12) |
| `/web/*` | Cualquier persona con un link de email | Verificación de email, recuperación de contraseña, eliminación de cuenta desde la web |

## Acceso

| Elemento | Definición |
|---|---|
| Cuentas | Tabla `admins`, independiente de `accounts`. El primero se crea con `ADMIN_BOOTSTRAP_EMAIL`, que envía un link para definir la contraseña; los demás los crea un administrador (RF-ADM-01) |
| Sesión | Cookie `HttpOnly`, `Secure`, `SameSite=Strict`, con identificador aleatorio y estado guardado en el servidor. Vence a los 30 minutos de inactividad |
| CSRF | Token por sesión en cada formulario y en el encabezado `X-CSRF-Token` de los requests de HTMX |
| Contraseñas | bcrypt, mismas reglas que las cuentas de la app |
| Intentos fallidos | Mismo bloqueo que RN-CTA-05 |

## Páginas del panel

| Página | Funciones | RF |
|---|---|---|
| Validaciones | Lista de pediatras pendientes y en revalidación; aprobar o rechazar con motivo; link al buscador de REFEPS/SISA | RF-ADM-02 |
| Pediatras | Buscar, suspender y rehabilitar | RF-ADM-03 |
| Tablas de referencia | Importar CSV, revisar errores, publicar versión, ver historial | RF-ADM-04 |
| Parámetros de crecimiento | Umbrales, clasificación técnica, cruce de percentiles, plausibilidad, textos para tutores; cada cambio pide "validado por" | RF-ADM-05, 11 |
| Calendario | Versiones del Calendario Nacional, dosis, anticipación y tolerancia | RF-ADM-06 |
| Campañas | Alta, edición y fin | RF-ADM-07 |
| Consejos | Editor con vista previa, borrador y publicación | RF-ADM-08 |
| Hitos | Catálogo | RF-ADM-09 |
| Cronograma de controles | Edades de control | RF-ADM-10 |
| Registro de accesos | Filtros por cuenta, identificador de niño y fechas; sin datos de salud | RF-ADM-12 |
| Administradores | Alta y baja de administradores | RF-ADM-01 |

Ninguna página consulta tablas con datos de salud (`measurements`, `consultations`, `habits`,
etc.); una prueba automatizada verifica que los repositorios del panel no las usen (RN-ADM-01).

## Importación de tablas

1. El administrador sube un CSV por tabla con columnas `source, indicator, sex, axis, x, l, m, s`
   o `source, indicator, sex, axis, x, p3, p10, p25, p50, p75, p90, p97`.
2. El backend valida el formato, la monotonía de `x` y de los valores, y que los casos de control
   cargados en el sistema den el resultado esperado con la tabla nueva.
3. Si todo está bien, queda como versión en borrador; si no, muestra las filas con error y no
   guarda nada (RF-ADM-04, CA1).
4. Al publicar, el cambio se registra en `admin_audit` con "validado por" (RN-ADM-02) y la app lo
   recibe en su siguiente actualización de datos de referencia.

## Páginas públicas

| Página | Flujo |
|---|---|
| `GET /web/verify-email?token=` | Verifica el email y muestra "Tu email quedó verificado. Volvé a la app para continuar" (RF-CTA-02) |
| `GET /web/reset-password?token=` · `POST` | Formulario de contraseña nueva con las reglas de RN-CTA-01 (RF-CTA-05) |
| `GET /web/confirm-email-change?token=` | Confirma el cambio de email (RF-CTA-11) |
| `GET /web/delete-account` · `POST` | Eliminación de cuenta desde la web, requisito de Google Play (RNF-LEG-03): pide email y contraseña y aplica RN-CTA-08 |

Se usan páginas del backend en lugar de abrir la app desde el link porque funcionan desde
cualquier dispositivo, por ejemplo si el usuario lee el email en la computadora (ADR-27).
