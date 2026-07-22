ALTER TABLE ai_recommendations
    ADD CONSTRAINT uq_ai_recommendations_trigger_event_id UNIQUE (trigger_event_id);

CREATE INDEX idx_consumption_snapshots_home_timestamp ON consumption_snapshots (home_id, timestamp);

CREATE INDEX idx_operational_events_home_timestamp ON operational_events (home_id, timestamp);
