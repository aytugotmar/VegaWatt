package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Air conditioners: compressor cycles COOLING/IDLE to hold a target temperature, modulated by a
 * deterministic time-of-day "demand intensity" curve (peaking in the afternoon, troughing
 * overnight) so cycling isn't as if constant demand exists around the clock. Below a low-demand
 * threshold at night, the unit can take a genuine OFF excursion — most homes don't run an AC
 * unattended overnight — resuming automatically once demand rises again, all as a pure function
 * of the clock (no persisted temperature state). No defrost concept — that's a
 * refrigeration-specific behavior (frost buildup on a cold evaporator coil in a sealed cabinet),
 * not something a room air conditioner cycles through.
 */
final class AirConditioningThermostatModel {

    private static final double PEAK_HOUR = 15.5; // mid-afternoon
    private static final double HALF_WIDTH_HOURS = 9; // demand tapers to 0 by ~06:30/00:30
    private static final double OFF_ENTER_DEMAND = 0.10;
    private static final double OFF_EXIT_DEMAND = 0.25; // hysteresis vs. OFF_ENTER_DEMAND

    private AirConditioningThermostatModel() {
    }

    static ApplianceBehaviorModel.GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                                             ZonedDateTime measuredAt, RandomSource random) {
        Instant now = measuredAt.toInstant();
        double demand = DiurnalCurve.demandIntensity(measuredAt, config.applianceId(), HALF_WIDTH_HOURS, PEAK_HOUR);

        String currentMode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "COOLING";
        Instant stateStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        long elapsedInState = Duration.between(stateStartedAt, now).getSeconds();

        String nextMode = currentMode;
        if ("COOLING".equals(currentMode)) {
            double toIdleProbability = 0.35 - 0.25 * demand; // low demand -> switches off sooner
            if (elapsedInState > 900 && random.nextDouble() < toIdleProbability) { // 15 mins
                nextMode = "IDLE";
                stateStartedAt = now;
            }
        } else if ("IDLE".equals(currentMode)) {
            if (elapsedInState > 1200) { // 20 mins
                if (demand < OFF_ENTER_DEMAND && random.nextDouble() < 0.15) {
                    nextMode = "OFF";
                    stateStartedAt = now;
                } else {
                    double toCoolingProbability = 0.05 + 0.30 * demand; // high demand -> resumes sooner
                    if (random.nextDouble() < toCoolingProbability) {
                        nextMode = "COOLING";
                        stateStartedAt = now;
                    }
                }
            }
        } else { // OFF
            if (demand > OFF_EXIT_DEMAND) {
                nextMode = "COOLING";
                stateStartedAt = now;
            }
        }

        BigDecimal maxW = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : new BigDecimal("180");
        BigDecimal standbyMin = config.standbyMinWatt() != null ? config.standbyMinWatt() : new BigDecimal("1");
        BigDecimal standbyMax = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : new BigDecimal("3");

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        if ("COOLING".equals(nextMode)) {
            operatingState = ApplianceOperatingState.ACTIVE;
            double jitter = 0.95 + random.nextDouble() * 0.10;
            powerWatt = maxW.multiply(BigDecimal.valueOf(jitter));
        } else if ("OFF".equals(nextMode)) {
            operatingState = ApplianceOperatingState.OFF;
            powerWatt = standbyMin;
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
