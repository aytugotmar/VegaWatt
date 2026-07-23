package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Thermostatic cycle behavior model for refrigerators, freezers, air conditioners, heaters.
 * Cycles through IDLE / COMPRESSOR_ON / DEFROST.
 */
@Component
public class ThermostaticCycleBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.THERMOSTATIC_CYCLE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "COMPRESSOR_ON";

        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        ApplianceOperatingState operatingState = ApplianceOperatingState.ACTIVE;
        BigDecimal powerWatt;

        // Transition modes based on elapsed duration
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
                operatingState, nextMode, stateStartedAt, null, null, null, null, now, null, null, null
        );

        return new GeneratedReading(powerWatt, nextRuntimeState);
    }
}
