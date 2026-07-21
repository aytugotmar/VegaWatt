package com.vegawatt.sensors.publishing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record TelemetryEventPayload(
        UUID eventId,
        int eventVersion,
        Instant occurredAt,
        UUID homeId,
        UUID applianceId,
        BigDecimal powerWatt,
        int measurementIntervalSeconds) {
}
