package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Manually switched devices (LED bulbs, desk lamps, fans).
 * Toggles between OFF and ACTIVE according to hour of day (higher evening usage).
 */
@Component
public class ManualSwitchBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.MANUAL_SWITCH;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        int hour = measuredAt.getHour();

        boolean isEvening = (hour >= 18 && hour <= 23);
        boolean isMorning = (hour >= 6 && hour <= 9);

        double activeProbability = isEvening ? 0.85 : (isMorning ? 0.40 : 0.10);

        boolean isActive = random.nextDouble() < activeProbability;
        ApplianceOperatingState state = isActive ? ApplianceOperatingState.ACTIVE : ApplianceOperatingState.OFF;

        BigDecimal basePower = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("10");
        if (isActive && config.simulationMaxWatt() != null) {
            double jitter = 0.97 + random.nextDouble() * 0.06;
            basePower = config.simulationMaxWatt().multiply(BigDecimal.valueOf(jitter));
        } else if (!isActive) {
            basePower = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
        }

        Instant stateStartedAt = (previousState != null && previousState.operatingState() == state)
                ? previousState.stateStartedAt() : now;

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                state, isActive ? "ON" : "OFF", stateStartedAt, null, null, null, null, now, null, null, null
        );

        return new GeneratedReading(basePower, nextRuntimeState);
    }
}
