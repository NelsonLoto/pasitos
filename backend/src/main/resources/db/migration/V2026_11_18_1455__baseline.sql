-- Línea base del esquema (I0-11): 13 · Base de datos, ADR-03, ADR-23 y ADR-26.
-- Crea solo lo que usan I0 e I1; el resto de las tablas las crea cada incremento.
-- Una migración ya mergeada no se edita: se corrige con una nueva.
--
-- Reglas de esta línea base:
--   * CHECK solo donde 13 define los valores o una regla de negocio los implica. Los valores y
--     largos que nadie definió todavía los agrega la tarea que escribe esa columna.
--   * Emails guardados en minúsculas y sin espacios en los extremos.
--   * Tokens: solo su hash SHA-256, en bytea de 32 bytes. Contraseñas: solo su hash bcrypt.
--   * ON DELETE: CASCADE para lo que es de una cuenta o de un niño y no se sincroniza;
--     RESTRICT en las tablas sincronizables y en los catálogos, que se borran con lápida o desde
--     su servicio; sin FK para la autoría y las revocaciones, que tienen que seguir existiendo.
--   * Índice en cada clave foránea.
--   * ⟳ = tabla sincronizable: el id lo genera la app, y tiene sync_version y deleted_at (ADR-15).

-- Sincronización (13, ADR-15) ------------------------------------------------------------------
-- Los triggers de cada tabla sincronizable los agrega I0-08.

CREATE SEQUENCE sync_seq;

CREATE FUNCTION set_sync_version() RETURNS trigger AS $$
BEGIN
    NEW.sync_version := nextval('sync_seq');
    NEW.updated_at := now();
    RETURN NEW;
END $$ LANGUAGE plpgsql;

-- Cuentas y acceso ------------------------------------------------------------------------------

