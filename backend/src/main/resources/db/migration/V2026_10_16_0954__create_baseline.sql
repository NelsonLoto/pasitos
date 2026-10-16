-- Línea base del esquema (ADR-03). Reemplaza a V1 y V2 (personas, usuarios, sesiones, …).
-- Solo crea lo que necesitan I0 e I1; el resto de las tablas de 13 · Base de datos las crea cada
-- incremento con su propia migración.
--
-- Convenciones (docs/spec-tecnica/13-base-de-datos.md):
--   - Nombres en inglés, snake_case, tablas en plural (ADR-21).
--   - Clave primaria id uuid; en las tablas sincronizables la genera la app.
--   - Fechas del calendario como date; momentos como timestamptz en UTC (ADR-26).
--   - Enums como text con CHECK.
--   - Tablas sincronizables (⟳): sync_version, asignada por trigger, y deleted_at (ADR-15).
--   - author_account_id no tiene FK: los registros de un pediatra quedan en el historial del niño
--     aunque su cuenta se elimine (RN-CTA-08).

-- ---------------------------------------------------------------------------------------------
-- Versión de sincronización
-- ---------------------------------------------------------------------------------------------

CREATE SEQUENCE sync_seq;

CREATE FUNCTION set_sync_version() RETURNS trigger AS $$
BEGIN
  NEW.sync_version := nextval('sync_seq');
  NEW.updated_at := now();
  RETURN NEW;
END $$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------------------------
-- Cuentas y acceso
-- ---------------------------------------------------------------------------------------------

CREATE TABLE accounts (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email              text NOT NULL,
  email_verified_at  timestamptz,
  first_name         text NOT NULL,
  last_name          text NOT NULL,
  password_hash      text,
  google_subject     text,
  is_guardian        boolean NOT NULL DEFAULT false,
  last_mode          text CHECK (last_mode IN ('GUARDIAN', 'PEDIATRICIAN')),
  failed_login_count integer NOT NULL DEFAULT 0,
  locked_until       timestamptz,
  created_at         timestamptz NOT NULL DEFAULT now(),
  updated_at         timestamptz,
  CONSTRAINT accounts_email_lowercase CHECK (email = lower(email)),
  CONSTRAINT accounts_has_credential CHECK (password_hash IS NOT NULL OR google_subject IS NOT NULL)
);
CREATE UNIQUE INDEX accounts_email_key ON accounts (email);
CREATE UNIQUE INDEX accounts_google_subject_key ON accounts (google_subject);

-- Un login activo por fila. Se guarda solo el hash SHA-256 del token de renovación (ADR-11).
CREATE TABLE sessions (
  id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id                  uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
  refresh_token_hash          text NOT NULL,
  previous_refresh_token_hash text,
  expires_at                  timestamptz NOT NULL,
  last_used_at                timestamptz NOT NULL DEFAULT now(),
  device_label                text,
  created_at                  timestamptz NOT NULL DEFAULT now(),
  updated_at                  timestamptz
);
CREATE UNIQUE INDEX sessions_refresh_token_hash_key ON sessions (refresh_token_hash);
CREATE INDEX sessions_account_id_idx ON sessions (account_id);

-- Tokens de un solo uso de los links de email (RN-CTA-06). Solo se guarda su hash.
CREATE TABLE email_tokens (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
  purpose    text NOT NULL CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD', 'CHANGE_EMAIL')),
  token_hash text NOT NULL,
  new_email  text,
  expires_at timestamptz NOT NULL,
  used_at    timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz,
  CONSTRAINT email_tokens_new_email_only_on_change CHECK ((purpose = 'CHANGE_EMAIL') = (new_email IS NOT NULL))
);
CREATE UNIQUE INDEX email_tokens_token_hash_key ON email_tokens (token_hash);
CREATE INDEX email_tokens_account_id_idx ON email_tokens (account_id);

-- ---------------------------------------------------------------------------------------------
-- Niños, tutores y vínculos
-- ---------------------------------------------------------------------------------------------

