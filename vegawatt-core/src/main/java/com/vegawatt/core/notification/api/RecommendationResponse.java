package com.vegawatt.core.notification.api;

import com.vegawatt.core.notification.domain.AiRecommendation;
import java.time.Instant;
import java.util.UUID;

public record RecommendationResponse(
        UUID id,
        String triggerType,
        String content,
        boolean fallbackUsed,
        String emailStatus,
        Instant createdAt) {

    public static RecommendationResponse from(AiRecommendation recommendation) {
        return new RecommendationResponse(recommendation.id(), recommendation.triggerType().name(),
                recommendation.content(), recommendation.fallbackUsed(), recommendation.emailStatus().name(),
                recommendation.createdAt());
    }
}
