package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HomeLiveState(
        UUID homeId,
        String homeName,
        BigDecimal currentEnergyKwh,
        Money currentCost,
        BigDecimal energyQuotaPercentage,
        BigDecimal budgetQuotaPercentage,
        TariffState tariffState,
        boolean penaltyActive,
        Instant lastUpdatedAt) {

    public static HomeLiveState zero(UUID homeId, String homeName, Instant now) {
        return new HomeLiveState(homeId, homeName, BigDecimal.ZERO.setScale(9), Money.zero(), BigDecimal.ZERO,
                BigDecimal.ZERO, TariffState.BASE, false, now);
    }
}
