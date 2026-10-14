# Pasitos — Especificación técnica

| | |
|---|---|
| **Versión** | 1.1.0 |
| **Fecha** | 14/10/2026 |
| **Estado** | Borrador para revisión del equipo |
| **Implementa** | [SPEC funcional v1.0.0](../spec/README.md) |

## Propósito

Define **cómo** se construye lo que la [SPEC funcional](../spec/README.md) define como **qué**:
arquitectura, tecnologías, modelo de datos, API, sincronización, seguridad, infraestructura,
pruebas y proceso de trabajo. Es la referencia para el plan de implementación y para cada PR.

Cuando esta SPEC y la funcional parecen contradecirse, manda la funcional: se corrige esta.

## Estructura

| Archivo | Contenido |
|---|---|
| [00 · Arquitectura](00-arquitectura.md) | Vista general, componentes, principios técnicos y fundaciones (I0) |
| [01 · Stack](01-stack.md) | Tecnologías y versiones por componente |
| [02 · Estructura del repositorio](02-estructura-del-repo.md) | Carpetas, builds de Gradle, nombres e idioma del código |
| [10 · Módulo compartido](10-modulo-compartido.md) | Motor de crecimiento, estados de vacunas y contratos de la API |
| [11 · Backend](11-backend.md) | Capas, autenticación, autorización, tareas programadas, configuración |
| [12 · API](12-api.md) | Convenciones y catálogo de endpoints |
| [13 · Base de datos](13-base-de-datos.md) | Esquema físico, convenciones y migraciones |
| [14 · Sincronización](14-sincronizacion.md) | Protocolo entre la app y el backend, funcionamiento sin conexión |
| [15 · App Android](15-android.md) | Capas, base local cifrada, sesión, modos, notificaciones |
| [16 · Panel de administración](16-panel-admin.md) | Panel web y páginas públicas del backend |
| [17 · Notificaciones y emails](17-notificaciones-y-emails.md) | Push sin datos, generación, emails transaccionales |
| [18 · Informes PDF](18-informes-pdf.md) | Generación de informes en el servidor |
| [20 · Seguridad y privacidad](20-seguridad.md) | Medidas técnicas para cada RNF de seguridad y privacidad |
| [30 · Infraestructura](30-infraestructura.md) | Entornos, despliegue, copias de seguridad, publicación de la app |
| [31 · Calidad e integración continua](31-calidad-y-ci.md) | Estrategia de pruebas, CI y definición de terminado |
| [32 · Proceso de desarrollo](32-proceso-de-desarrollo.md) | Flujo SDD, tareas, trabajo en paralelo y agentes de IA |
| [40 · Trazabilidad](40-trazabilidad.md) | Qué componentes implementan cada módulo funcional |
| [50 · Decisiones de arquitectura](50-adr.md) | Registro de decisiones (ADR) |
| [92 · Pendientes técnicos](92-pendientes.md) | Decisiones abiertas y tareas previas |

## Convenciones

- Los identificadores de la SPEC funcional (`RF-CRE-05`, `RN-PED-04`, `RNF-SEG-06`) se citan tal
  cual para trazar cada decisión técnica a su requerimiento.
- Las decisiones de arquitectura se identifican como `ADR-NN` y están en
  [50 · Decisiones de arquitectura](50-adr.md).
- Los nombres en el código, la API y la base de datos están en **inglés**; los textos de la app y
  la documentación, en español (ADR-21). La tabla de equivalencias está en
  [02 · Estructura del repositorio](02-estructura-del-repo.md#equivalencias-de-nombres).
- Los ejemplos de código son ilustrativos: fijan contratos y responsabilidades, no la
  implementación exacta.

## Cómo se modifica

Igual que la SPEC funcional: rama, PR, versión semántica y fila en el registro de cambios. Un
cambio técnico que altere comportamiento visible requiere primero el cambio en la SPEC funcional.

## Registro de cambios

| Versión | Fecha | Cambio | Motivo |
|---|---|---|---|
| 1.1.0 | 14/10/2026 | Base de Room recreada al cambiar de versión hasta el piloto; P-04 (email real) pasa a antes de I6 | Ajustes surgidos al armar el plan de implementación |
| 1.0.0 | 14/10/2026 | Versión inicial | Relevamiento técnico en 2 rondas (14/10/2026) sobre la SPEC funcional v1.0.0 |
