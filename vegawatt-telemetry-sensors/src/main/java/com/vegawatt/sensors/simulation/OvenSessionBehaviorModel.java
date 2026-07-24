package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Oven: PREHEATING -> BAKING -> KEEP_WARM -> OFF. Session starts are scheduled via
 * {@link DiurnalCurve#plannedSessionStartHour} (0-3/day, 7-21h) instead of a flat per-tick
 * probability, so bakes land at deterministic, spread-out times across the day.
 */
final class OvenSessionBehaviorModel {

    private static final int DAILY_SESSION_CAP = 3;
    private static final double WINDOW_START_HOUR = 7;
    private static final double WINDOW_END_HOUR = 21;

    private static final long PREHEATING_SECONDS = 300; // 5 min
    private static final long BAKING_SECONDS = 1800; // 30 min
    private static final long KEEP_WARM_SECONDS = 600; // 10 min

    private OvenSessionBehaviorModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config,
                                                             ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();
        LocalDate today = measuredAt.toLocalDate();

        String mode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "OFF";
        Instant stageStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInStage = Duration.between(stageStartedAt, now).getSeconds();
        int sessionsToday = previousState == null ? 0 : previousState.sessionsTodayAt(today);

        String nextMode;
        Instant nextStageStartedAt = stageStartedAt;

        if ("OFF".equals(mode)) {
            double plannedStart = DiurnalCurve.plannedSessionStartHour(measuredAt, config.applianceId(),
                    sessionsToday, WINDOW_START_HOUR, WINDOW_END_HOUR, 0, DAILY_SESSION_CAP);
            boolean startsSession = !Double.isNaN(plannedStart)
                    && DiurnalCurve.fractionalHour(measuredAt) >= plannedStart;
            nextMode = startsSession ? "PREHEATING" : "OFF";
            if (startsSession) {
                sessionsToday = sessionsToday + 1;
            }
        } else if ("PREHEATING".equals(mode)) {
            nextMode = elapsedInStage > PREHEATING_SECONDS ? "BAKING" : "PREHEATING";
        } else if ("BAKING".equals(mode)) {
            nextMode = elapsedInStage > BAKING_SECONDS ? "KEEP_WARM" : "BAKING";
        } else if ("KEEP_WARM".equals(mode)) {
            nextMode = elapsedInStage > KEEP_WARM_SECONDS ? "OFF" : "KEEP_WARM";
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

        return new ApplianceBehaviorModel.GeneratedReading(powerWatt, nextRuntimeState);
    }
}
