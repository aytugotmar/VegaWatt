package com.vegawatt.sensors.simulation;

import java.time.DayOfWeek;
import java.time.Duration;
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

    /** Deterministically plans between {@code minCount} and {@code maxCount} sessions for the
     * given calendar day (varies by day and appliance, stable across every tick of that day),
     * spread across {@code [windowStartHour, windowEndHour)} with per-slot jitter so they don't
     * clump right at window open — generalizes {@link #inDailySession} from one session/day to N.
     * Returns the planned start hour for the session at {@code sessionIndex} (0-based), or
     * {@link Double#NaN} if the day's plan has fewer than {@code sessionIndex + 1} sessions. */
    static double plannedSessionStartHour(ZonedDateTime time, UUID applianceId, int sessionIndex,
                                           double windowStartHour, double windowEndHour, int minCount,
                                           int maxCount) {
        long daySeed = time.toLocalDate().toEpochDay() * 1_000_003L + applianceId.hashCode();
        int range = Math.max(maxCount - minCount + 1, 1);
        int plannedCount = minCount + (int) Math.floorMod(daySeed, range);
        if (sessionIndex >= plannedCount) {
            return Double.NaN;
        }

        double span = Math.max(windowEndHour - windowStartHour, 0.1);
        double slotWidth = span / plannedCount;
        long slotSeed = daySeed * 1_000_003L + sessionIndex;
        double jitterFraction = Math.abs(slotSeed % 1000) / 1000.0;
        return windowStartHour + slotWidth * sessionIndex + jitterFraction * slotWidth * 0.9;
    }

    /** Deterministic, stateless demand-intensity curve in [0,1] — no persisted temperature state,
     * just a pure function of {@code (time, applianceId)} — used to modulate AC/heater cycle
     * timing and gate real OFF excursions (§5.6). Returns 1 exactly at a {@code peakHours} entry,
     * tapering to 0 at {@code halfWidthHours} away via the same raised-cosine shape as
     * {@link #bump}, with a stable per-appliance jitter on the peak times. Pass more than one peak
     * (e.g. a room heater's morning and evening peaks) and the highest of their individual curves
     * wins at any given hour. */
    static double demandIntensity(ZonedDateTime time, UUID applianceId, double halfWidthHours, double... peakHours) {
        int hash = applianceId.hashCode();
        double centerShift = jitterUnit(hash) * MAX_CENTER_JITTER_HOURS;
        double hour = fractionalHour(time);

        double best = 0.0;
        for (double peakHour : peakHours) {
            double distance = circularHourDistance(hour, peakHour + centerShift);
            double value = distance >= halfWidthHours ? 0.0 : 0.5 * (1 + Math.cos(Math.PI * distance / halfWidthHours));
            best = Math.max(best, value);
        }
        return best;
    }

    private static final double[] NOISE_BASE_PERIOD_SECONDS = {47, 113, 281};
    private static final double[] NOISE_WEIGHTS = {0.5, 0.3, 0.2};

    /** Deterministic, smoothly-varying noise in [0,1] as a function of elapsed time since a dwell
     * period started and the appliance id (§5.7) — a weighted sum of a few slow, per-appliance
     * jittered sinusoids, so an active device's power ramps gradually instead of teleporting
     * between extremes every tick. Purely a function of {@code (elapsed, applianceId)}, so it
     * needs zero persisted state beyond the dwell-start timestamp every model already tracks. */
    static double smoothedNoise01(Duration elapsed, UUID applianceId) {
        int hash = applianceId.hashCode();
        double seconds = elapsed.getSeconds();

        double total = 0.0;
        double weightSum = 0.0;
        for (int i = 0; i < NOISE_BASE_PERIOD_SECONDS.length; i++) {
            double periodJitter = 1.0 + jitterUnit(Integer.rotateLeft(hash, i * 11)) * 0.4;
            double phase = jitterUnit(Integer.rotateLeft(hash, i * 17 + 5)) * Math.PI * 2;
            double period = NOISE_BASE_PERIOD_SECONDS[i] * periodJitter;
            total += NOISE_WEIGHTS[i] * Math.sin(2 * Math.PI * seconds / period + phase);
            weightSum += NOISE_WEIGHTS[i];
        }
        double normalized = total / weightSum; // in [-1, 1]
        return (normalized + 1.0) / 2.0; // map to [0, 1]
    }
}
