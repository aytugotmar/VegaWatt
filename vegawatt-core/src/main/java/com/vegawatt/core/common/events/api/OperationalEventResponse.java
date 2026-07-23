package com.vegawatt.core.common.events.api;

import com.vegawatt.core.common.events.OperationalEvent;
import java.time.Instant;
import java.util.UUID;

public record OperationalEventResponse(
        UUID id,
        UUID applianceId,
        String eventType,
        Instant eventTime,
        String details) {

    public static OperationalEventResponse from(OperationalEvent event) {
        return new OperationalEventResponse(event.id(), event.applianceId(), event.eventType().name(),
                event.eventTime(), event.details());
    }
}
