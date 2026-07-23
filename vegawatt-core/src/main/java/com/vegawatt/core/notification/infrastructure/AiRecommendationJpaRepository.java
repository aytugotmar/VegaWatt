package com.vegawatt.core.notification.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiRecommendationJpaRepository extends JpaRepository<AiRecommendationEntity, UUID> {

    List<AiRecommendationEntity> findByHomeIdOrderByCreatedAtDesc(UUID homeId);

    Optional<AiRecommendationEntity> findByTriggerEventId(UUID triggerEventId);
}
