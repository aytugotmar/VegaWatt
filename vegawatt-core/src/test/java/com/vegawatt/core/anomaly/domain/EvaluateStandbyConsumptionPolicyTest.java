package com.vegawatt.core.anomaly.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.home.domain.ApplianceOperatingState;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EvaluateStandbyConsumptionPolicyTest {

    private static final BigDecimal STANDBY_MAX_WATT = new BigDecimal("3");
    private static final BigDecimal THRESHOLD_MULTIPLIER = new BigDecimal("3");
    private static final BigDecimal MINIMUM_EXCESS_WATT = new BigDecimal("2");
    private static final int BREACH_THRESHOLD = 3;
    private static final int RECOVERY_THRESHOLD = 3;
    // With STANDBY_MAX_WATT=3: multiplied=9, additive=5 -> calculatedThresholdWatt=9.
    private static final BigDecimal ABOVE_THRESHOLD = new BigDecimal("10");
    private static final BigDecimal AT_STANDBY_MAX = new BigDecimal("3");

    private static StandbyAnomalyEvaluationResult evaluate(int previousBreachCount, int previousRecoveryCount,
                                                             boolean previouslyActive, BigDecimal currentPowerWatt) {
        return EvaluateStandbyConsumptionPolicy.evaluate(previousBreachCount, previousRecoveryCount, previouslyActive,
                currentPowerWatt, STANDBY_MAX_WATT, THRESHOLD_MULTIPLIER, MINIMUM_EXCESS_WATT, BREACH_THRESHOLD,
                RECOVERY_THRESHOLD);
    }

    @Test
    void isEligibleRequiresStandbyOperatingStateStandbyDeviceProfileAndNonNullStandbyMaxWatt() {
        assertThat(EvaluateStandbyConsumptionPolicy.isEligible(ApplianceOperatingState.STANDBY,
                ApplianceBehaviorProfile.STANDBY_DEVICE, STANDBY_MAX_WATT)).isTrue();

        assertThat(EvaluateStandbyConsumptionPolicy.isEligible(ApplianceOperatingState.ACTIVE,
                ApplianceBehaviorProfile.STANDBY_DEVICE, STANDBY_MAX_WATT)).isFalse();
        assertThat(EvaluateStandbyConsumptionPolicy.isEligible(ApplianceOperatingState.STANDBY,
                ApplianceBehaviorProfile.ALWAYS_ON_STABLE, STANDBY_MAX_WATT)).isFalse();
        assertThat(EvaluateStandbyConsumptionPolicy.isEligible(ApplianceOperatingState.STANDBY,
                ApplianceBehaviorProfile.STANDBY_DEVICE, null)).isFalse();
        // v1 telemetry (no operating state) must never be inferred as STANDBY from wattage alone.
        assertThat(EvaluateStandbyConsumptionPolicy.isEligible(null, ApplianceBehaviorProfile.STANDBY_DEVICE,
                STANDBY_MAX_WATT)).isFalse();
    }

    @Test
    void thresholdIsTheLargerOfTheMultiplierAndAdditiveFormulas() {
        // multiplied = 3*3=9, additive = 3+2=5 -> 9 wins.
        assertThat(EvaluateStandbyConsumptionPolicy.calculateThresholdWatt(STANDBY_MAX_WATT, THRESHOLD_MULTIPLIER,
                MINIMUM_EXCESS_WATT)).isEqualByComparingTo("9");

        // For a tiny standbyMaxWatt (0.5W), multiplied=1.5, additive=2.5 -> excess formula wins.
        assertThat(EvaluateStandbyConsumptionPolicy.calculateThresholdWatt(new BigDecimal("0.5"),
                THRESHOLD_MULTIPLIER, MINIMUM_EXCESS_WATT)).isEqualByComparingTo("2.5");
    }

    @Test
    void consecutiveAbnormalReadingsIncrementTheBreachCounterAndANormalReadingResetsIt() {
        StandbyAnomalyEvaluationResult first = evaluate(0, 0, false, ABOVE_THRESHOLD);
        assertThat(first.standbyBreachCount()).isEqualTo(1);
        assertThat(first.standbyAnomalyActive()).isFalse();

        StandbyAnomalyEvaluationResult resetByNormalReading = evaluate(first.standbyBreachCount(), 0, false,
                AT_STANDBY_MAX);
        assertThat(resetByNormalReading.standbyBreachCount()).isZero();
    }

    @Test
    void reachingTheBreachThresholdActivatesOnlyOnTheExactTransitionTick() {
        StandbyAnomalyEvaluationResult afterOne = evaluate(0, 0, false, ABOVE_THRESHOLD);
        StandbyAnomalyEvaluationResult afterTwo = evaluate(afterOne.standbyBreachCount(), 0, false, ABOVE_THRESHOLD);
        StandbyAnomalyEvaluationResult afterThree = evaluate(afterTwo.standbyBreachCount(), 0, false,
                ABOVE_THRESHOLD);

        assertThat(afterOne.transitionedToActive()).isFalse();
        assertThat(afterTwo.transitionedToActive()).isFalse();
        assertThat(afterThree.transitionedToActive()).isTrue();
        assertThat(afterThree.standbyAnomalyActive()).isTrue();

        // A further abnormal reading stays active but must not re-fire the transition (dedup).
        StandbyAnomalyEvaluationResult afterFour = evaluate(afterThree.standbyBreachCount(),
                afterThree.standbyRecoveryCount(), true, ABOVE_THRESHOLD);
        assertThat(afterFour.transitionedToActive()).isFalse();
        assertThat(afterFour.standbyAnomalyActive()).isTrue();
    }

    @Test
    void midBandReadingBetweenRecoveryAndDetectionThresholdsNeitherActivatesNorRecovers() {
        // 5W: above standbyMaxWatt (3, the recovery bound) but below calculatedThresholdWatt (9).
        BigDecimal midBand = new BigDecimal("5");

        StandbyAnomalyEvaluationResult whileInactive = evaluate(2, 0, false, midBand);
        assertThat(whileInactive.standbyBreachCount()).isZero();
        assertThat(whileInactive.standbyAnomalyActive()).isFalse();

        StandbyAnomalyEvaluationResult whileActive = evaluate(0, 2, true, midBand);
        assertThat(whileActive.standbyRecoveryCount()).isZero();
        assertThat(whileActive.standbyAnomalyActive()).isTrue();
        assertThat(whileActive.transitionedToRecovered()).isFalse();
    }

    @Test
    void recoveryRequiresConsecutiveReadingsAtOrBelowStandbyMaxWattWithHysteresis() {
        StandbyAnomalyEvaluationResult afterOne = evaluate(0, 0, true, AT_STANDBY_MAX);
        StandbyAnomalyEvaluationResult afterTwo = evaluate(0, afterOne.standbyRecoveryCount(), true, AT_STANDBY_MAX);
        StandbyAnomalyEvaluationResult afterThree = evaluate(0, afterTwo.standbyRecoveryCount(), true,
                AT_STANDBY_MAX);

        assertThat(afterOne.transitionedToRecovered()).isFalse();
        assertThat(afterTwo.transitionedToRecovered()).isFalse();
        assertThat(afterThree.transitionedToRecovered()).isTrue();
        assertThat(afterThree.standbyAnomalyActive()).isFalse();
        assertThat(afterThree.standbyBreachCount())
                .as("breach counter must reset so a fresh detection cycle is required after recovery")
                .isZero();
    }
}
