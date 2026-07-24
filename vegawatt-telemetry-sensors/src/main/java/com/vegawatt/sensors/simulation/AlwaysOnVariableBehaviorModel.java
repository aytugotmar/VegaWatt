package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Always-on devices whose draw genuinely varies tick to tick (air purifiers cycling fan speed,
 * security cameras varying with motion/encoding load) — unlike {@link AlwaysOnStableBehaviorModel}'s
 * near-constant draw, power fluctuates across the appliance's full simulated range every tick.
 */
@Component
public class AlwaysOnVariableBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.ALWAYS_ON_VARIABLE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;

        BigDecimal min = config.simulationMinWatt() != null ? config.simulationMinWatt() : BigDecimal.ZERO;
        BigDecimal max = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : min;
        BigDecimal powerWatt = TelemetryGenerator.randomInRange(min, max, random);

        ApplianceRuntimeState nextState = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "RUNNING",
                stateStartedAt, null, null, null, null, now, null, null, null, 0, null);
        return new GeneratedReading(powerWatt, nextState);
    }
}
