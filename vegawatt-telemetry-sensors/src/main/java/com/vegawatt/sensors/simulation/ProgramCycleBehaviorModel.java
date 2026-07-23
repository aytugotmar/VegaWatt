package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Program cycle model for washing machines, dishwashers, dryers.
 * Multi-stage: WASHING -> RINSING -> SPINNING -> COMPLETED -> OFF.
 */
@Component
public class ProgramCycleBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.PROGRAM_CYCLE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();

        String mode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "OFF";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInStage = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = mode;
        ApplianceOperatingState operatingState = ApplianceOperatingState.ACTIVE;
        BigDecimal powerWatt;

        BigDecimal maxP = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
        BigDecimal minP = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("50");

        if ("OFF".equals(mode)) {
            operatingState = ApplianceOperatingState.OFF;
            powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
            if (random.nextDouble() < 0.015) { // start wash cycle
                nextMode = "WASHING";
                stateStartedAt = now;
            }
        } else if ("WASHING".equals(mode)) {
            // High power during heating phase of wash
            boolean heating = elapsedInStage < 400;
            powerWatt = heating ? maxP : minP.multiply(new BigDecimal("2"));
            if (elapsedInStage > 900) {
                nextMode = "RINSING";
                stateStartedAt = now;
            }
        } else if ("RINSING".equals(mode)) {
            powerWatt = minP.multiply(new BigDecimal("1.5"));
            if (elapsedInStage > 450) {
                nextMode = "SPINNING";
                stateStartedAt = now;
            }
        } else if ("SPINNING".equals(mode)) {
            powerWatt = maxP.multiply(new BigDecimal("0.85"));
            if (elapsedInStage > 300) {
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
