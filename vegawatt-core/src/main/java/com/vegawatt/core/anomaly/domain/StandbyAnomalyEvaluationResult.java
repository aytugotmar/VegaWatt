package com.vegawatt.core.anomaly.domain;

import java.math.BigDecimal;

public record StandbyAnomalyEvaluationResult(
        int standbyBreachCount,
        int standbyRecoveryCount,
        boolean standbyAnomalyActive,
        boolean transitionedToActive,
        boolean transitionedToRecovered,
        BigDecimal calculatedThresholdWatt) {

    public static StandbyAnomalyEvaluationResult unchanged(int standbyBreachCount, int standbyRecoveryCount,
                                                             boolean standbyAnomalyActive) {
        return new StandbyAnomalyEvaluationResult(standbyBreachCount, standbyRecoveryCount, standbyAnomalyActive,
                false, false, null);
    }
}
