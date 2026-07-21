package com.vegawatt.core.anomaly.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EvaluateApplianceAnomalyPolicyTest {

    private static final BigDecimal SAFE_LIMIT = new BigDecimal("2200");
    private static final int BREACH_THRESHOLD = 3;

    @Test
    void staysNormalBelowSafeLimit() {
        AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(0, false,
                new BigDecimal("1800"), SAFE_LIMIT, BREACH_THRESHOLD);

        assertThat(result.consecutiveBreachCount()).isZero();
        assertThat(result.anomalous()).isFalse();
        assertThat(result.transitionedToAnomalous()).isFalse();
    }

    @Test
    void incrementsBreachCounterOnSingleViolation() {
        AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(0, false,
                new BigDecimal("2500"), SAFE_LIMIT, BREACH_THRESHOLD);

        assertThat(result.consecutiveBreachCount()).isEqualTo(1);
        assertThat(result.anomalous()).isFalse();
    }

    @Test
    void marksAnomalousOnThirdConsecutiveBreach() {
        AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(2, false,
                new BigDecimal("2500"), SAFE_LIMIT, BREACH_THRESHOLD);

        assertThat(result.consecutiveBreachCount()).isEqualTo(3);
        assertThat(result.anomalous()).isTrue();
        assertThat(result.transitionedToAnomalous()).isTrue();
    }

    @Test
    void doesNotRetriggerTransitionWhileAlreadyAnomalous() {
        AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(3, true,
                new BigDecimal("2600"), SAFE_LIMIT, BREACH_THRESHOLD);

        assertThat(result.anomalous()).isTrue();
        assertThat(result.transitionedToAnomalous()).isFalse();
    }

    @Test
    void resetsCounterAndClearsAnomalyOnRecovery() {
        AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(4, true,
                new BigDecimal("1500"), SAFE_LIMIT, BREACH_THRESHOLD);

        assertThat(result.consecutiveBreachCount()).isZero();
        assertThat(result.anomalous()).isFalse();
        assertThat(result.transitionedToRecovered()).isTrue();
    }

    @Test
    void allowsReTriggerAfterRecoveryWithThreeNewBreaches() {
        AnomalyEvaluationResult recovered = EvaluateApplianceAnomalyPolicy.evaluate(3, true,
                new BigDecimal("1500"), SAFE_LIMIT, BREACH_THRESHOLD);
        AnomalyEvaluationResult breach1 = EvaluateApplianceAnomalyPolicy.evaluate(recovered.consecutiveBreachCount(),
                recovered.anomalous(), new BigDecimal("2500"), SAFE_LIMIT, BREACH_THRESHOLD);
        AnomalyEvaluationResult breach2 = EvaluateApplianceAnomalyPolicy.evaluate(breach1.consecutiveBreachCount(),
                breach1.anomalous(), new BigDecimal("2500"), SAFE_LIMIT, BREACH_THRESHOLD);
        AnomalyEvaluationResult breach3 = EvaluateApplianceAnomalyPolicy.evaluate(breach2.consecutiveBreachCount(),
                breach2.anomalous(), new BigDecimal("2500"), SAFE_LIMIT, BREACH_THRESHOLD);

        assertThat(breach3.anomalous()).isTrue();
        assertThat(breach3.transitionedToAnomalous()).isTrue();
    }

    @Test
    void honoursConfiguredBreachThresholdDifferentFromDefault() {
        AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(4, false,
                new BigDecimal("2500"), SAFE_LIMIT, 5);

        assertThat(result.consecutiveBreachCount()).isEqualTo(5);
        assertThat(result.anomalous()).isTrue();
        assertThat(result.transitionedToAnomalous()).isTrue();
    }
}
