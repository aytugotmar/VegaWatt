package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Short duration high-power devices like kettles, microwaves, coffee makers, toasters, and the
 * vacuum cleaner. Cycles between OFF (standby) and ACTIVE (high power) for a randomized session
 * duration. Session starts are scheduled via {@link DiurnalCurve#plannedSessionStartHour} — a
 * deterministic, per-day, spread-out plan — instead of a flat per-tick probability, so sessions
 * don't clump right at window-open and instead land at realistic, varied times across the day.
 */
@Component
public class ShortHighPowerBehaviorModel implements ApplianceBehaviorModel {

    private record UsageProfile(int minCount, int maxCount, double minSessionSeconds,
                                 double maxSessionSeconds, double windowStartHour, double windowEndHour,
                                 String activeMode) {
    }

    private static final UsageProfile DEFAULT_PROFILE = new UsageProfile(1, 5, 60, 300, 0, 24, "RUNNING");

    private static UsageProfile profileFor(String catalogCode) {
        if (catalogCode == null) {
            return DEFAULT_PROFILE;
        }
        return switch (catalogCode) {
            case "KETTLE" -> new UsageProfile(3, 8, 60, 180, 6, 23, "BOILING");
            case "MICROWAVE" -> new UsageProfile(1, 5, 30, 300, 6, 23, "MICROWAVING");
            case "TOASTER" -> new UsageProfile(0, 3, 180, 600, 6, 10, "TOASTING");
            case "COFFEE_MACHINE" -> new UsageProfile(2, 6, 60, 240, 6, 16, "BREWING");
            case "VACUUM_CLEANER" -> new UsageProfile(0, 2, 300, 1200, 8, 20, "CLEANING");
            default -> DEFAULT_PROFILE;
        };
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.SHORT_HIGH_POWER;
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

        if (sessionStillActive) {
            nextState = ApplianceOperatingState.ACTIVE;
            stateStartedAt = previousState.stateStartedAt();
            expectedStateEndAt = previousState.expectedStateEndAt();
            sessionsToday = previousState.sessionsToday();
            BigDecimal base = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
            double jitter = 0.96 + random.nextDouble() * 0.08;
            powerWatt = base.multiply(BigDecimal.valueOf(jitter));
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
                BigDecimal base = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
                double jitter = 0.96 + random.nextDouble() * 0.08;
                powerWatt = base.multiply(BigDecimal.valueOf(jitter));
            } else {
                nextState = ApplianceOperatingState.OFF;
                stateStartedAt = previousState == null ? now : previousState.stateStartedAt();
                expectedStateEndAt = null;
                sessionsToday = todaysSessions;
                powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
            }
        }

        ApplianceRuntimeState runtimeState = new ApplianceRuntimeState(
                nextState, nextState == ApplianceOperatingState.ACTIVE ? profile.activeMode() : "STANDBY",
                stateStartedAt, expectedStateEndAt, null, null, null, now, null, null, null, sessionsToday,
                measuredAt.toLocalDate()
        );

        return new GeneratedReading(powerWatt, runtimeState);
    }
}