-- ⟳ El niño no se borra físicamente mientras haya teléfonos que deban enterarse: deleted_at.
CREATE TABLE children (
  id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  first_name                  text NOT NULL,
  last_name                   text NOT NULL,
  sex                         text NOT NULL CHECK (sex IN ('FEMALE', 'MALE')),
  birth_date                  date NOT NULL,
  gestational_weeks           integer CHECK (gestational_weeks BETWEEN 22 AND 42),
  -- "No lo sé" (RN-HIJ-03): sin semanas y con esta marca; equivale a 40 semanas.
  gestational_weeks_unknown   boolean NOT NULL DEFAULT false,
  birth_weight_kg             numeric(6, 3) CHECK (birth_weight_kg > 0),
  birth_length_cm             numeric(5, 1) CHECK (birth_length_cm > 0),
  birth_head_circumference_cm numeric(5, 1) CHECK (birth_head_circumference_cm > 0),
  allergies                   text CHECK (char_length(allergies) <= 280),
  relevant_history            text CHECK (char_length(relevant_history) <= 280),
  avatar_key                  text,
  status                      text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FINALIZED')),
  created_by_account_id       uuid REFERENCES accounts (id) ON DELETE SET NULL,
  sync_version                bigint NOT NULL,
  deleted_at                  timestamptz,
  created_at                  timestamptz NOT NULL DEFAULT now(),
  updated_at                  timestamptz,
  CONSTRAINT children_gestational_weeks_or_unknown CHECK (gestational_weeks_unknown = (gestational_weeks IS NULL))
);
CREATE TRIGGER children_sync_version BEFORE INSERT OR UPDATE ON children
  FOR EACH ROW EXECUTE FUNCTION set_sync_version();

-- Consentimientos (RN-CTA-02). Va después de children porque la declaración de representación
-- referencia al niño.
CREATE TABLE consents (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id  uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
  type        text NOT NULL CHECK (type IN ('TERMS', 'GUARDIAN_DECLARATION', 'CONFIDENTIALITY')),
  version     text NOT NULL,
  -- Solo en GUARDIAN_DECLARATION: el niño por el que se declaró la representación.
  child_id    uuid REFERENCES children (id) ON DELETE SET NULL,
  accepted_at timestamptz NOT NULL DEFAULT now(),
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz
);
CREATE INDEX consents_account_id_idx ON consents (account_id);

-- ⟳ Relación tutor–niño (RN-HIJ-06: exactamente un tutor principal por niño).
CREATE TABLE guardianships (
  child_id     uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
  account_id   uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
  role         text NOT NULL CHECK (role IN ('PRINCIPAL', 'SECONDARY')),
  sync_version bigint NOT NULL,
  deleted_at   timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz,
  PRIMARY KEY (child_id, account_id)
);
CREATE UNIQUE INDEX guardianships_one_principal_per_child ON guardianships (child_id)
  WHERE role = 'PRINCIPAL' AND deleted_at IS NULL;
CREATE INDEX guardianships_account_id_idx ON guardianships (account_id);
CREATE TRIGGER guardianships_sync_version BEFORE INSERT OR UPDATE ON guardianships
  FOR EACH ROW EXECUTE FUNCTION set_sync_version();

-- Invitaciones a tutores, a pediatras no registrados y a reclamar un perfil. Al vencer se borran
-- los datos personales del invitado (RN-PED-07), por eso email y nombre admiten nulos.
CREATE TABLE invitations (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  type               text NOT NULL CHECK (type IN ('GUARDIAN', 'PEDIATRICIAN', 'CLAIM')),
  child_id           uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
  inviter_account_id uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
  email              text CHECK (email = lower(email)),
  first_name         text,
  last_name          text,
  status             text NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED')),
  sent_at            timestamptz NOT NULL DEFAULT now(),
  expires_at         timestamptz NOT NULL,
  responded_at       timestamptz,
  created_at         timestamptz NOT NULL DEFAULT now(),
  updated_at         timestamptz,
  CONSTRAINT invitations_pending_has_email CHECK (status <> 'PENDING' OR email IS NOT NULL)
);
CREATE INDEX invitations_email_status_idx ON invitations (email, status);
CREATE INDEX invitations_child_id_idx ON invitations (child_id);

-- Avisa a la app que debe borrar los datos locales de un niño (ADR-15). Sin FK a children: el
-- aviso tiene que sobrevivir al borrado del niño.
CREATE TABLE access_revocations (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id   uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
  child_id     uuid NOT NULL,
  sync_version bigint NOT NULL,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz
);
CREATE INDEX access_revocations_account_id_sync_version_idx ON access_revocations (account_id, sync_version);
CREATE TRIGGER access_revocations_sync_version BEFORE INSERT OR UPDATE ON access_revocations
  FOR EACH ROW EXECUTE FUNCTION set_sync_version();

-- ---------------------------------------------------------------------------------------------
-- Crecimiento
-- ---------------------------------------------------------------------------------------------

