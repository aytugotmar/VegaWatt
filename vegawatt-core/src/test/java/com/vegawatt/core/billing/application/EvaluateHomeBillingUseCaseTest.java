package com.vegawatt.core.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluateHomeBillingUseCaseTest {

    private BillingAccountRepository billingAccountRepository;
    private EvaluateHomeBillingUseCase evaluateHomeBillingUseCase;

    private final UUID homeId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-01T10:00:00Z");
    private Home home;

    @BeforeEach
    void setUp() {
        billingAccountRepository = mock(BillingAccountRepository.class);
        evaluateHomeBillingUseCase = new EvaluateHomeBillingUseCase(billingAccountRepository);

        home = Home.register("Test Home", "owner@example.com", new BigDecimal("100"), new BigDecimal("500"),
                new BigDecimal("2.50"), new BigDecimal("5.00"), now);
    }

    @Test
    void evaluate_sameBillingPeriod_accumulatesEnergyAndCost() {
        String currentPeriod = BillingPeriodResolver.currentPeriod(now);
        HomeLiveState current = new HomeLiveState(homeId, "Test Home", new BigDecimal("10.00"), Money.of(new BigDecimal("25.00")),
                new BigDecimal("10.00"), new BigDecimal("5.00"), TariffState.BASE, false, currentPeriod, now, null, 0L);

        HomeBillingEvaluation evaluation = evaluateHomeBillingUseCase.evaluate(home, current, new BigDecimal("5.00"), now,
                UUID.randomUUID());

        assertThat(evaluation.newState().billingPeriod()).isEqualTo(currentPeriod);
        assertThat(evaluation.newState().currentEnergyKwh()).isEqualByComparingTo("15.00");
        assertThat(evaluation.newState().currentCost()).isEqualTo(Money.of(new BigDecimal("37.50")));
        assertThat(evaluation.newState().tariffState()).isEqualTo(TariffState.BASE);
    }

    @Test
    void evaluate_billingPeriodRollover_resetsAccumulatedTotalsForNewMonth() {
        String oldPeriod = "2026-07";
        String newPeriod = BillingPeriodResolver.currentPeriod(now); // 2026-08

        // Previous month was in penalty state with high energy and cost
        HomeLiveState oldState = new HomeLiveState(homeId, "Test Home", new BigDecimal("150.00"), Money.of(new BigDecimal("750.00")),
                new BigDecimal("150.00"), new BigDecimal("150.00"), TariffState.PENALTY, true, oldPeriod, now, null, 0L);

        when(billingAccountRepository.findByHomeIdAndBillingPeriod(eq(home.id()), eq(newPeriod)))
                .thenReturn(Optional.empty());

        HomeBillingEvaluation evaluation = evaluateHomeBillingUseCase.evaluate(home, oldState, new BigDecimal("2.00"), now,
                UUID.randomUUID());

        assertThat(evaluation.newState().billingPeriod()).isEqualTo(newPeriod);
        // Energy starts fresh for August: 0 + 2 = 2 kWh
        assertThat(evaluation.newState().currentEnergyKwh()).isEqualByComparingTo("2.00");
        // Cost starts fresh for August with BASE tariff: 2 * 2.50 = 5.00 TRY
        assertThat(evaluation.newState().currentCost()).isEqualTo(Money.of(new BigDecimal("5.00")));
        // Penalty status resets to BASE for the new billing period
        assertThat(evaluation.newState().tariffState()).isEqualTo(TariffState.BASE);
        assertThat(evaluation.newState().penaltyActive()).isFalse();
    }
}
