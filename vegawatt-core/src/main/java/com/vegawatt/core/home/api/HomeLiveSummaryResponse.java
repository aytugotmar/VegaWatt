package com.vegawatt.core.home.api;

import com.vegawatt.core.home.application.HomeLiveSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HomeLiveSummaryResponse(
        UUID homeId,
        String homeName,
        BigDecimal currentEnergyKwh,
        BigDecimal currentCost,
        BigDecimal energyQuotaPercentage,
        BigDecimal budgetQuotaPercentage,
        String tariffState,
        boolean penaltyActive,
        Instant lastUpdatedAt) {

    public static HomeLiveSummaryResponse from(HomeLiveSummary summary) {
        var state = summary.liveState();
        return new HomeLiveSummaryResponse(state.homeId(), state.homeName(), state.currentEnergyKwh(),
                state.currentCost().rounded(), state.energyQuotaPercentage(), state.budgetQuotaPercentage(),
                state.tariffState().name(), state.penaltyActive(), state.lastUpdatedAt());
    }
}
