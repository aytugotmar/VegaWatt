-- V7's partial index predated dead_lettered (added in V23), so it only covered
-- "published_at IS NULL" while the actual pending-events query
-- (findByPublishedAtIsNullAndDeadLetteredFalseOrderByCreatedAtAsc) filters on
-- "published_at IS NULL AND dead_lettered = FALSE". Recreating it with the matching predicate.
DROP INDEX IF EXISTS idx_outbox_events_unpublished;
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (created_at)
    WHERE published_at IS NULL AND dead_lettered = FALSE;
