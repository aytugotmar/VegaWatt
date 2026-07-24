ALTER TABLE notification_jobs
    ADD COLUMN appliance_id UUID REFERENCES appliances (id) ON DELETE CASCADE;

CREATE INDEX idx_notification_jobs_cooldown
    ON notification_jobs (home_id, appliance_id, trigger_type, created_at);
