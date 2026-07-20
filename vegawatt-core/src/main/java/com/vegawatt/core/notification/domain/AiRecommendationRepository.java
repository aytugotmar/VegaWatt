package com.vegawatt.core.notification.domain;

import java.util.List;
import java.util.UUID;

public interface AiRecommendationRepository {

    AiRecommendation save(AiRecommendation recommendation);

    List<AiRecommendation> findByHomeId(UUID homeId);
}
