ALTER TABLE sys_users
    ADD COLUMN IF NOT EXISTS auth_version bigint;

UPDATE sys_users
SET auth_version = 0
WHERE auth_version IS NULL;

ALTER TABLE sys_users
    ALTER COLUMN auth_version SET DEFAULT 0,
    ALTER COLUMN auth_version SET NOT NULL;
