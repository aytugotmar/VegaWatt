package com.vegawatt.sensors.simulation;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pure diurnal-shape math shared by every {@link ApplianceProfile}: smooth bumps for demand
 * curves (no visible steps on a chart) plus deterministic duty-cycle and session-window gating
 * derived only from the appliance id and the clock, so no state needs to be kept between
 * simulation ticks — a restart or a second appliance never desynchronizes the pattern.
 */
final class DiurnalCurve {

    /** Peak/trough times shift by up to this much per appliance, so two ACs don't peak at the
     * exact same minute. */
    private static final double MAX_CENTER_JITTER_HOURS = 0.75;

    /** Peak/trough depth scales by up to this fraction per appliance, so two ACs don't peak at
     * the exact same wattage. */
    private static final double MAX_AMPLITUDE_JITTER_FRACTION = 0.12;

    private DiurnalCurve() {
    }

    static boolean isWeekend(ZonedDateTime time) {
        DayOfWeek day = time.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    static double fractionalHour(ZonedDateTime time) {
        return time.getHour() + time.getMinute() / 60.0 + time.getSecond() / 3600.0;
    }

    /** Sum of every bump's contribution at {@code hour}, added to a 1.0 baseline, with a stable
     * per-appliance shift on every bump's center time and a stable per-appliance scale on every
     * bump's depth — this is the "personality" that keeps two ACs of the same type from peaking
     * at the exact same minute and wattage across different homes. Never lets the result fall to
     * zero or below, since a trough is a reduction in demand, not a power outage. */
    static double diurnalMultiplier(double hour, List<ApplianceProfile.Bump> bumps, UUID applianceId) {
        int hash = applianceId.hashCode();
        double centerShift = jitterUnit(hash) * MAX_CENTER_JITTER_HOURS;
        double amplitudeScale = 1.0 + jitterUnit(Integer.rotateLeft(hash, 16)) * MAX_AMPLITUDE_JITTER_FRACTION;

        double total = 1.0;
        for (ApplianceProfile.Bump bump : bumps) {
            double jitteredCenter = bump.centerHour() + centerShift;
            double jitteredMultiplier = 1.0 + (bump.multiplier() - 1.0) * amplitudeScale;
            total += bump(hour, jitteredCenter, bump.halfWidthHours(), jitteredMultiplier);
        }
        return Math.max(total, 0.05);
    }

    /** A stable value in [-1.0, 1.0) derived from {@code hash}; zero when {@code hash} is zero,
     * so a zero-hash appliance id is a clean "no personality jitter" baseline for tests. */
    private static double jitterUnit(int hash) {
        return (hash % 1000) / 1000.0;
    }

    /** Raised-cosine bump: contributes 0 at and beyond {@code halfWidthHours}, and
     * {@code multiplier - 1.0} exactly at {@code centerHour}, with a continuously smooth
     * (no-kink) transition in between — this is what keeps the curve step-free. */
    static double bump(double hour, double centerHour, double halfWidthHours, double multiplier) {
        double distance = circularHourDistance(hour, centerHour);
        if (distance >= halfWidthHours) {
            return 0.0;
        }
        double shape = 0.5 * (1 + Math.cos(Math.PI * distance / halfWidthHours));
        return (multiplier - 1.0) * shape;
    }

    private static double circularHourDistance(double hour, double centerHour) {
        double diff = Math.abs(hour - centerHour) % 24.0;
        return Math.min(diff, 24.0 - diff);
    }

    /** A stable per-appliance phase offset, so two identical appliances (same type, same
     * period) don't cycle in perfect lockstep across different homes. */
    private static long phaseSeconds(UUID applianceId, long periodSeconds) {
        return Math.floorMod(applianceId.hashCode(), periodSeconds);
    }

    static boolean dutyCycleOn(ZonedDateTime time, UUID applianceId, ApplianceProfile.DutyCycle dutyCycle) {
        long periodSeconds = dutyCycle.periodSeconds();
        long position = Math.floorMod(time.toEpochSecond() + phaseSeconds(applianceId, periodSeconds), periodSeconds);
        return position < (long) (periodSeconds * dutyCycle.onFraction());
    }

    /** Picks one deterministic session start per calendar day, inside the session's window, and
     * reports whether {@code time} falls inside that day's session. The start varies by
     * appliance and by day (via the epoch day), but is stable across ticks of the same day. */
    static boolean inDailySession(ZonedDateTime time, UUID applianceId, ApplianceProfile.Session session) {
        long daySeed = time.toLocalDate().toEpochDay() + applianceId.hashCode();
        double offsetFraction = Math.abs(daySeed % 1000) / 1000.0;
        double availableSpan = Math.max(session.windowEndHour() - session.windowStartHour() - session.durationHours(), 0);
        double sessionStart = session.windowStartHour() + offsetFraction * availableSpan;

        double hour = fractionalHour(time);
        return hour >= sessionStart && hour < sessionStart + session.durationHours();
    }
}
