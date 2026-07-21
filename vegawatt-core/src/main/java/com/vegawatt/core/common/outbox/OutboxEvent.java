package com.vegawatt.core.common.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        Instant createdAt,
        Instant publishedAt,
        int retryCount) {

    public static OutboxEvent create(String aggregateType, UUID aggregateId, String eventType, String payload,
                                      Instant createdAt) {
        return new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId, eventType, payload, createdAt, null, 0);
    }

    public OutboxEvent published(Instant publishedAt) {
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, createdAt, publishedAt,
                retryCount);
    }

    public OutboxEvent retryFailed() {
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, createdAt, publishedAt,
                retryCount + 1);
    }

    public boolean isPublished() {
        return publishedAt != null;
    }
}
