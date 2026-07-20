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
    void preservesSubKurusPrecisionForSmallEnergyIncrements() {
        Money cost = EvaluateTariffPolicy.cost(new BigDecimal("0.002"), TariffState.BASE, BASE_TARIFF,
                PENALTY_TARIFF);

        assertThat(cost.amount()).isEqualByComparingTo("0.0042");
        assertThat(cost.amount().scale()).isEqualTo(6);
        assertThat(cost.rounded()).isEqualByComparingTo("0.00");
    }

    @Test
    void accumulatingManySubKurusCostsProducesCorrectRoundedTotal() {
        Money total = Money.zero();
        Money perEventCost = EvaluateTariffPolicy.cost(new BigDecimal("0.002"), TariffState.BASE, BASE_TARIFF,
                PENALTY_TARIFF);

        for (int i = 0; i < 500; i++) {
            total = total.plus(perEventCost);
        }

        assertThat(total.rounded()).isEqualByComparingTo("2.10");
    }
}
