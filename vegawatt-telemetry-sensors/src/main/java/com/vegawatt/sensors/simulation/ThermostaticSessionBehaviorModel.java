package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Thermostatic session model for ovens and irons.
 * PREHEATING -> BAKING -> KEEP_WARM -> OFF, capped at a realistic number of sessions per day and
 * gated to daytime hours. Power and operating state are derived from the post-transition
 * {@code nextMode}, avoiding the one-tick ACTIVE/OFF-mode contradiction the naive version had.
 */
@Component
public class ThermostaticSessionBehaviorModel implements ApplianceBehaviorModel {

    private record UsageProfile(int dailyCap, double startProbability) {
    }

    private static final UsageProfile DEFAULT_PROFILE = new UsageProfile(3, 0.02);
    private static final int WINDOW_START_HOUR = 6;
    private static final int WINDOW_END_HOUR = 22;

    private static UsageProfile profileFor(String catalogCode) {
        if (catalogCode == null) {
            return DEFAULT_PROFILE;
        }
        return switch (catalogCode) {
            case "OVEN" -> new UsageProfile(3, 0.02);
            case "IRON" -> new UsageProfile(2, 0.015);
            default -> DEFAULT_PROFILE;
        };
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.THERMOSTATIC_SESSION;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        LocalDate today = measuredAt.toLocalDate();
        UsageProfile profile = profileFor(config.catalogCode());

        String mode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "OFF";
        Instant stageStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInMode = Duration.between(stageStartedAt, now).getSeconds();
        int sessionsToday = previousState == null ? 0 : previousState.sessionsTodayAt(today);

        String nextMode;
        Instant nextStageStartedAt = stageStartedAt;

        if ("OFF".equals(mode)) {
            int hour = measuredAt.getHour();
            boolean inWindow = hour >= WINDOW_START_HOUR && hour < WINDOW_END_HOUR;
            boolean startsSession = sessionsToday < profile.dailyCap() && inWindow
                    && random.nextDouble() < profile.startProbability();
            nextMode = startsSession ? "PREHEATING" : "OFF";
            if (startsSession) {
                sessionsToday = sessionsToday + 1;
            }
        } else if ("PREHEATING".equals(mode)) {
            nextMode = elapsedInMode > 300 ? "BAKING" : "PREHEATING"; // 5 min preheat
        } else if ("BAKING".equals(mode)) {
            nextMode = elapsedInMode > 1800 ? "KEEP_WARM" : "BAKING"; // 30 min baking
        } else if ("KEEP_WARM".equals(mode)) {
            nextMode = elapsedInMode > 600 ? "OFF" : "KEEP_WARM";
        } else {
            nextMode = "OFF";
        }
        if (!nextMode.equals(mode)) {
            nextStageStartedAt = now;
        }

        BigDecimal maxPower = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
        long elapsedInNextStage = Duration.between(nextStageStartedAt, now).getSeconds();

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        switch (nextMode) {
            case "PREHEATING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = maxPower.multiply(new BigDecimal("0.98"));
            }
            case "BAKING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                boolean pulsingOn = (elapsedInNextStage % 60) < 35;
                powerWatt = pulsingOn ? maxPower : config.simulationMinWatt();
            }
            case "KEEP_WARM" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                boolean pulsingOn = (elapsedInNextStage % 120) < 20;
                powerWatt = pulsingOn ? maxPower.multiply(new BigDecimal("0.5")) : config.simulationMinWatt();
            }
            default -> {
                operatingState = ApplianceOperatingState.OFF;
                powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
            }
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, nextStageStartedAt, null, null, null, null, now, null, null, null,
                sessionsToday, today
        );

        return new GeneratedReading(powerWatt, nextRuntimeState);
    }
}
