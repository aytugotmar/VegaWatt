package com.vegawatt.core.notification.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_jobs")
class NotificationJobEntity {

    @Id
    private UUID id;

    @Column(name = "trigger_event_id", nullable = false)
    private UUID triggerEventId;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(nullable = false)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationJobEntity() {
    }

    NotificationJobEntity(UUID id, UUID triggerEventId, UUID homeId, String triggerType, String status,
                          int attemptCount, Instant nextAttemptAt, String lastError, Instant createdAt) {
        this.id = id;
        this.triggerEventId = triggerEventId;
        this.homeId = homeId;
        this.triggerType = triggerType;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getTriggerEventId() {
        return triggerEventId;
    }

    UUID getHomeId() {
        return homeId;
    }

    String getTriggerType() {
        return triggerType;
    }

    String getStatus() {
        return status;
    }

    int getAttemptCount() {
        return attemptCount;
    }

    Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    String getLastError() {
        return lastError;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
