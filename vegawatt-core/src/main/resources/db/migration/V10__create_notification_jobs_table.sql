CREATE TABLE notification_jobs (
    id                UUID PRIMARY KEY,
    trigger_event_id  UUID NOT NULL UNIQUE REFERENCES operational_events (id),
    home_id           UUID NOT NULL REFERENCES homes (id) ON DELETE CASCADE,
    trigger_type      VARCHAR(50) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    attempt_count     INT NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ NOT NULL,
    last_error        TEXT,
    created_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notification_jobs_due ON notification_jobs (status, next_attempt_at);
