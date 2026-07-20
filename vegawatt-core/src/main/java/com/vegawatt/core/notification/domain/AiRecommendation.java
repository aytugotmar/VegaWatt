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
        Instant createdAt) {

    public static AiRecommendation create(UUID homeId, AdvisoryTriggerType triggerType, String content,
                                           boolean fallbackUsed, Instant createdAt) {
        return new AiRecommendation(UUID.randomUUID(), homeId, triggerType, content, fallbackUsed,
                EmailStatus.PENDING, createdAt);
    }

    public AiRecommendation withEmailStatus(EmailStatus newStatus) {
        return new AiRecommendation(id, homeId, triggerType, content, fallbackUsed, newStatus, createdAt);
    }
}
