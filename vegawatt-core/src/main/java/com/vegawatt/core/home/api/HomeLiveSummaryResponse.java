package com.vegawatt.core.home.api;

import com.vegawatt.core.home.domain.HomeLiveState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HomeLiveSummaryResponse(
        UUID homeId,
        BigDecimal currentEnergyKwh,
        BigDecimal currentCost,
        BigDecimal energyQuotaPercentage,
        BigDecimal budgetQuotaPercentage,
        String tariffState,
        boolean penaltyActive,
        Instant lastUpdatedAt) {

    public static HomeLiveSummaryResponse from(HomeLiveState state) {
        return new HomeLiveSummaryResponse(state.homeId(), state.currentEnergyKwh(), state.currentCost().amount(),
                state.energyQuotaPercentage(), state.budgetQuotaPercentage(), state.tariffState().name(),
                state.penaltyActive(), state.lastUpdatedAt());
    }
}
