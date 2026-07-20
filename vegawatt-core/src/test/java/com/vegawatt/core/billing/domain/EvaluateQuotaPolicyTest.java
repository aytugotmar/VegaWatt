package com.vegawatt.core.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EvaluateQuotaPolicyTest {

    @Test
    void detectsCrossing80PercentThreshold() {
        QuotaTransition transition = EvaluateQuotaPolicy.evaluate(new BigDecimal("75"), new BigDecimal("82"));

        assertThat(transition.crossed80()).isTrue();
        assertThat(transition.crossed100()).isFalse();
    }

    @Test
    void detectsCrossing100PercentThreshold() {
        QuotaTransition transition = EvaluateQuotaPolicy.evaluate(new BigDecimal("97"), new BigDecimal("103"));

        assertThat(transition.crossed80()).isFalse();
        assertThat(transition.crossed100()).isTrue();
    }

    @Test
    void detectsBothThresholdsCrossedInOneJump() {
        QuotaTransition transition = EvaluateQuotaPolicy.evaluate(new BigDecimal("60"), new BigDecimal("110"));

        assertThat(transition.crossed80()).isTrue();
        assertThat(transition.crossed100()).isTrue();
    }

    @Test
    void doesNotReportDuplicateTransitionWhenAlreadyAboveThreshold() {
        QuotaTransition transition = EvaluateQuotaPolicy.evaluate(new BigDecimal("85"), new BigDecimal("90"));

        assertThat(transition.crossed80()).isFalse();
        assertThat(transition.crossed100()).isFalse();
    }

    @Test
    void doesNotCrossWhenStayingBelowThreshold() {
        QuotaTransition transition = EvaluateQuotaPolicy.evaluate(new BigDecimal("40"), new BigDecimal("55"));

        assertThat(transition.crossed80()).isFalse();
        assertThat(transition.crossed100()).isFalse();
    }
}
