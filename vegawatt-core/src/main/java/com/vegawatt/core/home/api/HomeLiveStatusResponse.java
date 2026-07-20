package com.vegawatt.core.home.api;

import com.vegawatt.core.home.application.HomeLiveStatus;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.Home;
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
            BigDecimal accumulatedEnergyKwh,
            int consecutiveBreachCount,
            boolean anomalous,
            Instant lastUpdatedAt) {

        static ApplianceLiveStatusResponse from(ApplianceLiveState state, Home home) {
            Appliance appliance = home.appliances().stream()
                    .filter(candidate -> candidate.id().equals(state.applianceId()))
                    .findFirst()
                    .orElse(null);
            String name = appliance != null ? appliance.name() : "Unknown";
            String type = appliance != null ? appliance.type() : "UNKNOWN";
            BigDecimal safePowerLimitWatt = appliance != null ? appliance.safePowerLimitWatt() : null;
            return new ApplianceLiveStatusResponse(state.applianceId(), name, type, safePowerLimitWatt,
                    state.currentPowerWatt(), state.accumulatedEnergyKwh(), state.consecutiveBreachCount(),
                    state.anomalous(), state.lastUpdatedAt());
        }
    }

    public static HomeLiveStatusResponse from(HomeLiveStatus status) {
        var state = status.liveState();
        return new HomeLiveStatusResponse(
                state.homeId(),
                status.home().name(),
                state.currentEnergyKwh(),
                state.currentCost().rounded(),
                state.energyQuotaPercentage(),
                state.budgetQuotaPercentage(),
                state.tariffState().name(),
                state.penaltyActive(),
                state.lastUpdatedAt(),
                status.appliances().stream()
                        .map(applianceState -> ApplianceLiveStatusResponse.from(applianceState, status.home()))
                        .toList());
    }
}
