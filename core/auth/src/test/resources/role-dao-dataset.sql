-- Fixture of RoleDaoImplTest only. Wipes the identity tables, then loads two users, four roles and four
-- permissions. ADMIN holds all four permissions, EDITOR holds USER:READ, VIEWER holds ROLE:READ and has no
-- user, GUEST has neither permission nor user. alice (1) holds ADMIN and EDITOR, bob (2) holds EDITOR.
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO users (id, first_name, last_name, email, password, enabled, account_locked) VALUES
    (1, 'Alice', 'Martin', 'alice@example.com', 'hash-alice', TRUE, FALSE),
    (2, 'Bob', 'Durand', 'bob@example.com', 'hash-bob', TRUE, FALSE);

INSERT INTO roles (id, role_name) VALUES (1, 'ADMIN'), (2, 'EDITOR'), (3, 'VIEWER'), (4, 'GUEST');

INSERT INTO permissions (id, resource, action) VALUES
    (1, 'USER', 'READ'), (2, 'USER', 'WRITE'), (3, 'ROLE', 'READ'), (4, 'ROLE', 'WRITE');

INSERT INTO role_permission (role_id, permission_id) VALUES (1, 1), (1, 2), (1, 3), (1, 4), (2, 1), (3, 3);

INSERT INTO role_user (role_id, user_id) VALUES (1, 1), (2, 1), (2, 2);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE users_id_seq RESTART WITH 100;
ALTER SEQUENCE roles_id_seq RESTART WITH 100;
ALTER SEQUENCE permissions_id_seq RESTART WITH 100;
