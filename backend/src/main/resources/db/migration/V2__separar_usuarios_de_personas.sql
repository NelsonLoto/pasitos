-- Separa el login de "personas" en 3 partes:
--   - "personas" suma dni (tutor) y matricula (pediatra), y conserva
--     email (ya existia desde V1): son parte de quien es la persona, no
--     el secreto en si.
--   - "usuarios" (nueva) tiene solo el secreto -password_hash,
--     auth_provider-. 1-a-1 con "personas" (persona_id es a la vez PK y
--     FK). El login se resuelve buscando primero en "personas" (por
--     email/dni/matricula segun el rol) para encontrar el id, y recien
--     ahi en "usuarios" para validar la contraseña.
--   - "sesiones" (nueva) registra cada login activo, para poder revocarlo
--     del lado del servidor antes de que el JWT expire solo (perdida de
--     celular, etc). Apunta a usuarios.persona_id (no a personas.id
--     directo): asi la base impide que un "hijo" -sin credenciales- tenga
--     sesiones. Revocar = borrar la fila, no hay columna de "revocado".
--
-- IMPORTANTE: al momento de este commit, AuthRoutes.kt / ChildRoutes.kt /
-- PersonasTable.kt todavia leen personas.password_hash /
-- personas.auth_provider directo -esta migracion los deja sin esas
-- columnas-. El refactor de ese codigo para usar "usuarios" (y
-- opcionalmente "sesiones") queda pendiente; hasta que se haga, el login
-- y registro via la app no van a funcionar contra una base que ya tenga
-- esta migracion aplicada.

-- 1) personas: sumar dni y matricula (email ya existe desde V1)
ALTER TABLE "personas" ADD COLUMN "dni" varchar UNIQUE;
ALTER TABLE "personas" ADD COLUMN "matricula" varchar UNIQUE;

-- 2) usuarios: tabla nueva, solo el secreto
CREATE TABLE "usuarios" (
  "persona_id" uuid PRIMARY KEY,
  "password_hash" varchar,
  "auth_provider" varchar,
  "created_at" timestamp NOT NULL DEFAULT (now())
);

ALTER TABLE "usuarios" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("id") DEFERRABLE INITIALLY IMMEDIATE;

-- 3) Migrar password_hash/auth_provider existentes de personas -> usuarios
-- (email se queda en personas, no se migra)
INSERT INTO "usuarios" ("persona_id", "password_hash", "auth_provider", "created_at")
SELECT "id", "password_hash", "auth_provider", "created_at"
FROM "personas"
WHERE "password_hash" IS NOT NULL OR "auth_provider" IS NOT NULL;

-- 4) Sacar el secreto de "personas" (email se queda, es identificacion)
ALTER TABLE "personas" DROP COLUMN "password_hash";
ALTER TABLE "personas" DROP COLUMN "auth_provider";

-- 5) sesiones: tabla nueva, un login activo por fila
CREATE TABLE "sesiones" (
  "id" uuid PRIMARY KEY,
  "usuario_id" uuid NOT NULL,
  "token_hash" varchar NOT NULL UNIQUE,
  "created_at" timestamp NOT NULL DEFAULT (now()),
  "expires_at" timestamp NOT NULL
);

ALTER TABLE "sesiones" ADD FOREIGN KEY ("usuario_id") REFERENCES "usuarios" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

COMMENT ON COLUMN "personas"."tipo_persona" IS 'tutor | hijo | pediatra';
COMMENT ON COLUMN "personas"."email" IS 'tutor - login (RF01)';
COMMENT ON COLUMN "personas"."dni" IS 'tutor - documento de identidad';
COMMENT ON COLUMN "personas"."matricula" IS 'pediatra - matricula profesional (fuera de alcance actual, ver Anexo I)';
COMMENT ON COLUMN "usuarios"."password_hash" IS 'null si login social';
COMMENT ON COLUMN "usuarios"."auth_provider" IS 'email | google';
COMMENT ON COLUMN "sesiones"."usuario_id" IS 'FK a usuarios.persona_id - quien inicio esta sesion';
COMMENT ON COLUMN "sesiones"."token_hash" IS 'hash del JWT emitido, nunca el token en texto plano';
COMMENT ON COLUMN "sesiones"."expires_at" IS 'mismo vencimiento que el JWT (JWT_EXPIRATION_MINUTES)';
