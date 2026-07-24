package com.vegawatt.core.anomaly.domain;

public record AnomalyEvaluationResult(
        int consecutiveBreachCount,
        int consecutiveNormalCount,
        boolean anomalous,
        boolean transitionedToAnomalous,
        boolean transitionedToRecovered) {
}
