-- Fixture of PermissionDaoImplTest only. Wipes the identity tables, then loads four permissions and three
-- roles. USER:READ is held by ADMIN and EDITOR (2 roles), ROLE:WRITE by ADMIN only (1 role), USER:WRITE and
-- ROLE:READ by no role (0), so countRolesWithPermission and the "still held" refusal have known answers.
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO roles (id, role_name) VALUES (1, 'ADMIN'), (2, 'EDITOR'), (3, 'VIEWER');

INSERT INTO permissions (id, resource, action) VALUES
    (1, 'USER', 'READ'), (2, 'USER', 'WRITE'), (3, 'ROLE', 'READ'), (4, 'ROLE', 'WRITE');

INSERT INTO role_permission (role_id, permission_id) VALUES (1, 1), (1, 4), (2, 1);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE roles_id_seq RESTART WITH 100;
ALTER SEQUENCE permissions_id_seq RESTART WITH 100;
