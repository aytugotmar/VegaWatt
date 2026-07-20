CREATE TABLE ai_recommendations (
    id            UUID PRIMARY KEY,
    home_id       UUID NOT NULL REFERENCES homes (id) ON DELETE CASCADE,
    trigger_type  VARCHAR(50) NOT NULL,
    content       TEXT NOT NULL,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    email_status  VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ai_recommendations_home_id_created_at ON ai_recommendations (home_id, created_at);
