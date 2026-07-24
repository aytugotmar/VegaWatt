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
 * flat draw for the whole plugged-in period. Capped at a realistic number of charge sessions per
 * day.
 */
@Component
public class ChargingCurveBehaviorModel implements ApplianceBehaviorModel {

    private record UsageProfile(int dailyCap, double minSessionSeconds, double maxSessionSeconds,
                                 double startProbability) {
    }

    private static final UsageProfile DEFAULT_PROFILE = new UsageProfile(2, 1800, 7200, 0.003);

    private static UsageProfile profileFor(String catalogCode) {
        if (catalogCode == null) {
            return DEFAULT_PROFILE;
        }
        return switch (catalogCode) {
            case "LAPTOP" -> new UsageProfile(2, 3600, 10800, 0.003); // 1-3h charge sessions
            case "PHONE_CHARGER" -> new UsageProfile(2, 3600, 21600, 0.002); // overnight-capable
            case "TABLET_CHARGER" -> new UsageProfile(1, 3600, 14400, 0.0015);
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
            boolean startsNewSession = todaysSessions < profile.dailyCap()
                    && random.nextDouble() < profile.startProbability();

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
