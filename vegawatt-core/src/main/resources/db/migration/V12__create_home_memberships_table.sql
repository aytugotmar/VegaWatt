CREATE TABLE home_memberships (
    id         UUID PRIMARY KEY,
    home_id    UUID NOT NULL REFERENCES homes (id),
    user_id    UUID NOT NULL REFERENCES users (id),
    role       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_home_memberships_role CHECK (role IN ('OWNER')),
    CONSTRAINT uq_home_memberships_home_user UNIQUE (home_id, user_id)
);

CREATE INDEX idx_home_memberships_user_id ON home_memberships (user_id);
