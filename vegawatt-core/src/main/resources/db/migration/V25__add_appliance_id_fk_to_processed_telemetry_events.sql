-- appliance_id was left without a foreign key when this table was created (V8), asymmetric with
-- home_id on the same row, which already references homes. Mirrors home_id's ON DELETE CASCADE.
ALTER TABLE processed_telemetry_events
    ADD CONSTRAINT fk_processed_telemetry_events_appliance_id
    FOREIGN KEY (appliance_id) REFERENCES appliances (id) ON DELETE CASCADE;

CREATE INDEX idx_processed_telemetry_events_appliance_id ON processed_telemetry_events (appliance_id);
