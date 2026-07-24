package com.vegawatt.core.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationJobRepository {

    NotificationJob save(NotificationJob job);

    /**
     * Atomically claims up to {@code limit} jobs that are due at {@code now}, marking each so no
     * other worker will pick it up before {@code leaseUntil}, and returns them for processing.
     * Claiming and leasing happen in one transaction, so two workers running concurrently receive
     * disjoint batches rather than both processing the same job and sending the advisory twice. If
     * a worker dies mid-processing, the lease lapses and the job becomes due again, so nothing is
     * lost either.
     */
    List<NotificationJob> claimDue(Instant now, Instant leaseUntil, int limit);

    /**
     * True if a job for this exact home+appliance+triggerType combination (appliance {@code null}
     * for home-level triggers like a quota event) was created at or after {@code since} — the
     * notification-spam cooldown check. Callers still record the underlying {@code OperationalEvent}
     * regardless of this result; only the email-triggering job creation is gated.
     */
    boolean hasRecentJob(UUID homeId, UUID applianceId, AdvisoryTriggerType triggerType, Instant since);
}
