CREATE INDEX IF NOT EXISTS idx_consumption_snapshots_home_snapshot_time ON consumption_snapshots (home_id, snapshot_time);

CREATE INDEX IF NOT EXISTS idx_operational_events_home_event_time ON operational_events (home_id, event_time);
