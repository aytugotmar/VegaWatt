-- V17 only matched legacy `type` values that equal their catalog `code` 1:1 (plus the one
-- TV -> TELEVISION special case). Two more legacy type values predate the catalog split and have
-- no 1:1 code match, so they were silently skipped and stayed catalog_item_id IS NULL:
--   COMPUTER -> DESKTOP_COMPUTER (the catalog split COMPUTER into DESKTOP_COMPUTER/GAMING_COMPUTER)
--   HEATER   -> ELECTRIC_HEATER  (the catalog split HEATER into ELECTRIC_HEATER/FAN_HEATER/WATER_HEATER)
-- Never edit V17 after it has run in any environment; this backfill is additive and idempotent
-- (WHERE catalog_item_id IS NULL means it only ever touches rows V17 missed).
UPDATE appliances a
SET catalog_item_id = c.id,
    catalog_code_snapshot = c.code,
    behavior_profile_snapshot = c.behavior_profile,
    standby_min_watt = c.default_standby_min_watt,
    standby_max_watt = c.default_standby_max_watt
FROM appliance_catalog c
WHERE a.catalog_item_id IS NULL
  AND c.code = CASE a.type WHEN 'COMPUTER' THEN 'DESKTOP_COMPUTER' WHEN 'HEATER' THEN 'ELECTRIC_HEATER' ELSE NULL END;
