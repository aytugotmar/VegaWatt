package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Electric water heaters: heating element cycles HEATING/IDLE on a much longer period than a room
 * heater or fridge compressor — a tank holds its temperature for a long time between reheats.
 */
final class WaterHeatingThermostatModel {

    private WaterHeatingThermostatModel() {
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
            if (elapsedInState > 1800 && random.nextDouble() < 0.15) { // 30 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else { // IDLE
            if (elapsedInState > 3600 && random.nextDouble() < 0.15) { // 60 mins
                nextMode = "HEATING";
                stateStartedAt = now;
            }
        }

        BigDecimal minW = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("10");
        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("3000");

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
