package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Electric/fan heaters: heating element cycles HEATING/IDLE, faster than a fridge compressor
 * since a small room heater responds to ambient temperature much quicker than a sealed cabinet.
 */
final class RoomHeatingThermostatModel {

    private RoomHeatingThermostatModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "HEATING";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        if ("HEATING".equals(currentMode)) {
            if (elapsedInState > 600 && random.nextDouble() < 0.25) { // 10 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else { // IDLE
            if (elapsedInState > 900 && random.nextDouble() < 0.25) { // 15 mins
                nextMode = "HEATING";
                stateStartedAt = now;
            }
        }

        BigDecimal minW = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("10");
        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("2000");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        if ("HEATING".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            double jitter = 0.95 + random.nextDouble() * 0.10;
            powerWatt = maxW.multiply(BigDecimal.valueOf(jitter));
        } else {
            operatingState = ApplianceOperatingState.STANDBY;
            powerWatt = minW;
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, stateStartedAt, null, null, null, null, now, null, null, null, 0, null
        );

        return new ApplianceBehaviorModel.GeneratedReading(powerWatt, nextRuntimeState);
    }
}
