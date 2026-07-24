package com.vegawatt.core.anomaly.domain;

import java.math.BigDecimal;

/**
 * Hysteresis state machine mirroring {@link EvaluateStandbyConsumptionPolicy}'s shape: while
 * normal, {@code breachThreshold} consecutive readings above {@code safePowerLimitWatt} trigger
 * the anomaly. Once anomalous, recovery requires {@code recoveryThreshold} consecutive readings at
 * or below {@code safePowerLimitWatt * recoveryRatio} — a lower bound than the detection limit by
 * construction, so noise right at the safe limit can't flap the state back and forth. Breach and
 * recovery counts are independent: neither is derived from the other (the previous implementation
 * derived a "normal reading count" arithmetically from the breach count, which made recovery time
 * scale with how long the appliance had been anomalous instead of a fixed threshold).
 */
public final class EvaluateApplianceAnomalyPolicy {

    private EvaluateApplianceAnomalyPolicy() {
    }

    public static AnomalyEvaluationResult evaluate(int previousConsecutiveBreachCount,
                                                     int previousConsecutiveNormalCount, boolean previouslyAnomalous,
                                                     BigDecimal currentPowerWatt, BigDecimal safePowerLimitWatt,
                                                     int breachThreshold, int recoveryThreshold,
                                                     BigDecimal recoveryRatio) {
        if (previouslyAnomalous) {
            BigDecimal recoveryLimitWatt = safePowerLimitWatt.multiply(recoveryRatio);
            boolean normal = currentPowerWatt.compareTo(recoveryLimitWatt) <= 0;
            int consecutiveNormalCount = normal ? previousConsecutiveNormalCount + 1 : 0;
            boolean recovered = consecutiveNormalCount >= recoveryThreshold;
            if (recovered) {
                return new AnomalyEvaluationResult(0, 0, false, false, true);
            }
            return new AnomalyEvaluationResult(previousConsecutiveBreachCount, consecutiveNormalCount, true, false,
                    false);
        }

        boolean breach = currentPowerWatt.compareTo(safePowerLimitWatt) > 0;
        if (!breach) {
            return new AnomalyEvaluationResult(0, 0, false, false, false);
        }

        int consecutiveBreachCount = previousConsecutiveBreachCount + 1;
        boolean anomalous = consecutiveBreachCount >= breachThreshold;
        return new AnomalyEvaluationResult(consecutiveBreachCount, 0, anomalous, anomalous, false);
    }
}
