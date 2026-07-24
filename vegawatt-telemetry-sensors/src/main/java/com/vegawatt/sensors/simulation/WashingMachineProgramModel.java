package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Washing machine: WASHING -> RINSING -> SPINNING -> OFF, re-timed to a realistic ~70 minute total
 * cycle (a real wash program is 60-180 minutes; the original one-size-fits-all timing ran a full
 * cycle in ~27 minutes). Session starts are scheduled via {@link DiurnalCurve#plannedSessionStartHour}
 * instead of a per-tick coin flip, so programs land at deterministic, spread-out times across the
 * day instead of clumping right when the daytime window opens.
 */
final class WashingMachineProgramModel {

    private static final int DAILY_PROGRAM_CAP = 2;
    private static final double WINDOW_START_HOUR = 7;
    private static final double WINDOW_END_HOUR = 22;

    private static final long WASHING_SECONDS = 2400; // 40 min
    private static final long RINSING_SECONDS = 1080; // 18 min
    private static final long SPINNING_SECONDS = 750; // 12.5 min

    private WashingMachineProgramModel() {
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
            nextMode = startsNewCycle ? "WASHING" : "OFF";
            if (startsNewCycle) {
                sessionsToday = sessionsToday + 1;
            }
        } else if ("WASHING".equals(mode)) {
            nextMode = elapsedInStage > WASHING_SECONDS ? "RINSING" : "WASHING";
        } else if ("RINSING".equals(mode)) {
            nextMode = elapsedInStage > RINSING_SECONDS ? "SPINNING" : "RINSING";
        } else if ("SPINNING".equals(mode)) {
            nextMode = elapsedInStage > SPINNING_SECONDS ? "OFF" : "SPINNING";
        } else {
            nextMode = "OFF";
        }
        if (!nextMode.equals(mode)) {
            nextStageStartedAt = now;
        }

        BigDecimal maxP = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
        BigDecimal minP = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("50");
        long elapsedInNextStage = Duration.between(nextStageStartedAt, now).getSeconds();

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        switch (nextMode) {
            case "WASHING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                boolean heating = elapsedInNextStage < 400;
                powerWatt = heating ? maxP : minP.multiply(new BigDecimal("2"));
            }
            case "RINSING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minP.multiply(new BigDecimal("1.5"));
            }
            case "SPINNING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = maxP.multiply(new BigDecimal("0.85"));
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
