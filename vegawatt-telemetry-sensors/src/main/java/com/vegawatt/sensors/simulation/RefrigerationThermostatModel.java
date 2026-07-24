package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Refrigerators and freezers: compressor cycles on/off to hold temperature, with an occasional
 * defrost cycle. This is the original {@code ThermostaticCycleBehaviorModel} logic, unchanged —
 * it was already correct for this device family.
 */
final class RefrigerationThermostatModel {

    private RefrigerationThermostatModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "COMPRESSOR_ON";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        if ("COMPRESSOR_ON".equals(currentMode)) {
            if (elapsedInState > 900 && random.nextDouble() < 0.2) { // 15 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else if ("IDLE".equals(currentMode)) {
            if (elapsedInState > 1200 && random.nextDouble() < 0.2) { // 20 mins
                nextMode = (random.nextDouble() < 0.1) ? "DEFROST" : "COMPRESSOR_ON";
                stateStartedAt = now;
            }
        } else if ("DEFROST".equals(currentMode)) {
            if (elapsedInState > 300) { // 5 mins
                nextMode = "COMPRESSOR_ON";
                stateStartedAt = now;
            }
        }

        BigDecimal minW = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("40");
        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("180");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        if ("COMPRESSOR_ON".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            double jitter = 0.95 + random.nextDouble() * 0.10;
            powerWatt = maxW.multiply(BigDecimal.valueOf(jitter));
        } else if ("DEFROST".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            powerWatt = maxW.multiply(new BigDecimal("1.4")); // Defrost heater spike
        } else { // IDLE
            operatingState = ApplianceOperatingState.STANDBY;
            powerWatt = minW;
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, stateStartedAt, null, null, null, null, now, null, null, null, 0, null
        );

        return new ApplianceBehaviorModel.GeneratedReading(powerWatt, nextRuntimeState);
    }
}
