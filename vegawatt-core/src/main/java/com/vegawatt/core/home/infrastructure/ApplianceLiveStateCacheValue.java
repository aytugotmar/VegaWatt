package com.vegawatt.core.home.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;

class ApplianceLiveStateCacheValue {

    private String applianceName;
    private String applianceType;
    private BigDecimal safePowerLimitWatt;
    private BigDecimal currentPowerWatt;
    private BigDecimal accumulatedEnergyKwh;
    private int consecutiveBreachCount;
    private boolean anomalous;
    private Instant lastUpdatedAt;

    ApplianceLiveStateCacheValue() {
    }

    ApplianceLiveStateCacheValue(String applianceName, String applianceType, BigDecimal safePowerLimitWatt,
                                  BigDecimal currentPowerWatt, BigDecimal accumulatedEnergyKwh,
                                  int consecutiveBreachCount, boolean anomalous, Instant lastUpdatedAt) {
        this.applianceName = applianceName;
        this.applianceType = applianceType;
        this.safePowerLimitWatt = safePowerLimitWatt;
        this.currentPowerWatt = currentPowerWatt;
        this.accumulatedEnergyKwh = accumulatedEnergyKwh;
        this.consecutiveBreachCount = consecutiveBreachCount;
        this.anomalous = anomalous;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    String getApplianceName() {
        return applianceName;
    }

    String getApplianceType() {
        return applianceType;
    }

    BigDecimal getSafePowerLimitWatt() {
        return safePowerLimitWatt;
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
