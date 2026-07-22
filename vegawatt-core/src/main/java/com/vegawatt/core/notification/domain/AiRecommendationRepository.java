package com.vegawatt.core.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiRecommendationRepository {

    AiRecommendation save(AiRecommendation recommendation);

    List<AiRecommendation> findByHomeId(UUID homeId);

    /**
     * Finds the advisory already generated for an operational event, if any. A retry uses this
     * to reuse its previous advisory instead of paying for a second model call and writing a
     * second row for one event.
     */
    Optional<AiRecommendation> findByTriggerEventId(UUID triggerEventId);
}
