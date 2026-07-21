package com.vegawatt.core.notification.domain;

import java.util.UUID;

/**
 * Pairs an advisory trigger with the {@code operational_events} row that caused it, so the
 * eventual AI recommendation can be traced back to the exact quota-breach or anomaly event.
 */
public record AdvisoryTrigger(AdvisoryTriggerType type, UUID operationalEventId) {
}
