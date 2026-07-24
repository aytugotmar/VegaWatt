package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Air conditioners: compressor cycles COOLING/IDLE to hold a target temperature. No defrost
 * concept — that's a refrigeration-specific behavior (frost buildup on a cold evaporator coil in
 * a sealed cabinet), not something a room air conditioner cycles through.
 */
final class AirConditioningThermostatModel {

    private AirConditioningThermostatModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "COOLING";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        if ("COOLING".equals(currentMode)) {
            if (elapsedInState > 900 && random.nextDouble() < 0.2) { // 15 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else { // IDLE
            if (elapsedInState > 1200 && random.nextDouble() < 0.2) { // 20 mins
                nextMode = "COOLING";
                stateStartedAt = now;
            }
        }

        BigDecimal minW = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("40");
        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("180");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        if ("COOLING".equals(nextMode)) {
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
