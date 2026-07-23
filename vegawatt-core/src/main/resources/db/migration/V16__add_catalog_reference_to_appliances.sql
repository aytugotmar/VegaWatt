-- Additive: lets a user's appliance instance optionally reference the catalog item it was
-- created from. Snapshot columns (catalog_code_snapshot, behavior_profile_snapshot,
-- standby_*_watt) freeze the catalog values at registration time so later edits to the
-- catalog template never silently change an already-registered appliance's behavior.
ALTER TABLE appliances ADD COLUMN catalog_item_id UUID REFERENCES appliance_catalog (id);
ALTER TABLE appliances ADD COLUMN catalog_code_snapshot VARCHAR(64);
ALTER TABLE appliances ADD COLUMN behavior_profile_snapshot VARCHAR(50);
ALTER TABLE appliances ADD COLUMN standby_min_watt NUMERIC(10, 2);
ALTER TABLE appliances ADD COLUMN standby_max_watt NUMERIC(10, 2);

ALTER TABLE appliances ADD CONSTRAINT chk_appliances_standby_range CHECK (
    standby_min_watt IS NULL OR standby_max_watt IS NULL OR standby_max_watt >= standby_min_watt);

CREATE INDEX idx_appliances_catalog_item_id ON appliances (catalog_item_id);
