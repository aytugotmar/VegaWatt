CREATE TABLE refresh_sessions (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_sessions_user_id ON refresh_sessions (user_id);
