package com.vegawatt.core.anomaly.domain;

import java.math.BigDecimal;

public final class EvaluateApplianceAnomalyPolicy {

    private EvaluateApplianceAnomalyPolicy() {
    }

    public static AnomalyEvaluationResult evaluate(int previousConsecutiveBreachCount, boolean previouslyAnomalous,
                                                     BigDecimal currentPowerWatt, BigDecimal safePowerLimitWatt,
                                                     int breachThreshold) {
        boolean breach = currentPowerWatt.compareTo(safePowerLimitWatt) > 0;

        if (!breach) {
            if (previouslyAnomalous) {
                // Require recoveryThreshold (same as breachThreshold) consecutive normal readings to recover
                int consecutiveNormalCount = Math.max(0, breachThreshold - previousConsecutiveBreachCount + 1);
                boolean recovered = consecutiveNormalCount >= breachThreshold;
                int remainingBreach = recovered ? 0 : previousConsecutiveBreachCount - 1;
                return new AnomalyEvaluationResult(Math.max(0, remainingBreach), !recovered, false, recovered);
            }
            return new AnomalyEvaluationResult(0, false, false, false);
        }

        int consecutiveBreachCount = previousConsecutiveBreachCount + 1;
        boolean anomalous = previouslyAnomalous || consecutiveBreachCount >= breachThreshold;
        boolean transitionedToAnomalous = !previouslyAnomalous && anomalous;
        return new AnomalyEvaluationResult(consecutiveBreachCount, anomalous, transitionedToAnomalous, false);
    }
}
