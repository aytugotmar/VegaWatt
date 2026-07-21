package com.vegawatt.core.telemetry.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryReading(
        UUID eventId,
        UUID homeId,
        UUID applianceId,
        BigDecimal powerWatt,
        int measurementIntervalSeconds,
        Instant occurredAt) {

    public TelemetryReading {
        if (powerWatt.signum() < 0) {
            throw new InvalidTelemetryReadingException("powerWatt must not be negative");
        }
        if (measurementIntervalSeconds <= 0) {
            throw new InvalidTelemetryReadingException("measurementIntervalSeconds must be positive");
        }
    }
}
