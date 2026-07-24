package com.vegawatt.core.home.api;

import com.vegawatt.core.home.application.ApplianceCatalogView;
import com.vegawatt.core.home.application.HomeLiveStatus;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HomeLiveStatusResponse(
        UUID homeId,
        String homeName,
        BigDecimal currentEnergyKwh,
        BigDecimal currentCost,
        BigDecimal energyQuotaPercentage,
        BigDecimal budgetQuotaPercentage,
        String tariffState,
        boolean penaltyActive,
        Instant lastUpdatedAt,
        List<ApplianceLiveStatusResponse> appliances) {

    public record ApplianceLiveStatusResponse(
            UUID applianceId,
            String applianceName,
            String applianceType,
            BigDecimal safePowerLimitWatt,
            BigDecimal currentPowerWatt,
            String operatingState,
            String operatingMode,
            BigDecimal accumulatedEnergyKwh,
            int consecutiveBreachCount,
            boolean anomalous,
            boolean standbyAnomalyActive,
            String telemetryHealthStatus,
            String catalogCode,
            String catalogDisplayName,
            String catalogIconKey,
            Instant lastUpdatedAt) {

        static ApplianceLiveStatusResponse from(ApplianceLiveState state, ApplianceCatalogView catalogView) {
            return new ApplianceLiveStatusResponse(state.applianceId(), state.applianceName(), state.applianceType(),
                    state.safePowerLimitWatt(), state.currentPowerWatt(),
                    state.operatingState() == null ? null : state.operatingState().name(), state.operatingMode(),
                    state.accumulatedEnergyKwh(), state.consecutiveBreachCount(), state.anomalous(),
                    state.standbyAnomalyActive(),
                    state.telemetryHealthStatus() == null ? null : state.telemetryHealthStatus().name(),
                    catalogView.catalogCode(), catalogView.catalogDisplayName(), catalogView.catalogIconKey(),
                    state.lastUpdatedAt());
        }
    }

    public static HomeLiveStatusResponse from(HomeLiveStatus status) {
        var state = status.liveState();
        return new HomeLiveStatusResponse(
                state.homeId(),
                state.homeName(),
                state.currentEnergyKwh(),
                state.currentCost().rounded(),
                state.energyQuotaPercentage(),
                state.budgetQuotaPercentage(),
                state.tariffState().name(),
                state.penaltyActive(),
                state.lastUpdatedAt(),
                status.appliances().stream()
                        .map(applianceState -> ApplianceLiveStatusResponse.from(applianceState,
                                status.catalogInfoByApplianceId()
                                        .getOrDefault(applianceState.applianceId(), ApplianceCatalogView.EMPTY)))
                        .toList());
    }
}
