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
        Instant lastUpdatedAt,
        UUID lastEventId,
        long lastProcessedSequence,
        long stateVersion,
        String catalogCode,
        String catalogDisplayName,
        String catalogIconKey) {

    /** No catalog cosmetics — used by fallback/defensive paths that don't have the appliance's
     * catalog link in hand. Prefer the overload below whenever it's available. */
    public static ApplianceLiveState zero(UUID homeId, UUID applianceId, String applianceName, String applianceType,
                                           BigDecimal safePowerLimitWatt, Instant now) {
        return zero(homeId, applianceId, applianceName, applianceType, safePowerLimitWatt, now, null, null, null);
    }

    public static ApplianceLiveState zero(UUID homeId, UUID applianceId, String applianceName, String applianceType,
                                           BigDecimal safePowerLimitWatt, Instant now, String catalogCode,
                                           String catalogDisplayName, String catalogIconKey) {
        return new ApplianceLiveState(homeId, applianceId, applianceName, applianceType, safePowerLimitWatt,
                BigDecimal.ZERO, null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                ApplianceHealthStatus.NORMAL, now, null, 0L, 0L, catalogCode, catalogDisplayName, catalogIconKey);
    }

    /**
     * Only the Ignite adapters' {@code update()} methods should call this — they own stamping the
     * monotonic version on every mutation so {@code restore()}'s compare-and-swap can tell a stale
     * compensation from a legitimately newer write. Business logic constructing a new state should
     * just carry the existing value's {@code stateVersion} through unchanged.
     */
    public ApplianceLiveState withStateVersion(long stateVersion) {
        return new ApplianceLiveState(homeId, applianceId, applianceName, applianceType, safePowerLimitWatt,
                currentPowerWatt, operatingState, operatingMode, accumulatedEnergyKwh, consecutiveBreachCount,
                consecutiveNormalCount, anomalous, standbyBreachCount, standbyRecoveryCount, standbyAnomalyActive,
                telemetryHealthStatus, lastUpdatedAt, lastEventId, lastProcessedSequence, stateVersion, catalogCode,
                catalogDisplayName, catalogIconKey);
    }
}
