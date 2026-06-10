-- Migration: copy roles from user_roles join table into users.role
-- Postgres SQL. Run in a safe maintenance window and ensure DB backup exists.

BEGIN;

-- add role column (nullable for now)
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(32);

-- populate role per user with priority: ADMIN > DEVELOPER > NORMAL_USER
UPDATE users
SET role = COALESCE(sub.role, 'NORMAL_USER')
FROM (
  SELECT user_id,
    CASE
      WHEN bool_or(upper(role) = 'ADMIN') THEN 'ADMIN'
      WHEN bool_or(upper(role) = 'DEVELOPER') THEN 'DEVELOPER'
      ELSE 'NORMAL_USER'
    END AS role
  FROM user_roles
  GROUP BY user_id
) AS sub
WHERE users.id = sub.user_id;

-- ensure any users without entries get NORMAL_USER
UPDATE users SET role = 'NORMAL_USER' WHERE role IS NULL;

-- make column NOT NULL and set default
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'NORMAL_USER';
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

-- drop the legacy join table
DROP TABLE IF EXISTS user_roles;

COMMIT;
