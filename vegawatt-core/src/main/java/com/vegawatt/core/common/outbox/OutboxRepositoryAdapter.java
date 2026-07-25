package com.vegawatt.core.common.outbox;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class OutboxRepositoryAdapter implements OutboxRepository {

    private final OutboxEventJpaRepository jpaRepository;

    OutboxRepositoryAdapter(OutboxEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity saved = jpaRepository.save(toEntity(event));
        return toDomain(saved);
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        return jpaRepository.findByPublishedAtIsNullAndDeadLetteredFalseOrderByCreatedAtAsc(PageRequest.of(0, limit))
                .stream()
                .map(OutboxRepositoryAdapter::toDomain)
                .toList();
    }

    private static OutboxEventEntity toEntity(OutboxEvent event) {
        return new OutboxEventEntity(event.id(), event.aggregateType(), event.aggregateId(), event.eventType(),
                event.payload(), event.createdAt(), event.publishedAt(), event.retryCount(), event.deadLettered());
    }

    private static OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(entity.getId(), entity.getAggregateType(), entity.getAggregateId(),
                entity.getEventType(), entity.getPayload(), entity.getCreatedAt(), entity.getPublishedAt(),
                entity.getRetryCount(), entity.isDeadLettered());
    }
}
