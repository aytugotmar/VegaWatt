package com.vegawatt.core.common.events;

import java.time.Instant;
import java.util.UUID;

public record OperationalEvent(
        UUID id,
        UUID homeId,
        UUID applianceId,
        OperationalEventType eventType,
        Instant eventTime,
        String details) {

    public static OperationalEvent create(UUID homeId, UUID applianceId, OperationalEventType eventType,
                                           Instant eventTime, String details) {
        return new OperationalEvent(UUID.randomUUID(), homeId, applianceId, eventType, eventTime, details);
    }
}
