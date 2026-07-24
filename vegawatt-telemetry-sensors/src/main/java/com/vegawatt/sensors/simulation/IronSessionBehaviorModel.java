package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Iron: HEATING -> IRONING -> THERMOSTAT_IDLE -> OFF — a much shorter, plate-thermostat-driven
 * session than an oven's bake (no keep-warm holding phase; irons are left to cool on their own
 * once use ends). Session starts are scheduled via {@link DiurnalCurve#plannedSessionStartHour}
 * (0-2/day, 8-21h).
 */
final class IronSessionBehaviorModel {

    private static final int DAILY_SESSION_CAP = 2;
    private static final double WINDOW_START_HOUR = 8;
    private static final double WINDOW_END_HOUR = 21;

    private static final long HEATING_SECONDS = 150; // 2.5 min
    private static final long IRONING_SECONDS = 1200; // 20 min
    private static final long THERMOSTAT_IDLE_SECONDS = 200; // ~3.3 min unattended before auto-off

    private IronSessionBehaviorModel() {
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
            nextMode = startsSession ? "HEATING" : "OFF";
            if (startsSession) {
                sessionsToday = sessionsToday + 1;
            }
        } else if ("HEATING".equals(mode)) {
            nextMode = elapsedInStage > HEATING_SECONDS ? "IRONING" : "HEATING";
        } else if ("IRONING".equals(mode)) {
            nextMode = elapsedInStage > IRONING_SECONDS ? "THERMOSTAT_IDLE" : "IRONING";
        } else if ("THERMOSTAT_IDLE".equals(mode)) {
            nextMode = elapsedInStage > THERMOSTAT_IDLE_SECONDS ? "OFF" : "THERMOSTAT_IDLE";
        } else {
            nextMode = "OFF";
        }
        if (!nextMode.equals(mode)) {
            nextStageStartedAt = now;
        }

        BigDecimal maxPower = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
        BigDecimal minPower = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("20");
        long elapsedInNextStage = Duration.between(nextStageStartedAt, now).getSeconds();

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        switch (nextMode) {
            case "HEATING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = maxPower.multiply(new BigDecimal("0.95"));
            }
            case "IRONING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                // plate thermostat cycles the heating element on/off to hold temperature.
                boolean pulsingOn = (elapsedInNextStage % 45) < 20;
                powerWatt = pulsingOn ? maxPower : minPower;
            }
            case "THERMOSTAT_IDLE" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                powerWatt = minPower;
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
