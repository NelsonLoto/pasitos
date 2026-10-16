# 13 · Base de datos

PostgreSQL 16. Implementa el [modelo conceptual](../spec/40-modelo-conceptual.md) y reemplaza al
esquema actual (`personas`, `usuarios`, `sesiones`, …) con una nueva línea base (ADR-03).

## Convenciones

| Tema | Regla |
|---|---|
| Nombres | Inglés, `snake_case`, tablas en plural |
| Claves primarias | `id uuid`. En las tablas sincronizables lo genera la app; en las demás, el backend |
| Auditoría de fila | `created_at timestamptz not null default now()` y `updated_at timestamptz` |
| Fechas | `date` para fechas del calendario (nacimiento, medición, aplicación); `timestamptz` en UTC para momentos (ADR-26) |
| Números | `numeric(6,3)` para kg, `numeric(5,1)` para cm |
| Enums | `text` con `CHECK` (más fácil de migrar que los enums de PostgreSQL) |
| Autoría | Tablas con registros cargados por usuarios: `author_account_id uuid not null`, `author_role text check (author_role in ('GUARDIAN','PEDIATRICIAN'))`. `author_account_id` no tiene FK a `accounts`: lo que cargó un pediatra queda en el historial del niño aunque su cuenta se elimine (RN-CTA-08) |
| Sincronización | Tablas sincronizables: `sync_version bigint not null` y `deleted_at timestamptz` (ADR-15) |
| Borrado | Las tablas sincronizables usan borrado lógico (`deleted_at`) para propagar el borrado. El resto, borrado físico con `ON DELETE` explícito |
| Datos derivados | No se guardan percentiles, estados ni eventos automáticos (ADR-20, ADR-24) |

### Versión de sincronización

Una secuencia global y un trigger asignan una versión creciente en cada inserción o
modificación. El cursor de la app es la última versión que recibió:

```sql
CREATE SEQUENCE sync_seq;

CREATE FUNCTION set_sync_version() RETURNS trigger AS $$
BEGIN
  NEW.sync_version := nextval('sync_seq');
  NEW.updated_at := now();
  RETURN NEW;
END $$ LANGUAGE plpgsql;
-- CREATE TRIGGER … BEFORE INSERT OR UPDATE ON <tabla sincronizable> FOR EACH ROW EXECUTE FUNCTION set_sync_version();
```

## Tablas

### Cuentas y acceso

| Tabla | Columnas principales | Restricciones e índices |
|---|---|---|
| `accounts` | `email`, `email_verified_at`, `first_name`, `last_name`, `password_hash` (nulo si solo Google), `google_subject`, `is_guardian`, `last_mode`, `failed_login_count`, `locked_until` | `email` único (en minúsculas); `google_subject` único |
| `consents` | `account_id`, `type` (`TERMS`, `GUARDIAN_DECLARATION`, `CONFIDENTIALITY`), `version`, `child_id`, `accepted_at` | FK `account_id` con `ON DELETE CASCADE` |
| `sessions` | `account_id`, `refresh_token_hash`, `previous_refresh_token_hash`, `expires_at`, `last_used_at`, `device_label` | `refresh_token_hash` único; índice por `account_id` |
| `email_tokens` | `account_id`, `purpose` (`VERIFY_EMAIL`, `RESET_PASSWORD`, `CHANGE_EMAIL`), `token_hash`, `new_email`, `expires_at`, `used_at` | `token_hash` único |
| `devices` | `account_id`, `fcm_token`, `updated_at` | `fcm_token` único |
| `notification_preferences` | `account_id`, `type`, `enabled` | PK (`account_id`, `type`) |
| `pediatrician_profiles` | `account_id` (PK), `specialty`, `license_jurisdiction`, `license_number`, `status` (`PENDING`, `VALIDATED`, `REJECTED`, `REVALIDATION`, `SUSPENDED`), `status_reason`, `reviewed_by_admin_id`, `reviewed_at` | Único (`license_jurisdiction`, `license_number`) (RN-PED-03) |

### Niños, tutores y vínculos

