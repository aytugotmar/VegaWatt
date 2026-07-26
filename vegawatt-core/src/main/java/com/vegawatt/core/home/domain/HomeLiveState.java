package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.time.BillingPeriodResolver;
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
        String billingPeriod,
        Instant lastUpdatedAt,
        UUID lastEventId,
        long stateVersion) {

    public static HomeLiveState zero(UUID homeId, String homeName, String billingPeriod, Instant now) {
        return new HomeLiveState(homeId, homeName, BigDecimal.ZERO.setScale(9), Money.zero(), BigDecimal.ZERO,
                BigDecimal.ZERO, TariffState.BASE, false, billingPeriod, now, null, 0L);
    }

    public static HomeLiveState zero(UUID homeId, String homeName, Instant now) {
        return zero(homeId, homeName, BillingPeriodResolver.currentPeriod(now), now);
    }

    /**
     * Only the Ignite adapters' {@code update()} methods should call this — they own stamping the
     * monotonic version on every mutation so {@code restore()}'s compare-and-swap can tell a stale
     * compensation from a legitimately newer write. Business logic constructing a new state should
     * just carry the existing value's {@code stateVersion} through unchanged.
     */
    public HomeLiveState withStateVersion(long stateVersion) {
        return new HomeLiveState(homeId, homeName, currentEnergyKwh, currentCost, energyQuotaPercentage,
                budgetQuotaPercentage, tariffState, penaltyActive, billingPeriod, lastUpdatedAt, lastEventId,
                stateVersion);
    }
}
