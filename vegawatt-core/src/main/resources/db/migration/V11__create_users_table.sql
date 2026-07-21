CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);
