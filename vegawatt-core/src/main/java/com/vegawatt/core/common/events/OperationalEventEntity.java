package com.vegawatt.core.common.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operational_events")
class OperationalEventEntity {

    @Id
    private UUID id;

    @Column(name = "home_id", nullable = false)
    private UUID homeId;

    @Column(name = "appliance_id")
    private UUID applianceId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    protected OperationalEventEntity() {
    }

    OperationalEventEntity(UUID id, UUID homeId, UUID applianceId, String eventType, Instant eventTime,
                            String details) {
        this.id = id;
        this.homeId = homeId;
        this.applianceId = applianceId;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.details = details;
    }

    UUID getId() {
        return id;
    }

    UUID getHomeId() {
        return homeId;
    }

    UUID getApplianceId() {
        return applianceId;
    }

    String getEventType() {
        return eventType;
    }

    Instant getEventTime() {
        return eventTime;
    }

    String getDetails() {
        return details;
    }
}
