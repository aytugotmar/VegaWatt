package com.vegawatt.core.home.domain;

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
        BigDecimal accumulatedEnergyKwh,
        int consecutiveBreachCount,
        boolean anomalous,
        Instant lastUpdatedAt) {

    public static ApplianceLiveState zero(UUID homeId, UUID applianceId, String applianceName, String applianceType,
                                           BigDecimal safePowerLimitWatt, Instant now) {
        return new ApplianceLiveState(homeId, applianceId, applianceName, applianceType, safePowerLimitWatt,
                BigDecimal.ZERO, BigDecimal.ZERO.setScale(9), 0, false, now);
    }
}
