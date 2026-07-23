package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Thermostatic session model for ovens, irons.
 * PREHEATING -> BAKING / RUNNING -> KEEP_WARM -> OFF.
 */
@Component
public class ThermostaticSessionBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.THERMOSTATIC_SESSION;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();

        String mode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "OFF";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInMode = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = mode;
        ApplianceOperatingState operatingState = ApplianceOperatingState.ACTIVE;
        BigDecimal powerWatt;

        BigDecimal maxPower = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();

        if ("OFF".equals(mode)) {
            operatingState = ApplianceOperatingState.OFF;
            powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
            if (random.nextDouble() < 0.02) { // trigger session
                nextMode = "PREHEATING";
                stateStartedAt = now;
            }
        } else if ("PREHEATING".equals(mode)) {
            powerWatt = maxPower.multiply(new BigDecimal("0.98"));
            if (elapsedInMode > 300) { // 5 min preheat
                nextMode = "BAKING";
                stateStartedAt = now;
            }
        } else if ("BAKING".equals(mode)) {
            // Pulse element
            boolean pulsingOn = (elapsedInMode % 60) < 35;
            powerWatt = pulsingOn ? maxPower : config.simulationMinWatt();
            if (elapsedInMode > 1800) { // 30 min baking
                nextMode = "KEEP_WARM";
                stateStartedAt = now;
            }
        } else if ("KEEP_WARM".equals(mode)) {
            boolean pulsingOn = (elapsedInMode % 120) < 20;
            powerWatt = pulsingOn ? maxPower.multiply(new BigDecimal("0.5")) : config.simulationMinWatt();
            if (elapsedInMode > 600) {
                nextMode = "OFF";
                stateStartedAt = now;
            }
        } else {
            nextMode = "OFF";
            operatingState = ApplianceOperatingState.OFF;
            powerWatt = BigDecimal.ZERO;
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, stateStartedAt, null, null, null, null, now, null, null, null
        );

        return new GeneratedReading(powerWatt, nextRuntimeState);
    }
}
