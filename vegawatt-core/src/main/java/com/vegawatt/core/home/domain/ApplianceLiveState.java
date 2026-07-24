package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.ApplianceHealthStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplianceLiveState(
        UUID homeId,
        UUID applianceId,
        String applianceName,
        String applianceType,
        BigDecimal safePowerLimitWatt,
        BigDecimal currentPowerWatt,
        ApplianceOperatingState operatingState,
        String operatingMode,
        BigDecimal accumulatedEnergyKwh,
        int consecutiveBreachCount,
        int consecutiveNormalCount,
        boolean anomalous,
        int standbyBreachCount,
        int standbyRecoveryCount,
        boolean standbyAnomalyActive,
        ApplianceHealthStatus telemetryHealthStatus,
        Instant lastUpdatedAt) {

    public static ApplianceLiveState zero(UUID homeId, UUID applianceId, String applianceName, String applianceType,
                                           BigDecimal safePowerLimitWatt, Instant now) {
        return new ApplianceLiveState(homeId, applianceId, applianceName, applianceType, safePowerLimitWatt,
                BigDecimal.ZERO, null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                ApplianceHealthStatus.NORMAL, now);
    }
}
