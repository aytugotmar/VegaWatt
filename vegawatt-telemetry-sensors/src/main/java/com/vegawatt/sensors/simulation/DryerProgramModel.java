package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Dryer: HEATING -> TUMBLING -> COOLING -> ANTI_CREASE -> OFF — a dryer never "washes" or
 * "rinses" (the shared model's washing-machine stages leaking into a device that has no water
 * cycle at all). Realistic ~65 minute total cycle: a short initial heat-up, a long sustained
 * tumble-dry, a cool-down before the door is safe to open, and a brief anti-crease tumble.
 */
final class DryerProgramModel {

    private static final int DAILY_PROGRAM_CAP = 2;
    private static final double WINDOW_START_HOUR = 7;
    private static final double WINDOW_END_HOUR = 22;

    private static final long HEATING_SECONDS = 300; // 5 min
    private static final long TUMBLING_SECONDS = 2700; // 45 min
    private static final long COOLING_SECONDS = 600; // 10 min
    private static final long ANTI_CREASE_SECONDS = 300; // 5 min

    private DryerProgramModel() {
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
            nextMode = startsNewCycle ? "HEATING" : "OFF";
            if (startsNewCycle) {
                sessionsToday = sessionsToday + 1;
            }
        } else if ("HEATING".equals(mode)) {
            nextMode = elapsedInStage > HEATING_SECONDS ? "TUMBLING" : "HEATING";
        } else if ("TUMBLING".equals(mode)) {
            nextMode = elapsedInStage > TUMBLING_SECONDS ? "COOLING" : "TUMBLING";
        } else if ("COOLING".equals(mode)) {
            nextMode = elapsedInStage > COOLING_SECONDS ? "ANTI_CREASE" : "COOLING";
        } else if ("ANTI_CREASE".equals(mode)) {
            nextMode = elapsedInStage > ANTI_CREASE_SECONDS ? "OFF" : "ANTI_CREASE";
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
            case "HEATING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = maxP;
            }
            case "TUMBLING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = maxP.multiply(new BigDecimal("0.85"));
            }
            case "COOLING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minP.multiply(new BigDecimal("1.2")); // motor only, no heat
            }
            case "ANTI_CREASE" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minP.multiply(new BigDecimal("0.5")); // brief, intermittent tumble
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