CREATE TABLE accounts (
    id                 uuid PRIMARY KEY,
    email              text NOT NULL,
    email_verified_at  timestamptz,
    first_name         text NOT NULL,
    last_name          text NOT NULL,
    password_hash      text,  -- nulo si la cuenta solo ingresa con Google
    google_subject     text,
    is_guardian        boolean NOT NULL,
    last_mode          text,
    failed_login_count integer NOT NULL DEFAULT 0,
    locked_until       timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz,
    CONSTRAINT accounts_email_key UNIQUE (email),  -- RN-CTA-03
    CONSTRAINT accounts_google_subject_key UNIQUE (google_subject),
    CONSTRAINT accounts_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT accounts_password_hash_bcrypt
        CHECK (password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'),
    CONSTRAINT accounts_last_mode_check CHECK (last_mode IN ('GUARDIAN', 'PEDIATRICIAN')),
    CONSTRAINT accounts_failed_login_count_check CHECK (failed_login_count >= 0)
);

CREATE TABLE sessions (
    id                          uuid PRIMARY KEY,
    account_id                  uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    refresh_token_hash          bytea NOT NULL,
    previous_refresh_token_hash bytea,
    expires_at                  timestamptz NOT NULL,
    last_used_at                timestamptz NOT NULL,
    device_label                text,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz,
    CONSTRAINT sessions_refresh_token_hash_key UNIQUE (refresh_token_hash),
    CONSTRAINT sessions_refresh_token_hash_sha256 CHECK (octet_length(refresh_token_hash) = 32),
    CONSTRAINT sessions_previous_refresh_token_hash_sha256
        CHECK (octet_length(previous_refresh_token_hash) = 32)
);
CREATE INDEX sessions_account_id_idx ON sessions (account_id);
-- La detección de reutilización busca por el token anterior (11 · Backend §3).
CREATE INDEX sessions_previous_refresh_token_hash_idx ON sessions (previous_refresh_token_hash);

CREATE TABLE email_tokens (
    id         uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    purpose    text NOT NULL,
    token_hash bytea NOT NULL,
    new_email  text,  -- solo para CHANGE_EMAIL
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz,
    CONSTRAINT email_tokens_token_hash_key UNIQUE (token_hash),
    CONSTRAINT email_tokens_token_hash_sha256 CHECK (octet_length(token_hash) = 32),
    CONSTRAINT email_tokens_purpose_check
        CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD', 'CHANGE_EMAIL')),
    CONSTRAINT email_tokens_new_email_normalized CHECK (new_email = lower(btrim(new_email)))
);
CREATE INDEX email_tokens_account_id_idx ON email_tokens (account_id);

-- Niños, tutores y vínculos ---------------------------------------------------------------------

CREATE TABLE children (  -- ⟳
    id                          uuid PRIMARY KEY,
    first_name                  text NOT NULL,
    last_name                   text NOT NULL,
    sex                         text NOT NULL,
    birth_date                  date NOT NULL,
    gestational_weeks           integer NOT NULL,
    gestational_weeks_unknown   boolean NOT NULL DEFAULT false,
    birth_weight_kg             numeric(6, 3),
    birth_length_cm             numeric(5, 1),
    birth_head_circumference_cm numeric(5, 1),
    allergies                   text,
    relevant_history            text,
    avatar_key                  text,
    status                      text NOT NULL DEFAULT 'ACTIVE',
    created_by_account_id       uuid NOT NULL,  -- sin FK: es autoría (RN-CTA-08)
    sync_version                bigint NOT NULL,
    deleted_at                  timestamptz,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz,
    CONSTRAINT children_sex_check CHECK (sex IN ('FEMALE', 'MALE')),  -- RN-HIJ-02
    CONSTRAINT children_gestational_weeks_check CHECK (gestational_weeks BETWEEN 22 AND 42),
    -- "No lo sé" equivale a 40 semanas (RN-HIJ-03).
    CONSTRAINT children_gestational_weeks_unknown_check
        CHECK (NOT gestational_weeks_unknown OR gestational_weeks = 40),
    CONSTRAINT children_allergies_length CHECK (char_length(allergies) <= 280),  -- RN-HIJ-10
    CONSTRAINT children_relevant_history_length CHECK (char_length(relevant_history) <= 280),
    CONSTRAINT children_status_check CHECK (status IN ('ACTIVE', 'FINALIZED'))
);

-- Va después de children por su child_id (declaración de representación de cada hijo).
CREATE TABLE consents (
    id          uuid PRIMARY KEY,
    account_id  uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    type        text NOT NULL,
    version     text NOT NULL,  -- versión del texto aceptado (RN-CTA-02)
    child_id    uuid REFERENCES children (id) ON DELETE CASCADE,
    accepted_at timestamptz NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz,
    CONSTRAINT consents_type_check
        CHECK (type IN ('TERMS', 'GUARDIAN_DECLARATION', 'CONFIDENTIALITY'))
);
CREATE INDEX consents_account_id_idx ON consents (account_id);
CREATE INDEX consents_child_id_idx ON consents (child_id);

CREATE TABLE guardianships (  -- ⟳
    child_id     uuid NOT NULL REFERENCES children (id) ON DELETE RESTRICT,
    account_id   uuid NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    role         text NOT NULL,
    sync_version bigint NOT NULL,
    deleted_at   timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz,
    PRIMARY KEY (child_id, account_id),
    CONSTRAINT guardianships_role_check CHECK (role IN ('PRINCIPAL', 'SECONDARY'))
);
-- Un solo tutor principal vigente por niño (RN-HIJ-06).
CREATE UNIQUE INDEX guardianships_one_principal_idx ON guardianships (child_id)
    WHERE role = 'PRINCIPAL' AND deleted_at IS NULL;
CREATE INDEX guardianships_account_id_idx ON guardianships (account_id);

CREATE TABLE invitations (
    id                 uuid PRIMARY KEY,
    type               text NOT NULL,
    child_id           uuid REFERENCES children (id) ON DELETE CASCADE,  -- RN-HIJ-08
    inviter_account_id uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    -- Datos del invitado: se borran al vencer (RN-PED-07).
    email              text,
    first_name         text,
    last_name          text,
    status             text NOT NULL DEFAULT 'PENDING',
    sent_at            timestamptz NOT NULL,
    expires_at         timestamptz NOT NULL,
    responded_at       timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz,
    CONSTRAINT invitations_type_check CHECK (type IN ('GUARDIAN', 'PEDIATRICIAN', 'CLAIM')),
    CONSTRAINT invitations_status_check
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT invitations_email_normalized CHECK (email = lower(btrim(email)))
);
CREATE INDEX invitations_email_status_idx ON invitations (email, status);
CREATE INDEX invitations_child_id_idx ON invitations (child_id);
CREATE INDEX invitations_inviter_account_id_idx ON invitations (inviter_account_id);

-- Avisa a los teléfonos de una cuenta que borren los datos de un niño (ADR-15, 14 §2). Sin FK:
-- el aviso tiene que seguir existiendo aunque se borre el niño o la cuenta.
CREATE TABLE access_revocations (
    id           uuid PRIMARY KEY,
    account_id   uuid NOT NULL,
    child_id     uuid NOT NULL,
    sync_version bigint NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz
);
CREATE INDEX access_revocations_account_id_idx ON access_revocations (account_id);

-- Crecimiento -----------------------------------------------------------------------------------

CREATE TABLE measurements (  -- ⟳
    id                    uuid PRIMARY KEY,
    child_id              uuid NOT NULL REFERENCES children (id) ON DELETE RESTRICT,
    measured_on           date NOT NULL,
    weight_kg             numeric(6, 3),
    length_cm             numeric(5, 1),
    length_position       text,  -- sus valores los define I1-B6
    head_circumference_cm numeric(5, 1),
    outlier_confirmed     boolean NOT NULL DEFAULT false,
    consultation_id       uuid,  -- la FK se agrega en I4, con consultations
    author_account_id     uuid NOT NULL,  -- sin FK: es autoría (RN-CTA-08)
    author_role           text NOT NULL,
    sync_version          bigint NOT NULL,
    deleted_at            timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz,
    CONSTRAINT measurements_at_least_one_value
        CHECK (weight_kg IS NOT NULL OR length_cm IS NOT NULL OR head_circumference_cm IS NOT NULL),
    CONSTRAINT measurements_author_role_check CHECK (author_role IN ('GUARDIAN', 'PEDIATRICIAN'))
);
-- Una medición por autor y por fecha, sin contar las borradas (RN-CRE-12).
CREATE UNIQUE INDEX measurements_one_per_author_and_day_idx
    ON measurements (child_id, author_account_id, measured_on) WHERE deleted_at IS NULL;
CREATE INDEX measurements_child_id_idx ON measurements (child_id);

-- Vacunación ------------------------------------------------------------------------------------

CREATE TABLE vaccines (
    id         uuid PRIMARY KEY,
    name       text NOT NULL,
    prevents   text,
    source     text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz,
    CONSTRAINT vaccines_name_key UNIQUE (name)  -- cada vacuna está una sola vez
);

CREATE TABLE calendar_versions (
    id             uuid PRIMARY KEY,
    effective_from date NOT NULL,
    status         text NOT NULL,  -- sus valores los definen I1-S1 e I1-B7
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz
);

CREATE TABLE calendar_doses (
    id             uuid PRIMARY KEY,
    version_id     uuid NOT NULL REFERENCES calendar_versions (id) ON DELETE RESTRICT,
    vaccine_id     uuid NOT NULL REFERENCES vaccines (id) ON DELETE RESTRICT,
    dose_code      text NOT NULL,
    dose_label     text NOT NULL,
    age_months     integer NOT NULL,
    lead_days      integer NOT NULL,
    tolerance_days integer NOT NULL,
    born_from      date,
    born_until     date,
    sort_order     integer NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz,
    -- NULLS NOT DISTINCT: también chocan dos filas sin born_from. Que los rangos de un mismo
    -- dose_code no se superpongan lo verifica calendarProblems() al cargar (RN-VAC-10).
    CONSTRAINT calendar_doses_version_dose_born_from_key
        UNIQUE NULLS NOT DISTINCT (version_id, dose_code, born_from),
    CONSTRAINT calendar_doses_born_range_check CHECK (born_from <= born_until)
);
CREATE INDEX calendar_doses_vaccine_id_idx ON calendar_doses (vaccine_id);

CREATE TABLE applied_doses (  -- ⟳, se escribe por REST (RNF-OFF-06)
    id                uuid PRIMARY KEY,
    child_id          uuid NOT NULL REFERENCES children (id) ON DELETE RESTRICT,
    dose_code         text,
    campaign_id       uuid,  -- la FK se agrega en I6, con campaigns
    applied_on        date,  -- nulo en la carga retroactiva
    lot               text,
    place             text,
    consultation_id   uuid,  -- la FK se agrega en I4, con consultations
    author_account_id uuid NOT NULL,  -- sin FK: es autoría (RN-CTA-08)
    author_role       text NOT NULL,
    sync_version      bigint NOT NULL,
    deleted_at        timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz,
    CONSTRAINT applied_doses_dose_or_campaign CHECK ((dose_code IS NULL) <> (campaign_id IS NULL)),
    CONSTRAINT applied_doses_lot_length CHECK (char_length(lot) <= 40),  -- SUP-24
    CONSTRAINT applied_doses_place_length CHECK (char_length(place) <= 120),
    CONSTRAINT applied_doses_author_role_check CHECK (author_role IN ('GUARDIAN', 'PEDIATRICIAN'))
);
-- Una dosis del calendario por niño, sin contar las borradas (RN-VAC-09).
CREATE UNIQUE INDEX applied_doses_one_per_dose_idx
    ON applied_doses (child_id, dose_code) WHERE deleted_at IS NULL;
CREATE INDEX applied_doses_child_id_idx ON applied_doses (child_id);

CREATE TABLE other_vaccines (  -- ⟳, se escribe por REST (RNF-OFF-06)
    id                uuid PRIMARY KEY,
    child_id          uuid NOT NULL REFERENCES children (id) ON DELETE RESTRICT,
    name              text NOT NULL,
    applied_on        date,
    lot               text,
    note              text,
    author_account_id uuid NOT NULL,  -- sin FK: es autoría (RN-CTA-08)
    author_role       text NOT NULL,
    sync_version      bigint NOT NULL,
    deleted_at        timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz,
    CONSTRAINT other_vaccines_name_length CHECK (char_length(name) <= 120),  -- SUP-24
    CONSTRAINT other_vaccines_lot_length CHECK (char_length(lot) <= 40),
    CONSTRAINT other_vaccines_note_length CHECK (char_length(note) <= 280),
    CONSTRAINT other_vaccines_author_role_check CHECK (author_role IN ('GUARDIAN', 'PEDIATRICIAN'))
);
CREATE INDEX other_vaccines_child_id_idx ON other_vaccines (child_id);
