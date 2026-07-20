package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;
import java.time.Instant;

class HomeLiveStateCacheValue {

    private BigDecimal currentEnergyKwh;
    private BigDecimal currentCost;
    private BigDecimal energyQuotaPercentage;
    private BigDecimal budgetQuotaPercentage;
    private TariffState tariffState;
    private boolean penaltyActive;
    private Instant lastUpdatedAt;

    HomeLiveStateCacheValue() {
    }

    HomeLiveStateCacheValue(BigDecimal currentEnergyKwh, BigDecimal currentCost, BigDecimal energyQuotaPercentage,
                             BigDecimal budgetQuotaPercentage, TariffState tariffState, boolean penaltyActive,
                             Instant lastUpdatedAt) {
        this.currentEnergyKwh = currentEnergyKwh;
        this.currentCost = currentCost;
        this.energyQuotaPercentage = energyQuotaPercentage;
        this.budgetQuotaPercentage = budgetQuotaPercentage;
        this.tariffState = tariffState;
        this.penaltyActive = penaltyActive;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    BigDecimal getCurrentEnergyKwh() {
        return currentEnergyKwh;
    }

    BigDecimal getCurrentCost() {
        return currentCost;
    }

    BigDecimal getEnergyQuotaPercentage() {
        return energyQuotaPercentage;
    }

    BigDecimal getBudgetQuotaPercentage() {
        return budgetQuotaPercentage;
    }

    TariffState getTariffState() {
        return tariffState;
    }

    boolean isPenaltyActive() {
        return penaltyActive;
    }

    Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
