package com.vegawatt.core.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record AiRecommendation(
        UUID id,
        UUID homeId,
        AdvisoryTriggerType triggerType,
        String content,
        boolean fallbackUsed,
        EmailStatus emailStatus,
        Instant createdAt,
        UUID triggerEventId) {

    public static AiRecommendation create(UUID homeId, AdvisoryTriggerType triggerType, String content,
                                           boolean fallbackUsed, Instant createdAt, UUID triggerEventId) {
        return new AiRecommendation(UUID.randomUUID(), homeId, triggerType, content, fallbackUsed,
                EmailStatus.PENDING, createdAt, triggerEventId);
    }

    public AiRecommendation withEmailStatus(EmailStatus newStatus) {
        return new AiRecommendation(id, homeId, triggerType, content, fallbackUsed, newStatus, createdAt,
                triggerEventId);
    }
}
