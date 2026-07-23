package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Short duration high-power devices like kettles, microwaves, coffee makers, toasters.
 * Cycles between OFF (0W - Standby) and ACTIVE (High Power) for a short session duration.
 */
@Component
public class ShortHighPowerBehaviorModel implements ApplianceBehaviorModel {

    public ShortHighPowerBehaviorModel() {
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.SHORT_HIGH_POWER;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();

        boolean wasActive = previousState != null && previousState.operatingState() == ApplianceOperatingState.ACTIVE;
        Instant stateStartedAt = previousState == null ? now : previousState.stateStartedAt();
        long activeSeconds = wasActive ? Duration.between(stateStartedAt, now).getSeconds() : 0;

        ApplianceOperatingState nextState;
        BigDecimal powerWatt;

        // Session toggles after ~60 to 180 seconds if active, or triggers based on probability if OFF
        if (wasActive) {
            if (activeSeconds > 120 && random.nextDouble() < 0.3) {
                nextState = ApplianceOperatingState.OFF;
                powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
                stateStartedAt = now;
            } else {
                nextState = ApplianceOperatingState.ACTIVE;
                BigDecimal base = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
                double jitter = 0.96 + random.nextDouble() * 0.08;
                powerWatt = base.multiply(BigDecimal.valueOf(jitter));
            }
        } else {
            // OFF state: low probability to start a short high power burst during daytime
            int hour = measuredAt.getHour();
            double startProbability = (hour >= 7 && hour <= 22) ? 0.05 : 0.005;
            if (random.nextDouble() < startProbability) {
                nextState = ApplianceOperatingState.ACTIVE;
                BigDecimal base = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
                double jitter = 0.96 + random.nextDouble() * 0.08;
                powerWatt = base.multiply(BigDecimal.valueOf(jitter));
                stateStartedAt = now;
            } else {
                nextState = ApplianceOperatingState.OFF;
                powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
            }
        }

        ApplianceRuntimeState runtimeState = new ApplianceRuntimeState(
                nextState,
                nextState == ApplianceOperatingState.ACTIVE ? "HEATING" : "STANDBY",
                stateStartedAt, null, null, null, null, now, null, null, null
        );

        return new GeneratedReading(powerWatt, runtimeState);
    }
}
