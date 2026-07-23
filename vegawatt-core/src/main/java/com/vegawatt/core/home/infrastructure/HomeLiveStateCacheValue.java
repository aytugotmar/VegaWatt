package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;
import java.time.Instant;

class HomeLiveStateCacheValue {

    private String homeName;
    private BigDecimal currentEnergyKwh;
    private BigDecimal currentCost;
    private BigDecimal energyQuotaPercentage;
    private BigDecimal budgetQuotaPercentage;
    private TariffState tariffState;
    private boolean penaltyActive;
    private String billingPeriod;
    private Instant lastUpdatedAt;

    HomeLiveStateCacheValue() {
    }

    HomeLiveStateCacheValue(String homeName, BigDecimal currentEnergyKwh, BigDecimal currentCost,
                             BigDecimal energyQuotaPercentage, BigDecimal budgetQuotaPercentage,
                             TariffState tariffState, boolean penaltyActive, String billingPeriod,
                             Instant lastUpdatedAt) {
        this.homeName = homeName;
        this.currentEnergyKwh = currentEnergyKwh;
        this.currentCost = currentCost;
        this.energyQuotaPercentage = energyQuotaPercentage;
        this.budgetQuotaPercentage = budgetQuotaPercentage;
        this.tariffState = tariffState;
        this.penaltyActive = penaltyActive;
        this.billingPeriod = billingPeriod;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    String getHomeName() {
        return homeName;
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

    String getBillingPeriod() {
        return billingPeriod;
    }

    Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
