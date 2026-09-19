-- Fixture of BlacklistedTokenDaoImplTest only. Wipes the identity tables, then loads one user and three
-- blacklisted tokens: two expired before 2026-09-19T00:00:00Z ('e5...', 'f6...') and one still live ('a7...').
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO users (id, first_name, last_name, email, password, enabled, account_locked) VALUES
    (1, 'Alice', 'Martin', 'alice@example.com', 'hash-1', TRUE, FALSE);

INSERT INTO blacklisted_tokens (id, user_id, token, jti, blacklisted_at, created_at, expires_at, validated_at) VALUES
    (1, 1, repeat('e5', 32), 'jti-expired-1', '2026-09-17T10:30:00Z', '2026-09-17T10:00:00Z', '2026-09-17T11:00:00Z', NULL),
    (2, 1, repeat('f6', 32), 'jti-expired-2', '2026-09-17T12:30:00Z', '2026-09-17T12:00:00Z', '2026-09-17T13:00:00Z', NULL),
    (3, 1, repeat('a7', 32), 'jti-live', '2026-09-19T10:30:00Z', '2026-09-19T10:00:00Z', '2026-09-19T11:00:00Z', NULL);

ALTER SEQUENCE users_id_seq RESTART WITH 100;
ALTER SEQUENCE blacklisted_tokens_id_seq RESTART WITH 100;
