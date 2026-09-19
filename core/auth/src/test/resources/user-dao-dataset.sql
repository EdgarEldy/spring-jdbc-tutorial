-- Fixture of UserDaoImplTest only. Wipes the identity tables, then loads three users, three roles and
-- three permissions. alice (id 1) holds ADMIN and EDITOR, which both grant USER:READ (tests DISTINCT).
-- bob (id 2) is disabled with no role, carol (id 3) is enabled but locked.
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO users (id, first_name, last_name, email, password, enabled, account_locked) VALUES
    (1, 'Alice', 'Martin', 'alice@example.com', '$2a$04$alicealicealicealiceaeAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', TRUE, FALSE),
    (2, 'Bob', 'Durand', 'bob@example.com', '$2a$04$bobbobbobbobbobbobbobeAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', FALSE, FALSE),
    (3, 'Carol', 'Petit', 'carol@example.com', '$2a$04$carolcarolcarolcarolceAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', TRUE, TRUE);

INSERT INTO roles (id, role_name) VALUES (1, 'ADMIN'), (2, 'EDITOR'), (3, 'VIEWER');

INSERT INTO permissions (id, resource, action) VALUES (1, 'USER', 'READ'), (2, 'USER', 'WRITE'), (3, 'ROLE', 'READ');

INSERT INTO role_permission (role_id, permission_id) VALUES (1, 1), (1, 2), (1, 3), (2, 1), (3, 3);

INSERT INTO role_user (role_id, user_id) VALUES (1, 1), (2, 1);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE users_id_seq RESTART WITH 100;
ALTER SEQUENCE roles_id_seq RESTART WITH 100;
ALTER SEQUENCE permissions_id_seq RESTART WITH 100;
