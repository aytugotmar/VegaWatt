package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Manually switched devices (LED bulbs, desk lamps, fans, range hoods).
 * Toggles between OFF and ACTIVE according to hour of day (higher evening usage), but — unlike a
 * naive per-tick reroll — holds whichever state it's in for a randomized minimum dwell period
 * ({@link ApplianceRuntimeState#expectedStateEndAt()}) so a lamp doesn't flicker on/off every 5s.
 */
@Component
public class ManualSwitchBehaviorModel implements ApplianceBehaviorModel {

    private static final DwellRange DEFAULT_DWELL = new DwellRange(15, 90, 15, 90);

    /** on-min, on-max, off-min, off-max dwell minutes, per catalog code. */
    private record DwellRange(double onMinMinutes, double onMaxMinutes, double offMinMinutes, double offMaxMinutes) {
    }

    private static DwellRange dwellRangeFor(String catalogCode) {
        if (catalogCode == null) {
            return DEFAULT_DWELL;
        }
        return switch (catalogCode) {
            case "LED_BULB", "CHANDELIER" -> new DwellRange(15, 120, 15, 120);
            case "DESK_LAMP" -> new DwellRange(10, 90, 10, 90);
            case "FAN" -> new DwellRange(20, 180, 20, 180);
            case "RANGE_HOOD" -> new DwellRange(5, 30, 30, 240);
            default -> DEFAULT_DWELL;
        };
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.MANUAL_SWITCH;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        boolean dwellStillActive = previousState != null && previousState.expectedStateEndAt() != null
                && now.isBefore(previousState.expectedStateEndAt());

        boolean isActive;
        Instant stateStartedAt;
        Instant expectedStateEndAt;
        if (dwellStillActive) {
            isActive = previousState.operatingState() == ApplianceOperatingState.ACTIVE;
            stateStartedAt = previousState.stateStartedAt();
            expectedStateEndAt = previousState.expectedStateEndAt();
        } else {
            isActive = random.nextDouble() < activeProbability(measuredAt.getHour());
            stateStartedAt = now;
            DwellRange dwell = dwellRangeFor(config.catalogCode());
            double dwellMinutes = isActive
                    ? dwell.onMinMinutes() + random.nextDouble() * (dwell.onMaxMinutes() - dwell.onMinMinutes())
                    : dwell.offMinMinutes() + random.nextDouble() * (dwell.offMaxMinutes() - dwell.offMinMinutes());
            expectedStateEndAt = now.plusSeconds(Math.round(dwellMinutes * 60));
        }

        ApplianceOperatingState state = isActive ? ApplianceOperatingState.ACTIVE : ApplianceOperatingState.OFF;
        BigDecimal basePower = config.simulationMinWatt() != null ? config.simulationMinWatt() : new BigDecimal("10");
        if (isActive && config.simulationMaxWatt() != null) {
            double jitter = 0.97 + random.nextDouble() * 0.06;
            basePower = config.simulationMaxWatt().multiply(BigDecimal.valueOf(jitter));
        } else if (!isActive) {
            basePower = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                state, isActive ? "ON" : "OFF", stateStartedAt, expectedStateEndAt, null, null, null, now, null, null,
                null, 0, null
        );

        return new GeneratedReading(basePower, nextRuntimeState);
    }

    private static double activeProbability(int hour) {
        boolean isEvening = (hour >= 18 && hour <= 23);
        boolean isMorning = (hour >= 6 && hour <= 9);
        return isEvening ? 0.85 : (isMorning ? 0.40 : 0.10);
    }
}
