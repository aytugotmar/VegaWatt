CREATE TABLE appliances (
    id                   UUID PRIMARY KEY,
    home_id              UUID NOT NULL REFERENCES homes (id) ON DELETE CASCADE,
    name                 VARCHAR(255) NOT NULL,
    type                 VARCHAR(100) NOT NULL,
    safe_power_limit_watt NUMERIC(10, 2) NOT NULL,
    simulation_min_watt  NUMERIC(10, 2) NOT NULL,
    simulation_max_watt  NUMERIC(10, 2) NOT NULL,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_appliances_home_id_name UNIQUE (home_id, name),
    CONSTRAINT chk_appliances_safe_power_limit_positive CHECK (safe_power_limit_watt > 0),
    CONSTRAINT chk_appliances_simulation_range CHECK (simulation_min_watt >= 0 AND simulation_max_watt > simulation_min_watt)
);

CREATE INDEX idx_appliances_home_id ON appliances (home_id);
