package com.vegawatt.core.anomaly.domain;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.home.domain.ApplianceOperatingState;
import java.math.BigDecimal;

/**
 * Detects {@code UNUSUAL_STANDBY_CONSUMPTION}: a STANDBY_DEVICE-profile appliance drawing far more
 * power than its catalog standby range while reporting STANDBY. Independent of
 * {@link EvaluateApplianceAnomalyPolicy}'s safe-power-limit rule — neither state nor recovery is
 * shared between the two.
 */
public final class EvaluateStandbyConsumptionPolicy {

    private EvaluateStandbyConsumptionPolicy() {
    }

    /**
     * Only appliances reliably reporting STANDBY (never inferred from wattage — v1 telemetry with
     * no operating state is excluded here since {@code operatingState} is null) on the
     * STANDBY_DEVICE profile, with a catalog standby ceiling to compare against, are evaluated.
     */
    public static boolean isEligible(ApplianceOperatingState operatingState,
                                      ApplianceBehaviorProfile behaviorProfileSnapshot, BigDecimal standbyMaxWatt) {
        return operatingState == ApplianceOperatingState.STANDBY
                && behaviorProfileSnapshot == ApplianceBehaviorProfile.STANDBY_DEVICE
                && standbyMaxWatt != null;
    }

    public static BigDecimal calculateThresholdWatt(BigDecimal standbyMaxWatt, BigDecimal thresholdMultiplier,
                                                      BigDecimal minimumExcessWatt) {
        BigDecimal multiplied = standbyMaxWatt.multiply(thresholdMultiplier);
        BigDecimal additive = standbyMaxWatt.add(minimumExcessWatt);
        return multiplied.max(additive);
    }

    /**
     * Hysteresis state machine mirroring {@link EvaluateApplianceAnomalyPolicy}'s shape: while
     * inactive, {@code breachThreshold} consecutive readings above {@code calculatedThresholdWatt}
     * activate the anomaly (flagged only on the exact transition tick, so callers never emit more
     * than one detection event). Once active, recovery requires {@code recoveryThreshold}
     * consecutive readings at or below the catalog's own {@code standbyMaxWatt} — a lower bound
     * than the detection threshold by construction, so noise near the detection boundary can never
     * flip the state back and forth.
     */
    public static StandbyAnomalyEvaluationResult evaluate(int previousBreachCount, int previousRecoveryCount,
                                                            boolean previouslyActive, BigDecimal currentPowerWatt,
                                                            BigDecimal standbyMaxWatt, BigDecimal thresholdMultiplier,
                                                            BigDecimal minimumExcessWatt, int breachThreshold,
                                                            int recoveryThreshold) {
        BigDecimal calculatedThresholdWatt = calculateThresholdWatt(standbyMaxWatt, thresholdMultiplier,
                minimumExcessWatt);

        if (!previouslyActive) {
            boolean abnormal = currentPowerWatt.compareTo(calculatedThresholdWatt) > 0;
            int breachCount = abnormal ? previousBreachCount + 1 : 0;
            boolean active = breachCount >= breachThreshold;
            return new StandbyAnomalyEvaluationResult(breachCount, 0, active, active, false, calculatedThresholdWatt);
        }

        boolean normal = currentPowerWatt.compareTo(standbyMaxWatt) <= 0;
        int recoveryCount = normal ? previousRecoveryCount + 1 : 0;
        boolean recovered = recoveryCount >= recoveryThreshold;
        if (recovered) {
            // standbyBreachCount resets to 0 so a fresh detection cycle is required post-recovery;
            // standbyRecoveryCount is reported as the count that triggered this transition (for
            // event metadata) rather than 0 — harmless for persistence, since the next evaluation
            // while inactive ignores the previous recovery count entirely.
            return new StandbyAnomalyEvaluationResult(0, recoveryCount, false, false, true, calculatedThresholdWatt);
        }
        return new StandbyAnomalyEvaluationResult(previousBreachCount, recoveryCount, true, false, false,
                calculatedThresholdWatt);
    }
}
