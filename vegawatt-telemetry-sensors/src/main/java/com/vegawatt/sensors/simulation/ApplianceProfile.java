package com.vegawatt.sensors.simulation;

import java.util.List;

/**
 * Describes how one appliance type behaves over a day: a demand curve made of smooth "bumps"
 * (see {@link DiurnalCurve#bump}), an optional thermostat-style duty cycle (fridge, AC, water
 * heater — on/off compressor rather than continuous draw), and an optional daily usage session
 * (washing machine, dishwasher, oven — used once or twice a day, near-zero otherwise). At most
 * one of {@code dutyCycle} and {@code session} applies; an appliance is never both.
 */
record ApplianceProfile(
        List<Bump> bumps,
        List<Bump> weekendBumps,
        DutyCycle dutyCycle,
        Session weekdaySession,
        Session weekendSession) {

    static ApplianceProfile bumpOnly(List<Bump> bumps) {
        return new ApplianceProfile(bumps, List.of(), null, null, null);
    }

    static ApplianceProfile bumpOnly(List<Bump> bumps, List<Bump> weekendBumps) {
        return new ApplianceProfile(bumps, weekendBumps, null, null, null);
    }

    static ApplianceProfile dutyCycled(List<Bump> bumps, DutyCycle dutyCycle) {
        return new ApplianceProfile(bumps, List.of(), dutyCycle, null, null);
    }

    static ApplianceProfile sessionBased(Session weekdaySession, Session weekendSession) {
        return new ApplianceProfile(List.of(), List.of(), null, weekdaySession, weekendSession);
    }

    /** A raised-cosine demand bump centered on {@code centerHour}, tapering to baseline over
     * {@code halfWidthHours} in each direction. {@code multiplier} above 1.0 is a peak, below
     * 1.0 is a trough (e.g. the overnight low). */
    record Bump(double centerHour, double halfWidthHours, double multiplier) {
    }

    /** A thermostat-style on/off cycle: on for {@code onFraction} of every {@code periodSeconds},
     * drawing the full simulation range while on and {@code standbyFraction} of it while off. */
    record DutyCycle(long periodSeconds, double onFraction, double standbyFraction) {
    }

    /** One usage window per calendar day: a session of {@code durationHours} starts at a
     * per-appliance, per-day deterministic offset inside [{@code windowStartHour},
     * {@code windowEndHour}), drawing the full simulation range; {@code standbyFraction} of it
     * applies outside the session. */
    record Session(double windowStartHour, double windowEndHour, double durationHours, double standbyFraction) {
    }
}
