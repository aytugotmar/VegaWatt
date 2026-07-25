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
        int retryCount,
        boolean deadLettered) {

    public static OutboxEvent create(String aggregateType, UUID aggregateId, String eventType, String payload,
                                      Instant createdAt) {
        return new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId, eventType, payload, createdAt, null, 0,
                false);
    }

    public OutboxEvent published(Instant publishedAt) {
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, createdAt, publishedAt,
                retryCount, deadLettered);
    }

    public OutboxEvent retryFailed() {
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, createdAt, publishedAt,
                retryCount + 1, deadLettered);
    }

    /** Stops the relay from retrying this event forever after it has exhausted its retry budget. */
    public OutboxEvent deadLetter() {
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, createdAt, publishedAt,
                retryCount, true);
    }

    public boolean isPublished() {
        return publishedAt != null;
    }
}
