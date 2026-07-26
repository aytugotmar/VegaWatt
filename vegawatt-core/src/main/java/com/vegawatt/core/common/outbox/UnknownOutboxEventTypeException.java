package com.vegawatt.core.common.outbox;

/**
 * An outbox row's {@code eventType} doesn't match any known topic-resolution case. Unlike a Kafka
 * send failure, this is never transient — retrying resolves nothing — so {@link OutboxRelayScheduler}
 * dead-letters immediately instead of retrying forever.
 */
class UnknownOutboxEventTypeException extends RuntimeException {

    UnknownOutboxEventTypeException(String eventType) {
        super("Unknown outbox event type: " + eventType);
    }
}
