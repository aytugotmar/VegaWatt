CREATE TABLE billing_accounts (
    id                        UUID PRIMARY KEY,
    home_id                   UUID NOT NULL REFERENCES homes (id) ON DELETE CASCADE,
    billing_period            VARCHAR(7) NOT NULL,
    accumulated_energy_kwh    NUMERIC(12, 4) NOT NULL DEFAULT 0,
    accumulated_cost          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    penalty_active            BOOLEAN NOT NULL DEFAULT FALSE,
    energy_quota_80_notified  BOOLEAN NOT NULL DEFAULT FALSE,
    energy_quota_100_notified BOOLEAN NOT NULL DEFAULT FALSE,
    budget_quota_80_notified  BOOLEAN NOT NULL DEFAULT FALSE,
    budget_quota_100_notified BOOLEAN NOT NULL DEFAULT FALSE,
    version                   BIGINT NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_billing_accounts_home_period UNIQUE (home_id, billing_period)
);

CREATE INDEX idx_billing_accounts_home_id ON billing_accounts (home_id);
