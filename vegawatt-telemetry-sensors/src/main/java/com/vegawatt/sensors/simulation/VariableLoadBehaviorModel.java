package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Devices with a usage window during which their load fluctuates rather than staying flat — a
 * cooktop burner being raised/lowered, a desktop or gaming PC's CPU/GPU load varying with what's
 * running. Uses the same dwell-time-until-{@code expectedStateEndAt} approach as
 * {@link ManualSwitchBehaviorModel} to pick realistic usage-session lengths. While active, power
 * follows {@link DiurnalCurve#smoothedNoise01} — a deterministic, gradually-varying function of
 * elapsed dwell time — instead of a fresh uniform-random value across the whole range every tick,
 * since a real load doesn't teleport between its minimum and maximum every 5 seconds.
 */
@Component
public class VariableLoadBehaviorModel implements ApplianceBehaviorModel {

    private record DwellRange(double onMinMinutes, double onMaxMinutes, double offMinMinutes, double offMaxMinutes) {
    }

    private static final DwellRange DEFAULT_DWELL = new DwellRange(20, 120, 30, 240);

    private static DwellRange dwellRangeFor(String catalogCode) {
        if (catalogCode == null) {
            return DEFAULT_DWELL;
        }
        return switch (catalogCode) {
            case "ELECTRIC_COOKTOP" -> new DwellRange(10, 45, 60, 300);
            case "DESKTOP_COMPUTER" -> new DwellRange(60, 300, 30, 600);
            case "GAMING_COMPUTER" -> new DwellRange(30, 180, 60, 600);
            default -> DEFAULT_DWELL;
        };
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.VARIABLE_LOAD;
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
            isActive = random.nextDouble() < activeProbability(measuredAt.getHour(), config.catalogCode());
            stateStartedAt = now;
            DwellRange dwell = dwellRangeFor(config.catalogCode());
            double dwellMinutes = isActive
                    ? dwell.onMinMinutes() + random.nextDouble() * (dwell.onMaxMinutes() - dwell.onMinMinutes())
                    : dwell.offMinMinutes() + random.nextDouble() * (dwell.offMaxMinutes() - dwell.offMinMinutes());
            expectedStateEndAt = now.plusSeconds(Math.round(dwellMinutes * 60));
        }

        BigDecimal powerWatt;
        ApplianceOperatingState state;
        if (isActive) {
            state = ApplianceOperatingState.ACTIVE;
            BigDecimal min = config.simulationMinWatt() != null ? config.simulationMinWatt() : BigDecimal.ZERO;
            BigDecimal max = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : min;
            Duration elapsedInDwell = Duration.between(stateStartedAt, now);
            double noise = DiurnalCurve.smoothedNoise01(elapsedInDwell, config.applianceId());
            powerWatt = min.add(max.subtract(min).multiply(BigDecimal.valueOf(noise)));
        } else {
            state = ApplianceOperatingState.STANDBY;
            powerWatt = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                state, isActive ? "ACTIVE_LOAD" : "STANDBY", stateStartedAt, expectedStateEndAt, null, null, null,
                now, null, null, null, 0, null
        );

        return new GeneratedReading(powerWatt, nextRuntimeState);
    }

    /** Cooktop use peaks around mealtimes; desktop/gaming machines skew toward evening but can be
     * used any time of day. */
    private static double activeProbability(int hour, String catalogCode) {
        boolean mealTime = (hour >= 7 && hour < 9) || (hour >= 12 && hour < 14) || (hour >= 18 && hour < 21);
        if ("ELECTRIC_COOKTOP".equals(catalogCode)) {
            return mealTime ? 0.35 : 0.05;
        }
        boolean evening = hour >= 17 && hour <= 23;
        return evening ? 0.30 : 0.10;
    }
}
