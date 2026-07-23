package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Scheduled lighting/appliances (garden lights, outdoor security lights).
 * Active during night hours (19:00 - 06:00), OFF during day.
 */
@Component
public class ScheduledSwitchBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.SCHEDULED_SWITCH;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        int hour = measuredAt.getHour();

        boolean isNight = (hour >= 19 || hour < 6);

        ApplianceOperatingState state = isNight ? ApplianceOperatingState.ACTIVE : ApplianceOperatingState.OFF;

        BigDecimal basePower;
        if (isNight) {
            BigDecimal max = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
            double jitter = 0.98 + random.nextDouble() * 0.04;
            basePower = max.multiply(BigDecimal.valueOf(jitter));
        } else {
            basePower = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
        }

        Instant stateStartedAt = (previousState != null && previousState.operatingState() == state)
                ? previousState.stateStartedAt() : now;

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                state, isNight ? "SCHEDULED_ON" : "SCHEDULED_OFF", stateStartedAt, null, null, null, null, now, null, null, null
        );

        return new GeneratedReading(basePower, nextRuntimeState);
    }
}
