package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.home.domain.ApplianceOperatingState;
import java.math.BigDecimal;
import java.time.Instant;

class ApplianceLiveStateCacheValue {

    private String applianceName;
    private String applianceType;
    private BigDecimal safePowerLimitWatt;
    private BigDecimal currentPowerWatt;
    private ApplianceOperatingState operatingState;
    private String operatingMode;
    private BigDecimal accumulatedEnergyKwh;
    private int consecutiveBreachCount;
    private boolean anomalous;
    private int standbyBreachCount;
    private int standbyRecoveryCount;
    private boolean standbyAnomalyActive;
    private ApplianceHealthStatus telemetryHealthStatus;
    private Instant lastUpdatedAt;

    ApplianceLiveStateCacheValue() {
    }

    ApplianceLiveStateCacheValue(String applianceName, String applianceType, BigDecimal safePowerLimitWatt,
                                  BigDecimal currentPowerWatt, ApplianceOperatingState operatingState,
                                  String operatingMode, BigDecimal accumulatedEnergyKwh, int consecutiveBreachCount,
                                  boolean anomalous, int standbyBreachCount, int standbyRecoveryCount,
                                  boolean standbyAnomalyActive, ApplianceHealthStatus telemetryHealthStatus,
                                  Instant lastUpdatedAt) {
        this.applianceName = applianceName;
        this.applianceType = applianceType;
        this.safePowerLimitWatt = safePowerLimitWatt;
        this.currentPowerWatt = currentPowerWatt;
        this.operatingState = operatingState;
        this.operatingMode = operatingMode;
        this.accumulatedEnergyKwh = accumulatedEnergyKwh;
        this.consecutiveBreachCount = consecutiveBreachCount;
        this.anomalous = anomalous;
        this.standbyBreachCount = standbyBreachCount;
        this.standbyRecoveryCount = standbyRecoveryCount;
        this.standbyAnomalyActive = standbyAnomalyActive;
        this.telemetryHealthStatus = telemetryHealthStatus;
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

    ApplianceOperatingState getOperatingState() {
        return operatingState;
    }

    String getOperatingMode() {
        return operatingMode;
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

    int getStandbyBreachCount() {
        return standbyBreachCount;
    }

    int getStandbyRecoveryCount() {
        return standbyRecoveryCount;
    }

    boolean isStandbyAnomalyActive() {
        return standbyAnomalyActive;
    }

    ApplianceHealthStatus getTelemetryHealthStatus() {
        return telemetryHealthStatus;
    }

    Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
