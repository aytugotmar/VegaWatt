CREATE TABLE homes (
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL,
    contact_email          VARCHAR(255) NOT NULL,
    energy_quota_kwh       NUMERIC(12, 4) NOT NULL,
    budget_quota_try       NUMERIC(12, 2) NOT NULL,
    base_tariff_per_kwh    NUMERIC(12, 2) NOT NULL,
    penalty_tariff_per_kwh NUMERIC(12, 2) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_homes_energy_quota_positive CHECK (energy_quota_kwh > 0),
    CONSTRAINT chk_homes_budget_quota_positive CHECK (budget_quota_try > 0),
    CONSTRAINT chk_homes_base_tariff_positive CHECK (base_tariff_per_kwh > 0),
    CONSTRAINT chk_homes_penalty_gte_base CHECK (penalty_tariff_per_kwh >= base_tariff_per_kwh)
);
