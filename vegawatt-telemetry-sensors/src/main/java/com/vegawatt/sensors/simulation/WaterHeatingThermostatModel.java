package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Electric water heaters: heating element cycles HEATING/IDLE on a much longer period than a room
 * heater or fridge compressor — a tank holds its temperature for a long time between reheats. The
 * tank genuinely needs to run regardless of time of day — no OFF excursion here — but demand for
 * hot water still mildly modulates the cycle timing, peaking around morning and evening
 * showers/dishes and easing at midday.
 */
final class WaterHeatingThermostatModel {

    private static final double MORNING_PEAK_HOUR = 7;
    private static final double EVENING_PEAK_HOUR = 19.5;
    private static final double HALF_WIDTH_HOURS = 4;

    private WaterHeatingThermostatModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();
        double demand = DiurnalCurve.demandIntensity(measuredAt, config.applianceId(), HALF_WIDTH_HOURS,
                MORNING_PEAK_HOUR, EVENING_PEAK_HOUR);

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "HEATING";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        if ("HEATING".equals(currentMode)) {
            double toIdleProbability = 0.15 - 0.08 * demand; // high hot-water demand -> runs a bit longer
            if (elapsedInState > 1800 && random.nextDouble() < toIdleProbability) { // 30 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else { // IDLE
            double toHeatingProbability = 0.15 + 0.10 * demand; // high hot-water demand -> resumes a bit sooner
            if (elapsedInState > 3600 && random.nextDouble() < toHeatingProbability) { // 60 mins
                nextMode = "HEATING";
                stateStartedAt = now;
            }
        }

        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("3000");
        BigDecimal standbyMin = config.standbyMinWatt() != null ? config.standbyMinWatt() : new BigDecimal("1");
        BigDecimal standbyMax = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : new BigDecimal("3");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        if ("HEATING".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            double jitter = 0.95 + random.nextDouble() * 0.10;
            powerWatt = maxW.multiply(BigDecimal.valueOf(jitter));
        } else {
            operatingState = ApplianceOperatingState.STANDBY;
            powerWatt = TelemetryGenerator.randomInRange(standbyMin, standbyMax, random);
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, stateStartedAt, null, null, null, null, now, null, null, null, 0, null
        );

        return new ApplianceBehaviorModel.GeneratedReading(powerWatt, nextRuntimeState);
    }
}
