package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Program cycle model for washing machines, dishwashers, dryers.
 * Multi-stage: WASHING -> RINSING -> SPINNING -> OFF, capped at a realistic number of programs per
 * day (nobody runs a washing machine dozens of times a day) and only starting during reasonable
 * daytime hours. Power and operating state are always derived from the post-transition
 * {@code nextMode}, never the pre-transition mode — otherwise the tick a stage ends (or a new
 * cycle starts) can emit a state/power reading that contradicts the persisted mode.
 */
@Component
public class ProgramCycleBehaviorModel implements ApplianceBehaviorModel {

    private static final int DAILY_PROGRAM_CAP = 2;
    private static final double START_PROBABILITY = 0.015;
    private static final int WINDOW_START_HOUR = 7;
    private static final int WINDOW_END_HOUR = 23;

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.PROGRAM_CYCLE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
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
            int hour = measuredAt.getHour();
            boolean inWindow = hour >= WINDOW_START_HOUR && hour < WINDOW_END_HOUR;
            boolean startsNewCycle = sessionsToday < DAILY_PROGRAM_CAP && inWindow
                    && random.nextDouble() < START_PROBABILITY;
            nextMode = startsNewCycle ? "WASHING" : "OFF";
            if (startsNewCycle) {
                nextStageStartedAt = now;
                sessionsToday = sessionsToday + 1;
            }
        } else if ("WASHING".equals(mode)) {
            nextMode = elapsedInStage > 900 ? "RINSING" : "WASHING";
        } else if ("RINSING".equals(mode)) {
            nextMode = elapsedInStage > 450 ? "SPINNING" : "RINSING";
        } else if ("SPINNING".equals(mode)) {
            nextMode = elapsedInStage > 300 ? "OFF" : "SPINNING";
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

        return new GeneratedReading(powerWatt, nextRuntimeState);
    }
}
