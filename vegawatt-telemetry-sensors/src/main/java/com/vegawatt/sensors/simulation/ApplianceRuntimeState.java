package com.vegawatt.sensors.simulation;

import java.time.Instant;

/**
 * Per-appliance simulation state, held in {@link ApplianceRuntimeStateStore} between ticks. The
 * fault-related fields are declared now (matching yapılacak.md §16) but stay {@code null} until
 * the fault episode phase actually populates them — this avoids a breaking record shape change
 * later.
 */
public record ApplianceRuntimeState(
        ApplianceOperatingState operatingState,
        String operatingMode,
        Instant stateStartedAt,
        Instant expectedStateEndAt,
        String activeFaultCode,
        Instant faultStartedAt,
        Instant faultExpectedEndAt,
        Instant previousMeasurementAt,
        String sessionId) {
}
