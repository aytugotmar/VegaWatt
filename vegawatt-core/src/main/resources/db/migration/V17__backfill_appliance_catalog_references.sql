-- Best-effort backfill: maps each pre-existing appliance's free-text `type` to the matching
-- appliance_catalog `code`, so older rows get the same catalog association a newly registered
-- appliance of that type would get. Only touches rows where a matching catalog code exists;
-- unknown/custom types are left untouched (catalog_item_id stays NULL), which is exactly how
-- they behave today, so nothing breaks for them.
--
-- 'TV' is the one legacy type value that doesn't match its catalog code 1:1 (catalog code is
-- 'TELEVISION'); every other legacy type value already equals its catalog code.
UPDATE appliances a
SET catalog_item_id = c.id,
    catalog_code_snapshot = c.code,
    behavior_profile_snapshot = c.behavior_profile,
    standby_min_watt = c.default_standby_min_watt,
    standby_max_watt = c.default_standby_max_watt
FROM appliance_catalog c
WHERE a.catalog_item_id IS NULL
  AND c.code = CASE a.type WHEN 'TV' THEN 'TELEVISION' ELSE a.type END;
