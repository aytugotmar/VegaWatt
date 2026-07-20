CREATE TABLE processed_telemetry_events (
    event_id     UUID PRIMARY KEY,
    home_id      UUID NOT NULL REFERENCES homes (id) ON DELETE CASCADE,
    appliance_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_processed_telemetry_events_home_id ON processed_telemetry_events (home_id);
