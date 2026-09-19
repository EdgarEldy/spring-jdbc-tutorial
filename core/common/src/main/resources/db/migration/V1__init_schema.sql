-- Initial schema of both independent domains (identity/RBAC and e-commerce).
-- Flyway is the single source of truth of the schema: nothing else creates or alters tables.
-- The two domains share one database but have no foreign key between them.

-- ---------------------------------------------------------------------------
-- Identity / RBAC domain
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    -- bcrypt hash, never the clear password
    password       VARCHAR(100) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT FALSE,
    account_locked BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Emails are compared case-insensitively: the unique index is on the lower-cased value
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));

CREATE TABLE roles (
    id        BIGSERIAL    PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_roles_role_name UNIQUE (role_name)
);

CREATE TABLE permissions (
    id       BIGSERIAL    PRIMARY KEY,
    resource VARCHAR(100) NOT NULL,
    action   VARCHAR(100) NOT NULL,
    CONSTRAINT uq_permissions_resource_action UNIQUE (resource, action)
);

-- Roles are assigned onto a user, never the reverse
CREATE TABLE role_user (
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, user_id),
    -- No cascade on the role: deleting a role still assigned to a user is refused by the service
    CONSTRAINT fk_role_user_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_user_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_user_user ON role_user (user_id);

-- Permissions are assigned onto a role, never the reverse
CREATE TABLE role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    -- No cascade on the permission: deleting a permission still held by a role is refused by the service
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE INDEX idx_role_permission_permission ON role_permission (permission_id);

-- Activation and password reset tokens are stored as SHA-256 hex digests only (64 characters)
CREATE TABLE activation_tokens (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    token        VARCHAR(64) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    validated_at TIMESTAMPTZ,
    CONSTRAINT uq_activation_tokens_token UNIQUE (token),
    CONSTRAINT fk_activation_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_activation_tokens_user ON activation_tokens (user_id);

-- A revoked JWT: token is the SHA-256 hex digest of the JWT (never the JWT itself), jti its unique id
CREATE TABLE blacklisted_tokens (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    token          VARCHAR(64)  NOT NULL,
    jti            VARCHAR(100) NOT NULL,
    blacklisted_at TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    validated_at   TIMESTAMPTZ,
    CONSTRAINT uq_blacklisted_tokens_token UNIQUE (token),
    CONSTRAINT uq_blacklisted_tokens_jti UNIQUE (jti),
    CONSTRAINT fk_blacklisted_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- The scheduled cleanup deletes rows by expiry
CREATE INDEX idx_blacklisted_tokens_expires_at ON blacklisted_tokens (expires_at);

CREATE TABLE password_reset_tokens (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token       VARCHAR(64) NOT NULL,
    type        VARCHAR(30) NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

-- Audit trail of every RBAC mutation. No foreign key on purpose: the trail must outlive
-- whatever it references, and a refusal row is written in an independent transaction.
CREATE TABLE audit_logs (
    id            BIGSERIAL    PRIMARY KEY,
    actor_user_id BIGINT,
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     BIGINT,
    details       TEXT,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

-- ---------------------------------------------------------------------------
-- E-commerce domain
-- ---------------------------------------------------------------------------

CREATE TABLE categories (
    id            BIGSERIAL    PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_categories_category_name UNIQUE (category_name)
);

CREATE TABLE products (
    id           BIGSERIAL     PRIMARY KEY,
    category_id  BIGINT        NOT NULL,
    product_name VARCHAR(150)  NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    CONSTRAINT ck_products_unit_price CHECK (unit_price >= 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category ON products (category_id);

CREATE TABLE customers (
    id         BIGSERIAL    PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    telephone  VARCHAR(30),
    email      VARCHAR(255),
    address    VARCHAR(255)
);

CREATE TABLE orders (
    id          BIGSERIAL      PRIMARY KEY,
    customer_id BIGINT         NOT NULL,
    product_id  BIGINT         NOT NULL,
    quantity    INTEGER        NOT NULL,
    total       NUMERIC(14, 2) NOT NULL,
    CONSTRAINT ck_orders_quantity CHECK (quantity > 0),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_product ON orders (product_id);
