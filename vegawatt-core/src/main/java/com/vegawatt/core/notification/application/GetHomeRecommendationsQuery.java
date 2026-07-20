package com.vegawatt.core.notification.application;

import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.AiRecommendationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetHomeRecommendationsQuery {

    private final AiRecommendationRepository aiRecommendationRepository;

    public GetHomeRecommendationsQuery(AiRecommendationRepository aiRecommendationRepository) {
        this.aiRecommendationRepository = aiRecommendationRepository;
    }

    public List<AiRecommendation> execute(UUID homeId) {
        return aiRecommendationRepository.findByHomeId(homeId);
    }
}
