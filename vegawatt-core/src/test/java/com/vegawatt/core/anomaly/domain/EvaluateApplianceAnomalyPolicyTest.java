package com.vegawatt.core.anomaly.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EvaluateApplianceAnomalyPolicyTest {

    private static final BigDecimal SAFE_LIMIT = new BigDecimal("2200");
    private static final int BREACH_THRESHOLD = 3;
    private static final int RECOVERY_THRESHOLD = 3;
    private static final BigDecimal RECOVERY_RATIO = new BigDecimal("0.90");

    private static AnomalyEvaluationResult evaluate(int previousBreachCount, int previousNormalCount,
                                                      boolean previouslyAnomalous, BigDecimal currentPowerWatt) {
        return EvaluateApplianceAnomalyPolicy.evaluate(previousBreachCount, previousNormalCount, previouslyAnomalous,
                currentPowerWatt, SAFE_LIMIT, BREACH_THRESHOLD, RECOVERY_THRESHOLD, RECOVERY_RATIO);
    }

    @Test
    void staysNormalBelowSafeLimit() {
        AnomalyEvaluationResult result = evaluate(0, 0, false, new BigDecimal("1800"));

        assertThat(result.consecutiveBreachCount()).isZero();
        assertThat(result.anomalous()).isFalse();
        assertThat(result.transitionedToAnomalous()).isFalse();
    }

    @Test
    void incrementsBreachCounterOnSingleViolation() {
        AnomalyEvaluationResult result = evaluate(0, 0, false, new BigDecimal("2500"));

        assertThat(result.consecutiveBreachCount()).isEqualTo(1);
        assertThat(result.anomalous()).isFalse();
    }

    @Test
    void marksAnomalousOnThirdConsecutiveBreach() {
        AnomalyEvaluationResult result = evaluate(2, 0, false, new BigDecimal("2500"));

        assertThat(result.consecutiveBreachCount()).isEqualTo(3);
        assertThat(result.anomalous()).isTrue();
        assertThat(result.transitionedToAnomalous()).isTrue();
    }

    @Test
    void doesNotRetriggerTransitionWhileAlreadyAnomalous() {
        AnomalyEvaluationResult result = evaluate(3, 0, true, new BigDecimal("2600"));

        assertThat(result.anomalous()).isTrue();
        assertThat(result.transitionedToAnomalous()).isFalse();
    }

    @Test
    void resetsCounterAndClearsAnomalyOnRecovery() {
        AnomalyEvaluationResult result = evaluate(4, 2, true, new BigDecimal("1500"));

        assertThat(result.consecutiveBreachCount()).isZero();
        assertThat(result.anomalous()).isFalse();
        assertThat(result.transitionedToRecovered()).isTrue();
    }

    @Test
    void recoveryTakesExactlyThreeNormalReadingsRegardlessOfHowLongTheApplianceWasAnomalous() {
        // A device that stayed anomalous for a long time (breach count climbed to 100) must still
        // recover in exactly RECOVERY_THRESHOLD normal readings — this is the bug the old
        // implementation had: it derived the "normal reading count" arithmetically from the breach
        // count, so recovery time scaled with how anomalous the device had been.
        int breachCount = 100;
        AnomalyEvaluationResult first = evaluate(breachCount, 0, true, new BigDecimal("1500"));
        assertThat(first.anomalous()).isTrue();
        assertThat(first.transitionedToRecovered()).isFalse();

        AnomalyEvaluationResult second = evaluate(first.consecutiveBreachCount(), first.consecutiveNormalCount(),
                true, new BigDecimal("1500"));
        assertThat(second.anomalous()).isTrue();
        assertThat(second.transitionedToRecovered()).isFalse();

        AnomalyEvaluationResult third = evaluate(second.consecutiveBreachCount(), second.consecutiveNormalCount(),
                true, new BigDecimal("1500"));
        assertThat(third.anomalous()).isFalse();
        assertThat(third.transitionedToRecovered()).isTrue();
        assertThat(third.consecutiveBreachCount()).isZero();
    }

    @Test
    void aBreachDuringRecoveryResetsTheNormalStreakWithoutLosingBreachHistory() {
        AnomalyEvaluationResult afterOneNormal = evaluate(5, 0, true, new BigDecimal("1500"));
        assertThat(afterOneNormal.consecutiveNormalCount()).isEqualTo(1);

        AnomalyEvaluationResult brokenByBreach = evaluate(afterOneNormal.consecutiveBreachCount(),
                afterOneNormal.consecutiveNormalCount(), true, new BigDecimal("2500"));

        assertThat(brokenByBreach.anomalous()).isTrue();
        assertThat(brokenByBreach.consecutiveNormalCount()).isZero();
        assertThat(brokenByBreach.consecutiveBreachCount()).isEqualTo(5);
    }

    @Test
    void recoveryRequiresPowerAtOrBelowRecoveryRatioNotJustTheSafeLimit() {
        // Hysteresis: a reading just under the safe limit (2200 * 0.95) isn't low enough to count
        // as a recovery reading once recoveryRatio (0.90) is applied.
        AnomalyEvaluationResult result = evaluate(3, 0, true, new BigDecimal("2090"));

        assertThat(result.consecutiveNormalCount()).isZero();
        assertThat(result.anomalous()).isTrue();
    }

    @Test
    void allowsReTriggerAfterRecoveryWithThreeNewBreaches() {
        AnomalyEvaluationResult recovered = evaluate(3, 2, true, new BigDecimal("1500"));
        assertThat(recovered.transitionedToRecovered()).isTrue();

        AnomalyEvaluationResult breach1 = evaluate(recovered.consecutiveBreachCount(),
                recovered.consecutiveNormalCount(), recovered.anomalous(), new BigDecimal("2500"));
        AnomalyEvaluationResult breach2 = evaluate(breach1.consecutiveBreachCount(),
                breach1.consecutiveNormalCount(), breach1.anomalous(), new BigDecimal("2500"));
        AnomalyEvaluationResult breach3 = evaluate(breach2.consecutiveBreachCount(),
                breach2.consecutiveNormalCount(), breach2.anomalous(), new BigDecimal("2500"));

        assertThat(breach3.anomalous()).isTrue();
        assertThat(breach3.transitionedToAnomalous()).isTrue();
    }

    @Test
    void honoursConfiguredBreachThresholdDifferentFromDefault() {
        AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(4, 0, false,
                new BigDecimal("2500"), SAFE_LIMIT, 5, RECOVERY_THRESHOLD, RECOVERY_RATIO);

        assertThat(result.consecutiveBreachCount()).isEqualTo(5);
        assertThat(result.anomalous()).isTrue();
        assertThat(result.transitionedToAnomalous()).isTrue();
    }
}
