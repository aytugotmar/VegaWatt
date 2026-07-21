ALTER TABLE ai_recommendations
    ADD COLUMN trigger_event_id UUID REFERENCES operational_events (id);

CREATE INDEX idx_ai_recommendations_trigger_event_id ON ai_recommendations (trigger_event_id);
