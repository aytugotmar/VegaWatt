CREATE TABLE consumption_snapshots (
    id                     UUID PRIMARY KEY,
    home_id                UUID NOT NULL REFERENCES homes (id) ON DELETE CASCADE,
    snapshot_time          TIMESTAMPTZ NOT NULL,
    accumulated_energy_kwh NUMERIC(12, 4) NOT NULL,
    accumulated_cost       NUMERIC(12, 2) NOT NULL,
    tariff_state           VARCHAR(20) NOT NULL,
    CONSTRAINT chk_consumption_snapshots_tariff_state CHECK (tariff_state IN ('BASE', 'PENALTY'))
);

CREATE INDEX idx_consumption_snapshots_home_id_time ON consumption_snapshots (home_id, snapshot_time);
