package com.vegawatt.core.telemetry.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_telemetry_events")
class ProcessedTelemetryEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(name = "appliance_id", nullable = false)
    private UUID applianceId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedTelemetryEventEntity() {
    }

    ProcessedTelemetryEventEntity(UUID eventId, UUID homeId, UUID applianceId, Instant processedAt) {
        this.eventId = eventId;
        this.homeId = homeId;
        this.applianceId = applianceId;
        this.processedAt = processedAt;
    }
}
