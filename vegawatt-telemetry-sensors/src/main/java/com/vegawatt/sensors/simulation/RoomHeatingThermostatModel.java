package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Electric/fan heaters: heating element cycles HEATING/IDLE, faster than a fridge compressor
 * since a small room heater responds to ambient temperature much quicker than a sealed cabinet.
 * Modulated by a deterministic time-of-day "demand intensity" curve peaking morning and evening
 * (when a room is occupied and cold) and troughing at midday, with a genuine OFF excursion below
 * a low-demand threshold — a heater realistically isn't cycling unattended all day either.
 */
final class RoomHeatingThermostatModel {

    private static final double MORNING_PEAK_HOUR = 7;
    private static final double EVENING_PEAK_HOUR = 19.5;
    private static final double HALF_WIDTH_HOURS = 4; // demand tapers to 0 well before midday
    private static final double OFF_ENTER_DEMAND = 0.10;
    private static final double OFF_EXIT_DEMAND = 0.25; // hysteresis vs. OFF_ENTER_DEMAND

    private RoomHeatingThermostatModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();
        double demand = DiurnalCurve.demandIntensity(measuredAt, config.applianceId(), HALF_WIDTH_HOURS,
                MORNING_PEAK_HOUR, EVENING_PEAK_HOUR);

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "HEATING";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        if ("HEATING".equals(currentMode)) {
            double toIdleProbability = 0.40 - 0.30 * demand; // low demand -> switches off sooner
            if (elapsedInState > 600 && random.nextDouble() < toIdleProbability) { // 10 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else if ("IDLE".equals(currentMode)) {
            if (elapsedInState > 900) { // 15 mins
                if (demand < OFF_ENTER_DEMAND && random.nextDouble() < 0.15) {
                    nextMode = "OFF";
                    stateStartedAt = now;
                } else {
                    double toHeatingProbability = 0.05 + 0.35 * demand; // high demand -> resumes sooner
                    if (random.nextDouble() < toHeatingProbability) {
                        nextMode = "HEATING";
                        stateStartedAt = now;
                    }
                }
            }
        } else { // OFF
            if (demand > OFF_EXIT_DEMAND) {
                nextMode = "HEATING";
                stateStartedAt = now;
            }
        }

        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("2000");
        BigDecimal standbyMin = config.standbyMinWatt() != null ? config.standbyMinWatt() : new BigDecimal("1");
        BigDecimal standbyMax = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : new BigDecimal("3");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        if ("HEATING".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            double jitter = 0.95 + random.nextDouble() * 0.10;
            powerWatt = maxW.multiply(BigDecimal.valueOf(jitter));
        } else if ("OFF".equals(nextMode)) {
            operatingState = ApplianceOperatingState.OFF;
            powerWatt = standbyMin;
        } else { // IDLE
            operatingState = ApplianceOperatingState.STANDBY;
            powerWatt = TelemetryGenerator.randomInRange(standbyMin, standbyMax, random);
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, stateStartedAt, null, null, null, null, now, null, null, null, 0, null
        );

        return new ApplianceBehaviorModel.GeneratedReading(powerWatt, nextRuntimeState);
    }
}
