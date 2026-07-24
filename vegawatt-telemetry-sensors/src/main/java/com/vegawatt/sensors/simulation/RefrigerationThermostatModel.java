package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Refrigerators and freezers: compressor cycles on/off to hold temperature, with an occasional
 * defrost cycle. A fridge runs continuously regardless of time of day — no OFF excursion here —
 * but ambient kitchen warmth still mildly modulates the cycle timing (a slightly higher afternoon
 * ambient temperature means marginally longer compressor runs and shorter idle stretches).
 */
final class RefrigerationThermostatModel {

    private static final double AMBIENT_PEAK_HOUR = 15.5; // afternoon kitchen warmth
    private static final double HALF_WIDTH_HOURS = 9;

    private RefrigerationThermostatModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();
        double demand = DiurnalCurve.demandIntensity(measuredAt, config.applianceId(), HALF_WIDTH_HOURS,
                AMBIENT_PEAK_HOUR);

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "COMPRESSOR_ON";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        if ("COMPRESSOR_ON".equals(currentMode)) {
            double toIdleProbability = 0.20 - 0.10 * demand; // warmer ambient -> runs a bit longer
            if (elapsedInState > 900 && random.nextDouble() < toIdleProbability) { // 15 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else if ("IDLE".equals(currentMode)) {
            double toCompressorProbability = 0.20 + 0.10 * demand; // warmer ambient -> resumes a bit sooner
            if (elapsedInState > 1200 && random.nextDouble() < toCompressorProbability) { // 20 mins
                nextMode = (random.nextDouble() < 0.1) ? "DEFROST" : "COMPRESSOR_ON";
                stateStartedAt = now;
            }
        } else if ("DEFROST".equals(currentMode)) {
            if (elapsedInState > 300) { // 5 mins
                nextMode = "COMPRESSOR_ON";
                stateStartedAt = now;
            }
        }

        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("180");
        BigDecimal standbyMin = config.standbyMinWatt() != null ? config.standbyMinWatt() : new BigDecimal("1");
        BigDecimal standbyMax = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : new BigDecimal("3");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        if ("COMPRESSOR_ON".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            double jitter = 0.95 + random.nextDouble() * 0.10;
            powerWatt = maxW.multiply(BigDecimal.valueOf(jitter));
        } else if ("DEFROST".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            powerWatt = maxW.multiply(new BigDecimal("1.4")); // Defrost heater spike
        } else { // IDLE
            operatingState = ApplianceOperatingState.STANDBY;
            powerWatt = TelemetryGenerator.randomInRange(standbyMin, standbyMax, random);
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, stateStartedAt, null, null, null, null, now, null, null, null, 0, null
        );

        return new ApplianceBehaviorModel.GeneratedReading(powerWatt, nextRuntimeState);
    }
}
