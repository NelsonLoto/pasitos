# Pasitos — Especificación funcional (SPEC)

| | |
|---|---|
| **Versión** | 1.2.0 |
| **Fecha** | 14/10/2026 |
| **Estado** | Borrador para revisión del equipo |
| **Reemplaza a** | `docs/Documento_Funcional_App_Nino_Sano.docx` (v1.0, 17/08/2026) como documento de referencia funcional |

## Propósito

Esta SPEC es la **única fuente de verdad funcional** de Pasitos. El proyecto se desarrolla con
*Spec-Driven Development*: todo cambio de comportamiento se refleja primero en esta SPEC y recién
después en el diseño técnico, el código y las pruebas. Lo ya construido sobre el documento v1.0 se
conserva; lo que no coincida con esta SPEC se ajusta en el incremento que corresponda.

Pasitos es una aplicación móvil para el seguimiento del crecimiento y la salud de niños y niñas de
0 a 15 años en Argentina, orientada a tutores (madres, padres, representantes legales) y pediatras.
Es el producto de una tesis: el entregable final debe cubrir todo el alcance aquí descripto.

## Estructura

| Archivo | Contenido |
|---|---|
| [00 · Visión y alcance](00-vision-y-alcance.md) | Contexto, objetivo, principios, alcance, fuera de alcance, incrementos, supuestos, restricciones y riesgos |
| [01 · Actores y permisos](01-actores-y-permisos.md) | Actores, perfiles de cuenta y matriz de permisos |
| [10 · Cuentas y acceso](10-cuentas-y-acceso.md) | Registro, inicio de sesión, sesión, bloqueo, eliminación de cuenta |
| [11 · Hijos y tutores](11-hijos-y-tutores.md) | Perfil del niño, datos de nacimiento, varios tutores, perfil finalizado |
| [12 · Crecimiento](12-crecimiento.md) | Mediciones, percentiles, puntaje Z, IMC, curvas, estado del niño |
| [13 · Vacunación](13-vacunacion.md) | Calendario Nacional, dosis, estados, campañas, otras vacunas |
| [14 · Pediatras y vínculos](14-pediatras-y-vinculos.md) | Perfil profesional, validación, vínculo tutor–pediatra, pacientes propios |
| [15 · Consulta guiada](15-consulta-guiada.md) | Paso a paso del control y registro de controles del pediatra |
| [16 · Eventos y línea de tiempo](16-eventos-y-linea-de-tiempo.md) | Eventos automáticos y manuales, hitos del desarrollo |
| [17 · Hábitos](17-habitos.md) | Tomas, pañales y sueño (0 a 24 meses) |
| [18 · Consejos](18-consejos.md) | Consejos por edad y "¿Qué se viene?" |
| [19 · Informes](19-informes.md) | Informe de crecimiento en PDF y exportación del registro de controles |
| [20 · Notificaciones](20-notificaciones.md) | Tipos, disparadores y preferencias |
| [21 · Administración](21-administracion.md) | Panel web: validación de pediatras y parámetros clínicos y de contenido |
| [30 · Requerimientos no funcionales](30-requerimientos-no-funcionales.md) | Seguridad, privacidad, sin conexión, rendimiento, accesibilidad, compatibilidad |
| [40 · Modelo conceptual](40-modelo-conceptual.md) | Entidades del dominio y sus relaciones |
| [90 · Glosario](90-glosario.md) | Términos del dominio |
| [91 · Registro de decisiones](91-decisiones.md) | Decisiones tomadas durante el relevamiento y su justificación |
| [92 · Pendientes](92-pendientes.md) | Pendientes de validación médica y supuestos a confirmar |

## Convenciones

### Identificadores

| Prefijo | Significado | Ejemplo |
|---|---|---|
| `RF-<MÓD>-NN` | Requerimiento funcional | `RF-CRE-05` |
| `RN-<MÓD>-NN` | Regla de negocio | `RN-VAC-01` |
| `RNF-<CAT>-NN` | Requerimiento no funcional | `RNF-SEG-03` |
| `PVM-NN` | Pendiente de validación médica | `PVM-04` |
| `SUP-NN` | Supuesto a confirmar | `SUP-02` |
| `D-NN` | Decisión del relevamiento | `D-17` |

Módulos: `CTA` cuentas · `HIJ` hijos · `CRE` crecimiento · `VAC` vacunación · `PED` pediatras ·
`CON` consulta · `EVT` eventos · `HAB` hábitos · `CJO` consejos · `INF` informes ·
`NOT` notificaciones · `ADM` administración.

Los identificadores **no se reutilizan**: si un requerimiento se elimina, su ID queda dado de baja
en el registro de cambios.

### Redacción

- **Debe**: obligatorio. **Puede**: permitido u opcional.
- Cada requerimiento funcional indica actores, incremento (`I1`–`I6`; `I0` es técnico y no tiene RF, ver
  [00 · Visión y alcance](00-vision-y-alcance.md#6-plan-de-incrementos)) y si funciona sin conexión.
- Los criterios de aceptación se escriben como *Dado / Cuando / Entonces* y son la base de las
  pruebas de aceptación.
- ⚕️ **PVM** marca una regla clínica pendiente de validación médica. Puede implementarse con los
  valores iniciales indicados, pero no se considera cerrada hasta su validación (ver
  [92 · Pendientes](92-pendientes.md)).
- Los textos de ejemplo de la interfaz están en español de Argentina, con voseo.

## Cómo se modifica esta SPEC

1. Todo cambio se hace en una rama y por Pull Request, igual que el código
   (ver [`CONTRIBUTING.md`](../../CONTRIBUTING.md)).
2. Se actualiza la versión según el tipo de cambio:
   - **Mayor** (`2.0.0`): cambio de alcance (se agrega o quita un módulo, cambia un actor).
   - **Menor** (`1.1.0`): se agrega, modifica o da de baja un requerimiento o regla.
   - **Parche** (`1.0.1`): correcciones de redacción sin cambio de comportamiento.
3. Se agrega una fila al registro de cambios con el motivo (por ejemplo, "validación con pediatras
   del 28/10").
4. Si el cambio resuelve un `PVM` o un `SUP`, se marca como resuelto en
   [92 · Pendientes](92-pendientes.md) indicando quién lo validó.

## Registro de cambios

| Versión | Fecha | Cambio | Motivo |
|---|---|---|---|
| 1.2.0 | 14/10/2026 | El piloto se hace con datos ficticios: cambian RNF-PRI-08, R-04, SUP-14 y SUP-22 | Decisión del stakeholder (D-65) |
| 1.1.0 | 14/10/2026 | Se agrega el incremento I0 de fundaciones técnicas antes de I1; los RNF aplican desde I0 | Decisión del relevamiento técnico (ADR-25 de la SPEC técnica) |
| 1.0.0 | 14/10/2026 | Versión inicial | Relevamiento funcional en 4 rondas (13 y 14/10/2026). Amplía el alcance del documento v1.0: pediatra como usuario, rango 0–15 años, consulta guiada, línea de tiempo, estado del niño, administración |
