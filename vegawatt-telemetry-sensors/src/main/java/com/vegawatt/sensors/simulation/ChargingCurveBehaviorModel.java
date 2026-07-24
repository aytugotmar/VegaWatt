package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Battery-charged devices (laptops, phones, tablets): a charge session starts at higher (bulk)
 * power and tapers down toward a trickle as the session progresses — a real charge curve, not a
 * flat draw for the whole plugged-in period. Session starts are scheduled via
 * {@link DiurnalCurve#plannedSessionStartHour} — a deterministic, per-day, spread-out plan —
 * instead of a flat per-tick probability.
 */
@Component
public class ChargingCurveBehaviorModel implements ApplianceBehaviorModel {

    private record UsageProfile(int minCount, int maxCount, double minSessionSeconds,
                                 double maxSessionSeconds, double windowStartHour, double windowEndHour) {
    }

    private static final UsageProfile DEFAULT_PROFILE = new UsageProfile(1, 2, 1800, 7200, 0, 24);

    private static UsageProfile profileFor(String catalogCode) {
        if (catalogCode == null) {
            return DEFAULT_PROFILE;
        }
        return switch (catalogCode) {
            case "LAPTOP" -> new UsageProfile(1, 2, 3600, 10800, 8, 24); // daytime/evening use
            case "PHONE_CHARGER" -> new UsageProfile(1, 2, 3600, 21600, 0, 24); // overnight-capable
            case "TABLET_CHARGER" -> new UsageProfile(1, 2, 3600, 14400, 0, 24); // overnight-capable
            default -> DEFAULT_PROFILE;
        };
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.CHARGING_CURVE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        UsageProfile profile = profileFor(config.catalogCode());

        boolean sessionStillActive = previousState != null
                && previousState.operatingState() == ApplianceOperatingState.ACTIVE
                && previousState.expectedStateEndAt() != null && now.isBefore(previousState.expectedStateEndAt());

        ApplianceOperatingState nextState;
        BigDecimal powerWatt;
        Instant stateStartedAt;
        Instant expectedStateEndAt;
        int sessionsToday;

        BigDecimal max = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
        BigDecimal trickle = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;

        if (sessionStillActive) {
            nextState = ApplianceOperatingState.ACTIVE;
            stateStartedAt = previousState.stateStartedAt();
            expectedStateEndAt = previousState.expectedStateEndAt();
            sessionsToday = previousState.sessionsToday();
            powerWatt = chargeCurvePower(stateStartedAt, expectedStateEndAt, now, max, trickle);
        } else {
            int todaysSessions = previousState == null ? 0 : previousState.sessionsTodayAt(measuredAt.toLocalDate());
            double plannedStart = DiurnalCurve.plannedSessionStartHour(measuredAt, config.applianceId(),
                    todaysSessions, profile.windowStartHour(), profile.windowEndHour(), profile.minCount(),
                    profile.maxCount());
            boolean startsNewSession = !Double.isNaN(plannedStart)
                    && DiurnalCurve.fractionalHour(measuredAt) >= plannedStart;

            if (startsNewSession) {
                nextState = ApplianceOperatingState.ACTIVE;
                stateStartedAt = now;
                double sessionSeconds = profile.minSessionSeconds()
                        + random.nextDouble() * (profile.maxSessionSeconds() - profile.minSessionSeconds());
                expectedStateEndAt = now.plusSeconds(Math.round(sessionSeconds));
                sessionsToday = todaysSessions + 1;
                powerWatt = max; // bulk-charge power at the very start of the session
            } else {
                nextState = ApplianceOperatingState.STANDBY;
                stateStartedAt = previousState == null ? now : previousState.stateStartedAt();
                expectedStateEndAt = null;
                sessionsToday = todaysSessions;
                powerWatt = trickle;
            }
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                nextState, nextState == ApplianceOperatingState.ACTIVE ? "CHARGING" : "STANDBY", stateStartedAt,
                expectedStateEndAt, null, null, null, now, null, null, null, sessionsToday, measuredAt.toLocalDate()
        );

        return new GeneratedReading(powerWatt, nextRuntimeState);
    }

    /** Linear taper from bulk ({@code max}) power at session start down to a near-{@code trickle}
     * finishing power as the session approaches {@code expectedEnd} — approximates a real
     * constant-current/constant-voltage charge curve closely enough for simulation purposes. */
    private static BigDecimal chargeCurvePower(Instant sessionStart, Instant expectedEnd, Instant now, BigDecimal max,
                                                BigDecimal trickle) {
        long totalSeconds = Duration.between(sessionStart, expectedEnd).getSeconds();
        long elapsedSeconds = Duration.between(sessionStart, now).getSeconds();
        double ratio = totalSeconds <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, (double) elapsedSeconds / totalSeconds));
        BigDecimal range = max.subtract(trickle);
        return max.subtract(range.multiply(BigDecimal.valueOf(ratio)));
    }
}
