package com.vegawatt.core.telemetry.infrastructure;

import com.vegawatt.core.home.domain.ApplianceOperatingState;
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
        ApplianceOperatingState operatingState,
        String operatingMode,
        int measurementIntervalSeconds) {
}
