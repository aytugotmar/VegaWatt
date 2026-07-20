package com.vegawatt.core.home.api;

import com.vegawatt.core.home.application.HomeLiveStatus;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HomeLiveStatusResponse(
        UUID homeId,
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
            BigDecimal currentPowerWatt,
            BigDecimal accumulatedEnergyKwh,
            int consecutiveBreachCount,
            boolean anomalous,
            Instant lastUpdatedAt) {

        static ApplianceLiveStatusResponse from(ApplianceLiveState state) {
            return new ApplianceLiveStatusResponse(state.applianceId(), state.currentPowerWatt(),
                    state.accumulatedEnergyKwh(), state.consecutiveBreachCount(), state.anomalous(),
                    state.lastUpdatedAt());
        }
    }

    public static HomeLiveStatusResponse from(HomeLiveStatus status) {
        return new HomeLiveStatusResponse(
                status.home().homeId(),
                status.home().currentEnergyKwh(),
                status.home().currentCost().amount(),
                status.home().energyQuotaPercentage(),
                status.home().budgetQuotaPercentage(),
                status.home().tariffState().name(),
                status.home().penaltyActive(),
                status.home().lastUpdatedAt(),
                status.appliances().stream().map(ApplianceLiveStatusResponse::from).toList());
    }
}
