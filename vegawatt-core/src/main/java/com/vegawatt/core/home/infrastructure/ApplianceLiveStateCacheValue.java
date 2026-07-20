package com.vegawatt.core.home.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;

class ApplianceLiveStateCacheValue {

    private BigDecimal currentPowerWatt;
    private BigDecimal accumulatedEnergyKwh;
    private int consecutiveBreachCount;
    private boolean anomalous;
    private Instant lastUpdatedAt;

    ApplianceLiveStateCacheValue() {
    }

    ApplianceLiveStateCacheValue(BigDecimal currentPowerWatt, BigDecimal accumulatedEnergyKwh,
                                  int consecutiveBreachCount, boolean anomalous, Instant lastUpdatedAt) {
        this.currentPowerWatt = currentPowerWatt;
        this.accumulatedEnergyKwh = accumulatedEnergyKwh;
        this.consecutiveBreachCount = consecutiveBreachCount;
        this.anomalous = anomalous;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    BigDecimal getCurrentPowerWatt() {
        return currentPowerWatt;
    }

    BigDecimal getAccumulatedEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    int getConsecutiveBreachCount() {
        return consecutiveBreachCount;
    }

    boolean isAnomalous() {
        return anomalous;
    }

    Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