-- ⟳ Solo los valores cargados: percentiles, puntaje Z y estado se calculan al leer (ADR-20).
CREATE TABLE measurements (
  id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  child_id              uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
  measured_on           date NOT NULL,
  weight_kg             numeric(6, 3) CHECK (weight_kg > 0),
  length_cm             numeric(5, 1) CHECK (length_cm > 0),
  length_position       text CHECK (length_position IN ('LYING', 'STANDING')),
  head_circumference_cm numeric(5, 1) CHECK (head_circumference_cm > 0),
  outlier_confirmed     boolean NOT NULL DEFAULT false,
  -- La FK a consultations se agrega en I4, cuando se crea esa tabla.
  consultation_id       uuid,
  author_account_id     uuid NOT NULL,
  author_role           text NOT NULL CHECK (author_role IN ('GUARDIAN', 'PEDIATRICIAN')),
  sync_version          bigint NOT NULL,
  deleted_at            timestamptz,
  created_at            timestamptz NOT NULL DEFAULT now(),
  updated_at            timestamptz,
  CONSTRAINT measurements_at_least_one_value
    CHECK (weight_kg IS NOT NULL OR length_cm IS NOT NULL OR head_circumference_cm IS NOT NULL),
  CONSTRAINT measurements_length_has_position CHECK (length_cm IS NULL OR length_position IS NOT NULL)
);
-- RN-CRE-12: cada autor carga como máximo una medición por niño y por fecha.
CREATE UNIQUE INDEX measurements_one_per_author_and_day ON measurements (child_id, author_account_id, measured_on)
  WHERE deleted_at IS NULL;
CREATE TRIGGER measurements_sync_version BEFORE INSERT OR UPDATE ON measurements
  FOR EACH ROW EXECUTE FUNCTION set_sync_version();

-- ---------------------------------------------------------------------------------------------
-- Vacunación
-- ---------------------------------------------------------------------------------------------

CREATE TABLE vaccines (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name       text NOT NULL,
  prevents   text,
  source     text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz
);
CREATE UNIQUE INDEX vaccines_name_key ON vaccines (name);

-- Versiones del Calendario Nacional (RN-VAC-03).
CREATE TABLE calendar_versions (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  effective_from date NOT NULL,
  status         text NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz
);

-- dose_code es estable entre versiones (por ejemplo PENTA-1): así se identifica una dosis aplicada.
CREATE TABLE calendar_doses (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version_id     uuid NOT NULL REFERENCES calendar_versions (id) ON DELETE CASCADE,
  vaccine_id     uuid NOT NULL REFERENCES vaccines (id) ON DELETE RESTRICT,
  dose_code      text NOT NULL,
  dose_label     text NOT NULL,
  age_days       integer NOT NULL CHECK (age_days >= 0),
  lead_days      integer NOT NULL DEFAULT 7 CHECK (lead_days >= 0),
  tolerance_days integer NOT NULL DEFAULT 30 CHECK (tolerance_days >= 0),
  sort_order     integer NOT NULL,
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz,
  CONSTRAINT calendar_doses_version_dose_code_key UNIQUE (version_id, dose_code)
);

-- ⟳ Se lee por sincronización pero se escribe por REST (RNF-OFF-06).
CREATE TABLE applied_doses (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  child_id          uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
  dose_code         text,
  -- La FK a campaigns se agrega en I6, cuando se crea esa tabla.
  campaign_id       uuid,
  -- Nulo en la carga retroactiva: "aplicada con fecha no registrada" (RN-VAC-04).
  applied_on        date,
  lot               text,
  place             text,
  consultation_id   uuid,
  author_account_id uuid NOT NULL,
  author_role       text NOT NULL CHECK (author_role IN ('GUARDIAN', 'PEDIATRICIAN')),
  sync_version      bigint NOT NULL,
  deleted_at        timestamptz,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz,
  CONSTRAINT applied_doses_dose_or_campaign CHECK ((dose_code IS NULL) <> (campaign_id IS NULL))
);
-- RN-VAC-09: cada dosis del calendario se registra una sola vez por niño.
CREATE UNIQUE INDEX applied_doses_one_per_dose ON applied_doses (child_id, dose_code)
  WHERE dose_code IS NOT NULL AND deleted_at IS NULL;
CREATE TRIGGER applied_doses_sync_version BEFORE INSERT OR UPDATE ON applied_doses
  FOR EACH ROW EXECUTE FUNCTION set_sync_version();

-- ⟳ Vacunas fuera del Calendario Nacional, sin estado (RN-VAC-06).
CREATE TABLE other_vaccines (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  child_id          uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
  name              text NOT NULL,
  applied_on        date NOT NULL,
  lot               text,
  note              text CHECK (char_length(note) <= 280),
  author_account_id uuid NOT NULL,
  author_role       text NOT NULL CHECK (author_role IN ('GUARDIAN', 'PEDIATRICIAN')),
  sync_version      bigint NOT NULL,
  deleted_at        timestamptz,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz
);
CREATE INDEX other_vaccines_child_id_idx ON other_vaccines (child_id);
CREATE TRIGGER other_vaccines_sync_version BEFORE INSERT OR UPDATE ON other_vaccines
  FOR EACH ROW EXECUTE FUNCTION set_sync_version();
