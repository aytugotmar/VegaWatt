CREATE TABLE operational_events (
    id           UUID PRIMARY KEY,
    home_id      UUID NOT NULL REFERENCES homes (id) ON DELETE CASCADE,
    appliance_id UUID NULL REFERENCES appliances (id) ON DELETE CASCADE,
    event_type   VARCHAR(50) NOT NULL,
    event_time   TIMESTAMPTZ NOT NULL,
    details      TEXT NULL
);

CREATE INDEX idx_operational_events_home_id_time ON operational_events (home_id, event_time);
