package com.vegawatt.core.notification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A durable record of "an advisory needs to be generated and emailed for this operational event",
 * created in the same PostgreSQL transaction as the event it stems from. Replaces firing the
 * notification directly off the telemetry-processing thread: a {@link NotificationJobRepository}-
 * backed worker polls and retries these instead, so a crash between the event being recorded and
 * the email being sent never silently drops the notification.
 */
public record NotificationJob(
        UUID id,
        UUID triggerEventId,
        UUID homeId,
        UUID applianceId,
        AdvisoryTriggerType triggerType,
        NotificationJobStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt) {

    public static NotificationJob create(UUID triggerEventId, UUID homeId, UUID applianceId,
                                          AdvisoryTriggerType triggerType, Instant now) {
        return new NotificationJob(UUID.randomUUID(), triggerEventId, homeId, applianceId, triggerType,
                NotificationJobStatus.PENDING, 0, now, null, now);
    }

    public NotificationJob markSent() {
        return new NotificationJob(id, triggerEventId, homeId, applianceId, triggerType, NotificationJobStatus.SENT,
                attemptCount, nextAttemptAt, null, createdAt);
    }

    public NotificationJob markFailedForRetry(String error, Instant nextAttemptAt) {
        return new NotificationJob(id, triggerEventId, homeId, applianceId, triggerType, NotificationJobStatus.PENDING,
                attemptCount + 1, nextAttemptAt, error, createdAt);
    }

    public NotificationJob markTerminallyFailed(String error) {
        return new NotificationJob(id, triggerEventId, homeId, applianceId, triggerType, NotificationJobStatus.FAILED,
                attemptCount + 1, nextAttemptAt, error, createdAt);
    }
}
