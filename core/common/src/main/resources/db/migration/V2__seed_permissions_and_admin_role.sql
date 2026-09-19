-- Baseline RBAC seed: the permissions every resource of this project defines and an ADMIN role holding all of them.
-- Chicken and egg: creating a role or granting a permission requires ROLE:WRITE, so without a seeded role that
-- already carries it, nobody could ever obtain ROLE:WRITE and the RBAC endpoints would be unreachable.
-- No user is seeded here: an administrator account is created only by the optional, configuration driven bootstrap.
-- Idempotent on purpose: every statement can run against a database that already holds part of this data.

INSERT INTO permissions (resource, action)
SELECT r.resource, a.action
FROM (VALUES ('USER'), ('ROLE'), ('PERMISSION'), ('CATEGORY'), ('PRODUCT'), ('CUSTOMER'), ('ORDER')) AS r (resource)
CROSS JOIN (VALUES ('READ'), ('WRITE')) AS a (action)
ON CONFLICT (resource, action) DO NOTHING;

INSERT INTO roles (role_name)
VALUES ('ADMIN')
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