| Tabla | Columnas principales | Restricciones e índices |
|---|---|---|
| `children` ⟳ | `first_name`, `last_name`, `sex`, `birth_date`, `gestational_weeks`, `gestational_weeks_unknown`, `birth_weight_kg`, `birth_length_cm`, `birth_head_circumference_cm`, `allergies` (≤280), `relevant_history` (≤280), `avatar_key`, `status` (`ACTIVE`, `FINALIZED`), `created_by_account_id` | `CHECK gestational_weeks BETWEEN 22 AND 42` |
| `guardianships` ⟳ | `child_id`, `account_id`, `role` (`PRINCIPAL`, `SECONDARY`), `created_at` | PK (`child_id`, `account_id`); índice único parcial: un `PRINCIPAL` por niño |
| `invitations` | `type` (`GUARDIAN`, `PEDIATRICIAN`, `CLAIM`), `child_id`, `inviter_account_id`, `email`, `first_name`, `last_name`, `status` (`PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CANCELLED`), `sent_at`, `expires_at`, `responded_at` | Índice por `email` y `status`; datos personales borrados al vencer (RN-PED-07) |
| `care_links` ⟳ | `child_id`, `pediatrician_account_id` (nulo mientras espera registro), `invitation_id`, `status` (`REQUESTED`, `AWAITING_REGISTRATION`, `ACTIVE`, `REJECTED`, `CANCELLED`, `EXPIRED`, `ENDED`), `requested_by_account_id`, `requested_at`, `started_at`, `ended_at`, `end_reason`, `rejection_reason` | Únicos parciales: un `ACTIVE` por niño y uno en (`REQUESTED`, `AWAITING_REGISTRATION`) por niño (RN-PED-01) |
| `access_revocations` | `account_id`, `child_id`, `sync_version` | Avisa a la app que debe borrar datos locales del niño (ADR-15) |

### Crecimiento

| Tabla | Columnas principales | Restricciones e índices |
|---|---|---|
| `measurements` ⟳ | `child_id`, `measured_on`, `weight_kg`, `length_cm`, `length_position`, `head_circumference_cm`, `outlier_confirmed`, `consultation_id`, autoría | Único parcial (`child_id`, `author_account_id`, `measured_on`) sin borrar (RN-CRE-12); `CHECK` de al menos un valor |
| `reference_table_versions` | `version`, `status` (`DRAFT`, `PUBLISHED`, `RETIRED`), `published_at`, `validated_by`, `notes` | Una sola `PUBLISHED` |
| `reference_tables` | `version_id`, `source` (`WHO_2006`, `SAP`), `indicator`, `sex`, `axis`, `restricted_tails` | Único (`version_id`, `source`, `indicator`, `sex`) |
| `reference_rows` | `table_id`, `x`, `l`, `m`, `s`, `percentiles jsonb` | PK (`table_id`, `x`) |
| `threshold_versions` | Igual que `reference_table_versions` | |
| `thresholds` | `version_id`, `indicator`, `source`, `age_from_days`, `age_to_days`, `level`, `z_min`, `z_max`, `technical_label`, `guardian_text` | |
| `growth_settings` | `version_id`, `crossing_lines jsonb`, `crossing_min`, `plausibility jsonb`, `stale_after_days` | |

### Vacunación

| Tabla | Columnas principales | Restricciones e índices |
|---|---|---|
| `vaccines` | `name`, `prevents`, `source` | |
| `calendar_versions` | `effective_from`, `status` | |
| `calendar_doses` | `version_id`, `vaccine_id`, `dose_code`, `dose_label`, `age_days`, `lead_days`, `tolerance_days`, `sort_order` | Único (`version_id`, `dose_code`) |
| `campaigns` | `vaccine_id`, `name`, `age_from_days`, `age_to_days`, `starts_on`, `ends_on` | |
| `applied_doses` ⟳ | `child_id`, `dose_code` o `campaign_id`, `applied_on` (nulo en carga retroactiva), `lot`, `place`, `consultation_id`, autoría | Único parcial (`child_id`, `dose_code`) sin borrar (RN-VAC-09) |
| `other_vaccines` ⟳ | `child_id`, `name`, `applied_on`, `lot`, `note`, autoría | |

`applied_doses` y `other_vaccines` son sincronizables para lectura, pero se escriben por REST
porque requieren conexión (RNF-OFF-06).

### Consultas

| Tabla | Columnas principales | Restricciones e índices |
|---|---|---|
| `consultations` ⟳ | `child_id` (nulo si el niño se eliminó), `child_first_name`, `child_last_name`, `child_birth_date` (copia para RN-CTA-09), `pediatrician_account_id`, `status` (`DRAFT`, `CLOSED`), `consulted_on`, `type`, `companion`, `steps jsonb`, `family_instructions`, `private_notes`, `next_control_on`, `current_step`, `closed_at` | Único parcial: un `DRAFT` por niño y pediatra; `child_id` con `ON DELETE SET NULL` |
| `consultation_addenda` | `consultation_id`, `text`, `visibility` (`PRIVATE`, `FAMILY`), `created_at` | |

