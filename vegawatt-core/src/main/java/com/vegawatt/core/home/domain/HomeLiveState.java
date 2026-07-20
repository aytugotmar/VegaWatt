package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HomeLiveState(
        UUID homeId,
        BigDecimal currentEnergyKwh,
        Money currentCost,
        BigDecimal energyQuotaPercentage,
        BigDecimal budgetQuotaPercentage,
        TariffState tariffState,
        boolean penaltyActive,
        Instant lastUpdatedAt) {

    public static HomeLiveState zero(UUID homeId, Instant now) {
        return new HomeLiveState(homeId, BigDecimal.ZERO.setScale(4), Money.zero(), BigDecimal.ZERO,
                BigDecimal.ZERO, TariffState.BASE, false, now);
    }
}
