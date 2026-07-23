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
        String sessionId,
        /** Next time an every-tick-unbound fault model (currently only
         * {@code AlwaysOnStableBehaviorModel}) is allowed to roll the dice on starting a new
         * fault. {@code null} means "not using time-windowed evaluation" (e.g. models that gate
         * fault rolls on their own state-window boundaries instead). */
        Instant nextFaultEvaluationAt,
        /** While non-null and in the future, a fault that just ended blocks a new one from
         * starting, even if {@link #nextFaultEvaluationAt} says an evaluation is due. */
        Instant faultCooldownUntil) {

    public static ApplianceRuntimeState initial(Instant now) {
        return new ApplianceRuntimeState(ApplianceOperatingState.OFF, "STANDBY", now, null, null, null, null, now, null, null, null);
    }
}