`steps` guarda los pasos 3 a 8 con un esquema JSON versionado (signos vitales, alimentación,
desarrollo, pesquisas, examen físico). Las mediciones, dosis e hitos del paso correspondiente se
guardan en sus tablas con `consultation_id` y se hacen visibles al cerrar (RN-CON-03).
`private_notes` nunca se incluye en lo que se sincroniza a dispositivos que no son del autor.

### Seguimiento

| Tabla | Columnas principales | Restricciones e índices |
|---|---|---|
| `events` ⟳ | `child_id`, `type` (`DENTITION`, `HEALTH`, `MOMENT`), `subtype`, `occurred_on`, `title` (≤60), `note` (≤280), `icon_key`, autoría | |
| `milestones` | `description`, `area`, `expected_age_days`, `limit_age_days`, `source`, `active` | |
| `milestone_achievements` ⟳ | `child_id`, `milestone_id`, `achieved_on`, `consultation_id`, autoría | Único parcial (`child_id`, `milestone_id`) sin borrar |
| `habits` ⟳ | `child_id`, `type` (`BREASTFEEDING`, `BOTTLE`, `DIAPER`, `SLEEP`), `occurred_at`, `side`, `duration_range`, `amount_ml`, `milk_type`, `diaper_type`, `sleep_kind`, `sleep_range`, autoría | `CHECK` de columnas obligatorias por tipo |
| `advice` | `title`, `summary`, `body`, `category`, `age_from_months`, `age_to_months`, `anticipatory`, `source_name`, `source_url`, `status`, `published_at` | |
| `control_schedule` | `version`, `age_days`, `label` | |

### Notificaciones, auditoría y administración

| Tabla | Columnas principales | Restricciones e índices |
|---|---|---|
| `notifications` | `account_id`, `type` (`N01`…`N13`), `child_id`, `title`, `body`, `deep_link`, `dedup_key`, `created_at`, `read_at`, `push_sent_at` | Único (`account_id`, `dedup_key`) para no duplicar (RF-NOT-03, CA1) |
| `audit_log` | `id bigserial`, `account_id`, `action` (`VIEW`, `CREATE`, `UPDATE`, `DELETE`, `EXPORT`), `entity_type`, `entity_id`, `child_id`, `occurred_at` | Solo inserción; índices por `child_id` y `account_id` |
| `admins` | `email`, `name`, `password_hash`, `last_login_at` | |
| `admin_audit` | `admin_id`, `entity_type`, `entity_id`, `before jsonb`, `after jsonb`, `validated_by`, `occurred_at` | RN-ADM-02 |
| `scheduled_tasks` | La define db-scheduler | |

⟳ = tabla sincronizable (tiene `sync_version` y `deleted_at`).

## Migraciones

- Flyway, archivos en `backend/src/main/resources/db/migration`.
- **Nueva línea base en I0**: se eliminan `V1__esquema_inicial.sql` y
  `V2__separar_usuarios_de_personas.sql` y se crea el esquema de este documento. Cada integrante
  borra su base local una vez (`docker compose down -v`). Es posible porque no hay datos reales.
- **Versionado por fecha y hora** para evitar choques entre ramas en paralelo (ADR-23):
  `V2026_10_20_1530__create_children.sql`. Flyway se configura con `outOfOrder = true`.
- Una migración ya mergeada **nunca se edita**: se corrige con una nueva.
- Una columna que apunta a una tabla de un incremento posterior se crea sin FK, y la FK se agrega
  en la migración que crea esa tabla. En la línea base: `consultation_id` en `measurements` y
  `applied_doses` (I4) y `campaign_id` en `applied_doses` (I6).
- Las migraciones son aditivas cuando sea posible. Si se elimina una columna, en el mismo PR se
  actualiza el objeto `Table` de Exposed (lección del proyecto: `selectAll()` falla si la tabla
  declara columnas que ya no existen).

## Datos iniciales

| Datos | Cómo se cargan |
|---|---|
| Tablas OMS, transcripción SAP, umbrales iniciales | Archivos CSV en `backend/seed/`. En producción, por importación desde el panel (RF-ADM-04). En `local` y `ci`, con la tarea `./gradlew seedReferenceData` |
| Calendario Nacional, cronograma de controles, hitos, consejos iniciales | Mismo mecanismo, con archivos en `backend/seed/` |
| Primer administrador | Variable `ADMIN_BOOTSTRAP_EMAIL` al arrancar |
