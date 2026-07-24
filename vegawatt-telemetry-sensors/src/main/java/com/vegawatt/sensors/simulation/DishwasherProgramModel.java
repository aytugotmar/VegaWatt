package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Dishwasher: PRE_RINSE -> WASHING -> HEATING -> RINSING -> DRYING -> OFF — a dishwasher never
 * "spins" (that was the shared model's washing-machine stage leaking into a device it doesn't
 * apply to). Realistic ~105 minute total cycle, with a distinct HEATING stage for the
 * water-heater element (the dishwasher's single highest-power moment).
 */
final class DishwasherProgramModel {

    private static final int DAILY_PROGRAM_CAP = 2;
    private static final double WINDOW_START_HOUR = 7;
    private static final double WINDOW_END_HOUR = 22;

    private static final long PRE_RINSE_SECONDS = 600; // 10 min
    private static final long WASHING_SECONDS = 2400; // 40 min
    private static final long HEATING_SECONDS = 600; // 10 min
    private static final long RINSING_SECONDS = 900; // 15 min
    private static final long DRYING_SECONDS = 1800; // 30 min

    private DishwasherProgramModel() {
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
                    sessionsToday, WINDOW_START_HOUR, WINDOW_END_HOUR, 0, DAILY_PROGRAM_CAP);
            boolean startsNewCycle = !Double.isNaN(plannedStart)
                    && DiurnalCurve.fractionalHour(measuredAt) >= plannedStart;
            nextMode = startsNewCycle ? "PRE_RINSE" : "OFF";
            if (startsNewCycle) {
                sessionsToday = sessionsToday + 1;
            }
        } else if ("PRE_RINSE".equals(mode)) {
            nextMode = elapsedInStage > PRE_RINSE_SECONDS ? "WASHING" : "PRE_RINSE";
        } else if ("WASHING".equals(mode)) {
            nextMode = elapsedInStage > WASHING_SECONDS ? "HEATING" : "WASHING";
        } else if ("HEATING".equals(mode)) {
            nextMode = elapsedInStage > HEATING_SECONDS ? "RINSING" : "HEATING";
        } else if ("RINSING".equals(mode)) {
            nextMode = elapsedInStage > RINSING_SECONDS ? "DRYING" : "RINSING";
        } else if ("DRYING".equals(mode)) {
            nextMode = elapsedInStage > DRYING_SECONDS ? "OFF" : "DRYING";
        } else {
            nextMode = "OFF";
        }
        if (!nextMode.equals(mode)) {
            nextStageStartedAt = now;
        }

        BigDecimal maxP = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
        BigDecimal minP = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("50");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        switch (nextMode) {
            case "PRE_RINSE" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minP.multiply(new BigDecimal("0.5")); // pump only, no heating
            }
            case "WASHING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minP.multiply(new BigDecimal("1.5"));
            }
            case "HEATING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = maxP; // the dishwasher's single highest-power moment
            }
            case "RINSING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minP.multiply(new BigDecimal("1.5"));
            }
            case "DRYING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minP.multiply(new BigDecimal("0.8"));
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
