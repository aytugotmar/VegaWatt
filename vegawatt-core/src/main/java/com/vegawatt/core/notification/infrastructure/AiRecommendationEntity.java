package com.vegawatt.core.notification.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
class AiRecommendationEntity {

    @Id
    private UUID id;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(name = "email_status", nullable = false)
    private String emailStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiRecommendationEntity() {
    }

    AiRecommendationEntity(UUID id, UUID homeId, String triggerType, String content, boolean fallbackUsed,
                            String emailStatus, Instant createdAt) {
        this.id = id;
        this.homeId = homeId;
        this.triggerType = triggerType;
        this.content = content;
        this.fallbackUsed = fallbackUsed;
        this.emailStatus = emailStatus;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getHomeId() {
        return homeId;
    }

    String getTriggerType() {
        return triggerType;
    }

    String getContent() {
        return content;
    }

    boolean isFallbackUsed() {
        return fallbackUsed;
    }

    String getEmailStatus() {
        return emailStatus;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
