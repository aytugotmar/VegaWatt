package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Router/modem/mesh-node/hub-style devices: always ACTIVE, narrow power range (yapılacak.md
 * §15.1), with a low-probability fault episode ({@code POWER_SPIKE_MALFUNCTION}) that pushes power
 * above the safe limit for a short, bounded period — replacing the old per-tick global spike with
 * a sustained, realistic episode (§14).
 */
@Component
class AlwaysOnStableBehaviorModel implements ApplianceBehaviorModel {

    private static final String FAULT_CODE = "POWER_SPIKE_MALFUNCTION";
    private static final BigDecimal FAULT_POWER_MULTIPLIER = new BigDecimal("1.3");
    private static final long MIN_FAULT_SECONDS = 30;
    private static final long MAX_FAULT_SECONDS = 60;

    private final SimulationProperties properties;

    AlwaysOnStableBehaviorModel(SimulationProperties properties) {
        this.properties = properties;
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.ALWAYS_ON_STABLE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        boolean faultOngoing = previousState != null && previousState.activeFaultCode() != null
                && now.isBefore(previousState.faultExpectedEndAt());
        // A fault that was active last tick but whose window has now elapsed just ended — start
        // its cooldown from this tick, so the very next evaluation can't immediately re-trigger.
        boolean faultJustEnded = !faultOngoing && previousState != null && previousState.activeFaultCode() != null;

        Instant cooldownUntil = faultJustEnded
                ? now.plusSeconds(properties.faultCooldownSeconds())
                : previousState == null ? null : previousState.faultCooldownUntil();
        boolean cooldownActive = cooldownUntil != null && now.isBefore(cooldownUntil);

        Instant scheduledEvaluationAt = previousState == null ? null : previousState.nextFaultEvaluationAt();
        boolean evaluationDue = scheduledEvaluationAt == null || !now.isBefore(scheduledEvaluationAt);

        boolean faultStarting = !faultOngoing && !cooldownActive && evaluationDue
                && random.nextDouble() < properties.faultProbability();

        // Only advance the evaluation clock when an evaluation actually happens (dice rolled or
        // skipped for cooldown) — a fault in progress keeps the schedule frozen so the next real
        // evaluation lands `faultEvaluationIntervalSeconds` after the fault ends, not after it started.
        Instant nextFaultEvaluationAt = faultOngoing
                ? scheduledEvaluationAt
                : evaluationDue ? now.plusSeconds(properties.faultEvaluationIntervalSeconds()) : scheduledEvaluationAt;

        BigDecimal powerWatt;
        String faultCode;
        Instant faultStartedAt;
        Instant faultExpectedEndAt;

        if (faultOngoing) {
            powerWatt = faultPower(config, random);
            faultCode = previousState.activeFaultCode();
            faultStartedAt = previousState.faultStartedAt();
            faultExpectedEndAt = previousState.faultExpectedEndAt();
        } else if (faultStarting) {
            powerWatt = faultPower(config, random);
            long durationSeconds = MIN_FAULT_SECONDS
                    + Math.round(random.nextDouble() * (MAX_FAULT_SECONDS - MIN_FAULT_SECONDS));
            faultCode = FAULT_CODE;
            faultStartedAt = now;
            faultExpectedEndAt = now.plusSeconds(durationSeconds);
        } else {
            powerWatt = TelemetryGenerator.randomInRange(config.simulationMinWatt(), config.simulationMaxWatt(),
                    random);
            faultCode = null;
            faultStartedAt = null;
            faultExpectedEndAt = null;
        }

        Instant stateStartedAt = previousState == null ? now : previousState.stateStartedAt();
        ApplianceRuntimeState nextState = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "NORMAL",
                stateStartedAt, null, faultCode, faultStartedAt, faultExpectedEndAt, now, null,
                nextFaultEvaluationAt, cooldownUntil);

        return new GeneratedReading(powerWatt, nextState);
    }

    private static BigDecimal faultPower(ApplianceConfig config, RandomSource random) {
        BigDecimal ceiling = config.safePowerLimitWatt().multiply(FAULT_POWER_MULTIPLIER);
        return TelemetryGenerator.randomInRange(config.safePowerLimitWatt(), ceiling, random);
    }
}
