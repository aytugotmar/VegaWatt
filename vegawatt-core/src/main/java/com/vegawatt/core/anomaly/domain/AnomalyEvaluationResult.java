package com.vegawatt.core.anomaly.domain;

public record AnomalyEvaluationResult(
        int consecutiveBreachCount,
        boolean anomalous,
        boolean transitionedToAnomalous,
        boolean transitionedToRecovered) {
}
