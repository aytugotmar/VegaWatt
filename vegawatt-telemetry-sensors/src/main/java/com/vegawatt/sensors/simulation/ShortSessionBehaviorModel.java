package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Session-based devices used for a stretch of hours at a time, a few times a day at most —
 * humidifiers left running overnight or through a room session, projectors for an evening viewing.
 * Unlike {@link ShortHighPowerBehaviorModel} (minutes-long bursts), sessions here run for hours,
 * so both the daily cap and per-tick start probability are tuned much lower.
 */
@Component
public class ShortSessionBehaviorModel implements ApplianceBehaviorModel {

    private record UsageProfile(int dailyCap, double minSessionSeconds, double maxSessionSeconds,
                                 double startProbability, int windowStartHour, int windowEndHour) {
    }

    private static final UsageProfile DEFAULT_PROFILE = new UsageProfile(2, 3600, 10800, 0.001, 0, 24);

    private static UsageProfile profileFor(String catalogCode) {
        if (catalogCode == null) {
            return DEFAULT_PROFILE;
        }
        return switch (catalogCode) {
            case "HUMIDIFIER" -> new UsageProfile(3, 3600, 21600, 0.001, 0, 24);
            case "PROJECTOR" -> new UsageProfile(2, 3600, 14400, 0.003, 18, 24);
            default -> DEFAULT_PROFILE;
        };
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.SHORT_SESSION;
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

        BigDecimal min = config.simulationMinWatt() != null ? config.simulationMinWatt() : BigDecimal.ZERO;
        BigDecimal max = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : min;

        if (sessionStillActive) {
            nextState = ApplianceOperatingState.ACTIVE;
            stateStartedAt = previousState.stateStartedAt();
            expectedStateEndAt = previousState.expectedStateEndAt();
            sessionsToday = previousState.sessionsToday();
            powerWatt = TelemetryGenerator.randomInRange(min, max, random);
        } else {
            int hour = measuredAt.getHour();
            boolean inWindow = hour >= profile.windowStartHour() && hour < profile.windowEndHour();
            int todaysSessions = previousState == null ? 0 : previousState.sessionsTodayAt(measuredAt.toLocalDate());
            boolean startsNewSession = todaysSessions < profile.dailyCap() && inWindow
                    && random.nextDouble() < profile.startProbability();

            if (startsNewSession) {
                nextState = ApplianceOperatingState.ACTIVE;
                stateStartedAt = now;
                double sessionSeconds = profile.minSessionSeconds()
                        + random.nextDouble() * (profile.maxSessionSeconds() - profile.minSessionSeconds());
                expectedStateEndAt = now.plusSeconds(Math.round(sessionSeconds));
                sessionsToday = todaysSessions + 1;
                powerWatt = TelemetryGenerator.randomInRange(min, max, random);
            } else {
                nextState = ApplianceOperatingState.OFF;
                stateStartedAt = previousState == null ? now : previousState.stateStartedAt();
                expectedStateEndAt = null;
                sessionsToday = todaysSessions;
                powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
            }
        }

        ApplianceRuntimeState runtimeState = new ApplianceRuntimeState(
                nextState, nextState == ApplianceOperatingState.ACTIVE ? "RUNNING" : "OFF", stateStartedAt,
                expectedStateEndAt, null, null, null, now, null, null, null, sessionsToday, measuredAt.toLocalDate()
        );

        return new GeneratedReading(powerWatt, runtimeState);
    }
}
