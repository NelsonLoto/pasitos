# 92 · Pendientes técnicos

## Decisiones y tareas abiertas

| ID | Pendiente | Responsable | Fecha límite | Referencia |
|---|---|---|---|---|
| P-01 | Elegir el proveedor de hosting del piloto según los requisitos de [30](30-infraestructura.md#requisitos-para-elegir-el-proveedor-p-01) | Equipo | Antes de I6 | ADR-05 |
| P-02 | Confirmar si se renombra el repositorio en GitHub (`control-nino-sano-app` → `pasitos`). Si se hace, cada integrante actualiza su remoto con `git remote set-url` | Equipo | I0 | ADR-12 |
| P-03 | Registrar un dominio (por ejemplo, `.ar` en NIC Argentina o `.app`), necesario para HTTPS del piloto y para el email con SPF y DKIM | Equipo | Antes de I6 | ADR-07 |
| P-04 | Crear la cuenta del proveedor de email en región UE | Equipo | I1 (verificación de email) | ADR-07 |
| P-05 | Crear el proyecto de Firebase con una cuenta de Google del equipo (no personal) y registrar `com.pasitos.app` | Equipo | I1 (Google Sign-In) | ADR-06, ADR-13 |
| P-06 | Cuenta de desarrollador de Google Play | Nelson | Antes del piloto | ADR-13 |
| P-07 | Crear la organización de Sentry en región UE | Equipo | I0 | ADR-08 |
| P-08 | Definir dónde se guarda la clave de subida de la app (gestor de contraseñas compartido) | Equipo | Antes de la primera subida a Play | [30](30-infraestructura.md#publicación-de-la-app) |
| P-09 | Descargar las tablas expandidas de la OMS (L, M, S por día y por longitud o talla) y convertirlas al formato de importación | Equipo | I2 | [10](10-modulo-compartido.md#datos-de-referencia) |
| P-10 | Confirmar si el libro de la SAP publica L, M y S o solo percentiles (define si el puntaje Z de 5 a 15 años es exacto o aproximado) | Equipo con pediatras | I2 | PVM-14 |
| P-11 | Elegir dónde dejar la nota para agentes de IA de [32](32-proceso-de-desarrollo.md#4-trabajo-en-paralelo): `CONTRIBUTING.md` (recomendado, lo leen personas y agentes) u otro archivo de instrucciones para agentes | Equipo | I0 | ADR-22 |
| P-12 | Alinear la versión de Ktor Server (3.2.0) con la del cliente (3.6.x) | Equipo | I0 | [01](01-stack.md) |

## Supuestos técnicos

Detalles que esta SPEC define sin haberlos tratado en el relevamiento. Si no se objetan en la
revisión, quedan confirmados al mergear:

| Supuesto | Dónde |
|---|---|
| Límite de 120 requests por minuto por cuenta y 10 por minuto por IP en `/auth/login` | [20](20-seguridad.md) |
| Hasta 50 pacientes con detalle guardado en el teléfono del pediatra | [14](14-sincronizacion.md#4-alcance-por-rol-adr-16) |
| Páginas de hasta 500 filas en la sincronización | [14](14-sincronizacion.md#2-traer-cambios) |
| Copias de seguridad: 14 diarias y 8 semanales | [30](30-infraestructura.md#copias-de-seguridad) |
| PRs de hasta unas 400 líneas como referencia | [31](31-calidad-y-ci.md#4-revisión-de-código) |
| Sin entorno de producción para la tesis: el piloto cumple ese rol | [30](30-infraestructura.md#entornos) |
