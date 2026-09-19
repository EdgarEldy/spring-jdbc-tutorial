-- Fixture of UserDaoRbacTest only (the RBAC additions of UserDao). Wipes the identity tables, then loads six
-- users and five roles. ROLE:WRITE is granted by ADMIN, EDITOR and SUPER (roles 1, 2, 5); VIEWER (3) only
-- grants ROLE:READ; NONE (4) grants nothing.
--   alice (1) enabled, unlocked, holds ADMIN and EDITOR (two roles granting ROLE:WRITE: counted once)
--   bob   (2) DISABLED, holds ADMIN         -> excluded from the last-admin count
--   carol (3) enabled but LOCKED, holds ADMIN -> excluded
--   dave  (4) enabled, unlocked, holds VIEWER (no ROLE:WRITE) -> not a candidate
--   erin  (5) enabled, unlocked, no role
--   frank (6) enabled, unlocked, holds SUPER -> candidate
-- So the last-admin candidates are alice and frank (2).
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO users (id, first_name, last_name, email, password, enabled, account_locked) VALUES
    (1, 'Alice', 'Martin', 'alice@example.com', 'hash-alice', TRUE, FALSE),
    (2, 'Bob', 'Durand', 'bob@example.com', 'hash-bob', FALSE, FALSE),
    (3, 'Carol', 'Petit', 'carol@example.com', 'hash-carol', TRUE, TRUE),
    (4, 'Dave', 'Roux', 'dave@example.com', 'hash-dave', TRUE, FALSE),
    (5, 'Erin', 'Blanc', 'erin@example.com', 'hash-erin', TRUE, FALSE),
    (6, 'Frank', 'Noir', 'frank@example.com', 'hash-frank', TRUE, FALSE);

INSERT INTO roles (id, role_name) VALUES (1, 'ADMIN'), (2, 'EDITOR'), (3, 'VIEWER'), (4, 'NONE'), (5, 'SUPER');

INSERT INTO permissions (id, resource, action) VALUES (1, 'ROLE', 'WRITE'), (2, 'ROLE', 'READ');

INSERT INTO role_permission (role_id, permission_id) VALUES (1, 1), (2, 1), (3, 2), (5, 1);

INSERT INTO role_user (role_id, user_id) VALUES (1, 1), (2, 1), (1, 2), (1, 3), (3, 4), (5, 6);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE users_id_seq RESTART WITH 100;
ALTER SEQUENCE roles_id_seq RESTART WITH 100;
ALTER SEQUENCE permissions_id_seq RESTART WITH 100;
