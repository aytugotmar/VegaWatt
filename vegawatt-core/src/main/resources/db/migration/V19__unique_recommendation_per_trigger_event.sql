-- One advisory per operational event.
--
-- GenerateEnergyAdvisoryUseCase now looks up an existing recommendation before calling the
-- model, which handles the ordinary retry. This constraint is the guarantee underneath that
-- lookup: two workers claiming the same job concurrently would otherwise both miss and both
-- insert, and nothing downstream would notice two pieces of advice for one event.

-- Existing databases may already hold duplicates, since the retry path could write one per
-- attempt before this change. A constraint added without this cleanup fails only on databases
-- that have been running, which is to say never in CI and always on the machine that has been
-- up since yesterday. Keep the earliest row per event, id as the tie-break so the result does
-- not depend on timestamp collisions.
DELETE FROM ai_recommendations
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY trigger_event_id ORDER BY created_at, id) AS row_number
        FROM ai_recommendations
        WHERE trigger_event_id IS NOT NULL
    ) ranked
    WHERE ranked.row_number > 1
);

ALTER TABLE ai_recommendations
    ADD CONSTRAINT uq_ai_recommendations_trigger_event UNIQUE (trigger_event_id);

-- The unique constraint creates its own index on the same column, so the one from V9 is now
-- redundant and would only cost write time.
DROP INDEX IF EXISTS idx_ai_recommendations_trigger_event_id;
