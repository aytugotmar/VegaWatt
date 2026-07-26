ALTER TABLE consumption_snapshots ADD COLUMN billing_period VARCHAR(7);

-- Best-effort backfill for existing rows: derives the period from snapshot_time in the business
-- time zone (Europe/Istanbul), matching BillingPeriodResolver. New rows going forward stamp the
-- period the accumulated value actually belonged to at capture time (HomeLiveState.billingPeriod),
-- which this backfill can't reconstruct for historical rows but approximates correctly in the
-- overwhelming majority of cases.
UPDATE consumption_snapshots
SET billing_period = to_char(snapshot_time AT TIME ZONE 'Europe/Istanbul', 'YYYY-MM')
WHERE billing_period IS NULL;

ALTER TABLE consumption_snapshots ALTER COLUMN billing_period SET NOT NULL;

CREATE INDEX idx_consumption_snapshots_home_id_billing_period ON consumption_snapshots (home_id, billing_period);
