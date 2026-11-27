-- Una versión entregada al teléfono nunca debe adelantar una escritura sin confirmar.
-- El lock se toma antes de asignar la secuencia y dura hasta commit/rollback.
CREATE OR REPLACE FUNCTION set_sync_version() RETURNS trigger AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(162, 1);
    NEW.sync_version := nextval('sync_seq');
    NEW.updated_at := now();
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER children_sync_version BEFORE INSERT OR UPDATE ON children
    FOR EACH ROW EXECUTE FUNCTION set_sync_version();
CREATE TRIGGER guardianships_sync_version BEFORE INSERT OR UPDATE ON guardianships
    FOR EACH ROW EXECUTE FUNCTION set_sync_version();
CREATE TRIGGER measurements_sync_version BEFORE INSERT OR UPDATE ON measurements
    FOR EACH ROW EXECUTE FUNCTION set_sync_version();
CREATE TRIGGER applied_doses_sync_version BEFORE INSERT OR UPDATE ON applied_doses
    FOR EACH ROW EXECUTE FUNCTION set_sync_version();
CREATE TRIGGER other_vaccines_sync_version BEFORE INSERT OR UPDATE ON other_vaccines
    FOR EACH ROW EXECUTE FUNCTION set_sync_version();
CREATE TRIGGER access_revocations_sync_version BEFORE INSERT OR UPDATE ON access_revocations
    FOR EACH ROW EXECUTE FUNCTION set_sync_version();

-- Conservar versiones existentes, incluso en una base de desarrollo sembrada a mano.
-- La secuencia solo avanza; no se reasignan las versiones ya entregadas.
DO $$
DECLARE newest bigint;
BEGIN
    SELECT greatest(
        (SELECT CASE WHEN is_called THEN last_value ELSE 0 END FROM sync_seq),
        coalesce((SELECT max(sync_version) FROM children), 0),
        coalesce((SELECT max(sync_version) FROM guardianships), 0),
        coalesce((SELECT max(sync_version) FROM measurements), 0),
        coalesce((SELECT max(sync_version) FROM applied_doses), 0),
        coalesce((SELECT max(sync_version) FROM other_vaccines), 0),
        coalesce((SELECT max(sync_version) FROM access_revocations), 0)
    ) INTO newest;
    IF newest > 0 THEN PERFORM setval('sync_seq', newest, true); END IF;
END $$;

CREATE INDEX children_sync_version_idx ON children (sync_version);
CREATE INDEX guardianships_account_sync_idx ON guardianships (account_id, sync_version);
CREATE INDEX measurements_child_sync_idx ON measurements (child_id, sync_version);
CREATE INDEX applied_doses_child_sync_idx ON applied_doses (child_id, sync_version);
CREATE INDEX other_vaccines_child_sync_idx ON other_vaccines (child_id, sync_version);
CREATE INDEX access_revocations_account_sync_idx ON access_revocations (account_id, sync_version);
