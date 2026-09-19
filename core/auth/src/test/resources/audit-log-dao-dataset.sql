-- Fixture of AuditLogDaoImplTest only. Wipes the identity tables and the audit trail, then loads two audit
-- rows inserted out of chronological order on purpose (id 2 is older than id 1): row 1 has an actor and an
-- entity, row 2 has neither (NULL actor_user_id and NULL entity_id, as written by a bootstrap or a job).
-- audit_logs has no foreign key, so no user needs to exist.
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO audit_logs (id, actor_user_id, action, entity_type, entity_id, details, created_at) VALUES
    (2, NULL, 'BOOTSTRAP_ADMIN', 'USER', NULL, 'first row by id', '2026-09-19T08:00:00Z'),
    (1, 7, 'CREATE_ROLE', 'ROLE', 3, 'roleName=EDITOR', '2026-09-19T09:00:00Z');

-- Explicit ids do not advance the sequence: move it high so generated ids never collide
ALTER SEQUENCE audit_logs_id_seq RESTART WITH 100;
