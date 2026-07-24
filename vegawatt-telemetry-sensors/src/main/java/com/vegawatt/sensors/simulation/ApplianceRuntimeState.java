package com.vegawatt.sensors.simulation;

import java.time.Instant;
import java.time.LocalDate;

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
        Instant faultCooldownUntil,
        /** How many sessions/programs/activations this appliance has started on
         * {@link #sessionsCountedOnDate}. Read via {@link #sessionsTodayAt(LocalDate)}, which
         * auto-rolls over to 0 once the simulation date advances — there's no separate reset step. */
        int sessionsToday,
        LocalDate sessionsCountedOnDate) {

    public static ApplianceRuntimeState initial(Instant now) {
        return new ApplianceRuntimeState(ApplianceOperatingState.OFF, "STANDBY", now, null, null, null, null, now, null,
                null, null, 0, null);
    }

    /** The effective daily session count as of {@code today} — 0 if the last counted session was
     * on an earlier date (day rollover), otherwise {@link #sessionsToday}. */
    public int sessionsTodayAt(LocalDate today) {
        return today.equals(sessionsCountedOnDate) ? sessionsToday : 0;
    }
}
