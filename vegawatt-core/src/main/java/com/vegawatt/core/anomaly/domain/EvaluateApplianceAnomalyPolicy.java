package com.vegawatt.core.anomaly.domain;

import java.math.BigDecimal;

public final class EvaluateApplianceAnomalyPolicy {

    private static final int BREACH_THRESHOLD = 3;

    private EvaluateApplianceAnomalyPolicy() {
    }

    public static AnomalyEvaluationResult evaluate(int previousConsecutiveBreachCount, boolean previouslyAnomalous,
                                                     BigDecimal currentPowerWatt, BigDecimal safePowerLimitWatt) {
        boolean breach = currentPowerWatt.compareTo(safePowerLimitWatt) > 0;

        if (!breach) {
            boolean transitionedToRecovered = previouslyAnomalous;
            return new AnomalyEvaluationResult(0, false, false, transitionedToRecovered);
        }

        int consecutiveBreachCount = previousConsecutiveBreachCount + 1;
        boolean anomalous = previouslyAnomalous || consecutiveBreachCount >= BREACH_THRESHOLD;
        boolean transitionedToAnomalous = !previouslyAnomalous && anomalous;
        return new AnomalyEvaluationResult(consecutiveBreachCount, anomalous, transitionedToAnomalous, false);
    }
}
