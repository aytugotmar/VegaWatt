package com.vegawatt.core.billing.application;

import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.billing.domain.EvaluateQuotaPolicy;
import com.vegawatt.core.billing.domain.EvaluateTariffPolicy;
import com.vegawatt.core.billing.domain.QuotaTransition;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Application-layer entry point for the Tariff and Anomaly Rules module's home-level
 * responsibility: quota percentage tracking, 80/100% transitions and dynamic penalty
 * tariff activation. Also owns recovery of a cold Ignite entry from the permanent
 * Postgres billing ledger (the single source of truth for accumulated totals).
 */
@Service
public class EvaluateHomeBillingUseCase {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int PERCENTAGE_SCALE = 2;

    private final BillingAccountRepository billingAccountRepository;

    public EvaluateHomeBillingUseCase(BillingAccountRepository billingAccountRepository) {
        this.billingAccountRepository = billingAccountRepository;
    }

    public HomeBillingEvaluation evaluate(Home home, HomeLiveState current, BigDecimal energyIncrementKwh,
                                           Instant now) {
        String currentPeriod = BillingPeriodResolver.currentPeriod(now);
        HomeLiveState existing;
        if (current == null || current.billingPeriod() == null || !currentPeriod.equals(current.billingPeriod())) {
            existing = recoverFromLedger(home, now);
        } else {
            existing = current;
        }

        Money costIncrement = EvaluateTariffPolicy.cost(energyIncrementKwh, existing.tariffState(),
                home.baseTariffPerKwh(), home.penaltyTariffPerKwh());

        BigDecimal newEnergyKwh = existing.currentEnergyKwh().add(energyIncrementKwh);
        Money newCost = existing.currentCost().plus(costIncrement);
        BigDecimal newEnergyPercentage = percentage(newEnergyKwh, home.energyQuotaKwh());
        BigDecimal newBudgetPercentage = percentage(newCost.amount(), home.budgetQuotaTry());

        QuotaTransition energyTransition = EvaluateQuotaPolicy.evaluate(existing.energyQuotaPercentage(),
                newEnergyPercentage);
        QuotaTransition budgetTransition = EvaluateQuotaPolicy.evaluate(existing.budgetQuotaPercentage(),
                newBudgetPercentage);

        TariffState newTariffState = existing.tariffState() == TariffState.PENALTY || budgetTransition.crossed100()
                ? TariffState.PENALTY
                : TariffState.BASE;

        HomeLiveState newState = new HomeLiveState(home.id(), home.name(), newEnergyKwh, newCost,
                newEnergyPercentage, newBudgetPercentage, newTariffState, newTariffState == TariffState.PENALTY,
                currentPeriod, now);
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(energyIncrementKwh, costIncrement.amount(),
                energyTransition, budgetTransition);

        return new HomeBillingEvaluation(newState, outcome);
    }

    /**
     * Rebuilds a home's live state from Postgres's permanent billing ledger — used both when
     * Ignite's cache entry is cold, and to compensate Ignite back to Postgres's last-committed
     * truth after a failed persist (see ProcessTelemetryUseCase).
     */
    public HomeLiveState recoverFromLedger(Home home, Instant now) {
        String currentPeriod = BillingPeriodResolver.currentPeriod(now);
        return billingAccountRepository.findByHomeIdAndBillingPeriod(home.id(), currentPeriod)
                .map(billingAccount -> {
                    TariffState tariffState = billingAccount.penaltyActive() ? TariffState.PENALTY : TariffState.BASE;
                    BigDecimal energyPercentage = percentage(billingAccount.accumulatedEnergyKwh(),
                            home.energyQuotaKwh());
                    BigDecimal budgetPercentage = percentage(billingAccount.accumulatedCost().amount(),
                            home.budgetQuotaTry());
                    return new HomeLiveState(home.id(), home.name(), billingAccount.accumulatedEnergyKwh(),
                            billingAccount.accumulatedCost(), energyPercentage, budgetPercentage, tariffState,
                            billingAccount.penaltyActive(), currentPeriod, now);
                })
                .orElseGet(() -> HomeLiveState.zero(home.id(), home.name(), currentPeriod, now));
    }

    private static BigDecimal percentage(BigDecimal value, BigDecimal quota) {
        return value.multiply(ONE_HUNDRED).divide(quota, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }
}
