package com.vegawatt.core.telemetry.domain;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedTelemetryEventRepository {

    boolean existsByEventId(UUID eventId);

    void markProcessed(UUID eventId, UUID homeId, UUID applianceId, Instant processedAt);
}
