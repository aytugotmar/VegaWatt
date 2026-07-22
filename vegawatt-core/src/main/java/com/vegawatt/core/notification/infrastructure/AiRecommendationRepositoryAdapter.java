package com.vegawatt.core.notification.infrastructure;

import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.AiRecommendationRepository;
import com.vegawatt.core.notification.domain.EmailStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AiRecommendationRepositoryAdapter implements AiRecommendationRepository {

    private final AiRecommendationJpaRepository jpaRepository;

    AiRecommendationRepositoryAdapter(AiRecommendationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AiRecommendation save(AiRecommendation recommendation) {
        AiRecommendationEntity entity = new AiRecommendationEntity(recommendation.id(), recommendation.homeId(),
                recommendation.triggerType().name(), recommendation.content(), recommendation.fallbackUsed(),
                recommendation.emailStatus().name(), recommendation.createdAt(), recommendation.triggerEventId());
        jpaRepository.save(entity);
        return recommendation;
    }

    @Override
    public List<AiRecommendation> findByHomeId(UUID homeId) {
        return jpaRepository.findByHomeIdOrderByCreatedAtDesc(homeId).stream()
                .map(AiRecommendationRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<AiRecommendation> findByTriggerEventId(UUID triggerEventId) {
        return jpaRepository.findByTriggerEventId(triggerEventId)
                .map(AiRecommendationRepositoryAdapter::toDomain);
    }

    private static AiRecommendation toDomain(AiRecommendationEntity entity) {
        return new AiRecommendation(entity.getId(), entity.getHomeId(),
                AdvisoryTriggerType.valueOf(entity.getTriggerType()), entity.getContent(), entity.isFallbackUsed(),
                EmailStatus.valueOf(entity.getEmailStatus()), entity.getCreatedAt(), entity.getTriggerEventId());
    }
}
