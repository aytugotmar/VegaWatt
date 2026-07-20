package com.vegawatt.core.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EvaluateTariffPolicyTest {

    private static final BigDecimal BASE_TARIFF = new BigDecimal("2.10");
    private static final BigDecimal PENALTY_TARIFF = new BigDecimal("3.50");

    @Test
    void usesBaseTariffWhenNotInPenalty() {
        Money cost = EvaluateTariffPolicy.cost(new BigDecimal("10"), TariffState.BASE, BASE_TARIFF, PENALTY_TARIFF);

        assertThat(cost.amount()).isEqualByComparingTo("21.00");
    }

    @Test
    void usesPenaltyTariffWhenPenaltyActive() {
        Money cost = EvaluateTariffPolicy.cost(new BigDecimal("10"), TariffState.PENALTY, BASE_TARIFF,
                PENALTY_TARIFF);

        assertThat(cost.amount()).isEqualByComparingTo("35.00");
    }

    @Test
    void roundsToTwoDecimalPlacesHalfUp() {
        Money cost = EvaluateTariffPolicy.cost(new BigDecimal("0.001"), TariffState.BASE, BASE_TARIFF,
                PENALTY_TARIFF);

        assertThat(cost.amount()).isEqualByComparingTo("0.00");
        assertThat(cost.amount().scale()).isEqualTo(2);
    }
}
