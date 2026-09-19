-- Fixture of PasswordResetTokenDaoImplTest only. Wipes the identity tables, then loads two users and
-- one reset token for each ('c3...' for user 1, 'd4...' for user 2).
TRUNCATE TABLE audit_logs, users, roles, permissions RESTART IDENTITY CASCADE;

INSERT INTO users (id, first_name, last_name, email, password, enabled, account_locked) VALUES
    (1, 'Alice', 'Martin', 'alice@example.com', 'hash-1', TRUE, FALSE),
    (2, 'Bob', 'Durand', 'bob@example.com', 'hash-2', TRUE, FALSE);

INSERT INTO password_reset_tokens (id, user_id, token, type, expiry_date) VALUES
    (1, 1, repeat('c3', 32), 'PASSWORD_RESET', '2026-09-19T11:00:00Z'),
    (2, 2, repeat('d4', 32), 'PASSWORD_RESET', '2026-09-19T12:00:00Z');

ALTER SEQUENCE users_id_seq RESTART WITH 100;
ALTER SEQUENCE password_reset_tokens_id_seq RESTART WITH 100;
