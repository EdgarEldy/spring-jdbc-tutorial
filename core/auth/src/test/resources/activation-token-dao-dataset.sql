-- Fixture of ActivationTokenDaoImplTest only. Wipes the identity tables, then loads two users (1 and 2)
-- and two activation tokens: 'a1...' for user 1 (not validated), 'b2...' for user 2 (already validated).
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO users (id, first_name, last_name, email, password, enabled, account_locked) VALUES
    (1, 'Alice', 'Martin', 'alice@example.com', 'hash-1', FALSE, FALSE),
    (2, 'Bob', 'Durand', 'bob@example.com', 'hash-2', TRUE, FALSE);

INSERT INTO activation_tokens (id, user_id, token, created_at, expires_at, validated_at) VALUES
    (1, 1, repeat('a1', 32), '2026-09-19T10:00:00Z', '2026-09-20T10:00:00Z', NULL),
    (2, 2, repeat('b2', 32), '2026-09-18T10:00:00Z', '2026-09-19T10:00:00Z', '2026-09-18T11:00:00Z');

ALTER SEQUENCE users_id_seq RESTART WITH 100;
ALTER SEQUENCE activation_tokens_id_seq RESTART WITH 100;
