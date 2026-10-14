# 40 · Trazabilidad

Qué componentes implementan cada módulo de la SPEC funcional. El detalle por requerimiento queda
en los issues del plan (cada issue lista sus RF) y en los nombres de las pruebas (cada prueba
nombra su criterio, [31](31-calidad-y-ci.md#nombres-de-pruebas-trazables)).

| Módulo funcional | Backend (`com.pasitos.backend.…`) | Endpoints | Tablas | App (`com.pasitos.app.…`) | `shared` | Incremento |
|---|---|---|---|---|---|---|
| [10 · Cuentas](../spec/10-cuentas-y-acceso.md) | `auth`, `account`, `web` | `/auth/*`, `/me*`, `/web/*` | `accounts`, `consents`, `sessions`, `email_tokens` | `data/auth`, `data/account`, `feature/login`, `feature/register`, `feature/profile`, `core/security` | `api` | I0, I1, I6 |
| [11 · Hijos y tutores](../spec/11-hijos-y-tutores.md) | `child`, `guardianship` | `/children*`, `/invitations*` | `children`, `guardianships`, `invitations` | `data/child`, `feature/childonboarding`, `feature/childselector`, `feature/guardians` | `time`, `api` | I1, I6 |
| [12 · Crecimiento](../spec/12-crecimiento.md) | `measurement`, `growth` | `/sync/*`, `/reference-data*`, `/children/{id}/growth` | `measurements`, `reference_*`, `thresholds`, `growth_settings` | `data/measurement`, `data/growth`, `feature/measurement`, `feature/measurementhistory`, `feature/growthchart`, `feature/home` | `growth` | I1, I2 |
| [13 · Vacunación](../spec/13-vacunacion.md) | `vaccination` | `/vaccination-calendar`, `/children/{id}/applied-doses*`, `/other-vaccines*` | `vaccines`, `calendar_*`, `campaigns`, `applied_doses`, `other_vaccines` | `data/vaccination`, `feature/vaccination`, `feature/vaccinedetail` | `vaccination` | I1, I6 |
| [14 · Pediatras y vínculos](../spec/14-pediatras-y-vinculos.md) | `pediatrician`, `carelink` | `/me/pediatrician-profile`, `/pediatricians/search`, `/link-requests*`, `/children/{id}/care-link*`, `/patients*`, `/invitations/{id}/claim` | `pediatrician_profiles`, `care_links`, `invitations`, `access_revocations` | `data/pediatrician`, `data/carelink`, `feature/pediatrician*`, `feature/patients`, `feature/linkrequests` | `api` | I3 |
| [15 · Consulta guiada](../spec/15-consulta-guiada.md) | `consultation` | `/consultations*`, `/children/{id}/consultations` | `consultations`, `consultation_addenda` | `data/consultation`, `feature/consultation`, `feature/consultationdetail` | `growth`, `api` | I4 |
| [16 · Eventos](../spec/16-eventos-y-linea-de-tiempo.md) | `event`, `milestone` | `/sync/*`, `/milestones` | `events`, `milestones`, `milestone_achievements` | `data/event`, `data/milestone`, `feature/timeline`, `feature/development` | `time` | I5 |
| [17 · Hábitos](../spec/17-habitos.md) | `habit` | `/sync/*`, `/children/{id}/habits/summary` | `habits` | `data/habit`, `feature/habits` | — | I5 |
| [18 · Consejos](../spec/18-consejos.md) | `advice` | `/advice` | `advice` | `data/advice`, `feature/advicelist`, `feature/advicedetail` | — | I5 |
| [19 · Informes](../spec/19-informes.md) | `report` | `/children/{id}/growth-report`, `/children/{id}/consultation-record` | (lectura) + `audit_log` | `data/report`, `feature/reportexport` | `growth` | I6 |
| [20 · Notificaciones](../spec/20-notificaciones.md) | `notification`, `jobs` | `/notifications/*`, `/me/notifications*`, `/me/devices/*`, `/me/notification-preferences` | `notifications`, `devices`, `notification_preferences`, `control_schedule` | `data/notification`, `feature/notificationcenter`, `core/messaging` | `time` | I6 |
| [21 · Administración](../spec/21-administracion.md) | `admin`, `audit` | `/admin/*` (HTML) | `admins`, `admin_audit`, `audit_log` y catálogos | — | `growth` (validación de importación) | I2, I3, I6 |
| [30 · RNF sin conexión](../spec/30-requerimientos-no-funcionales.md#funcionamiento-sin-conexión-off--incremento-i5) | `sync` | `/sync/*` | `sync_version`, `deleted_at`, `access_revocations` | `core/sync`, `core/database` | — | I0, I1, I5 |
| [30 · RNF seguridad y privacidad](../spec/30-requerimientos-no-funcionales.md#seguridad-seg) | `auth`, `audit`, `common` | Todos | `sessions`, `audit_log`, `consents` | `core/security` | — | I0 en adelante |

Las carpetas `feature/` que no existen todavía (por ejemplo, `feature/patients`) se crean en el
incremento indicado.
