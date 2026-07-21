package com.vegawatt.core.history.domain;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConsumptionSnapshot(
        UUID id,
        UUID homeId,
        Instant snapshotTime,
        BigDecimal accumulatedEnergyKwh,
        Money accumulatedCost,
        TariffState tariffState) {

    public static ConsumptionSnapshot create(UUID homeId, Instant snapshotTime, BigDecimal accumulatedEnergyKwh,
                                              Money accumulatedCost, TariffState tariffState) {
        return new ConsumptionSnapshot(UUID.randomUUID(), homeId, snapshotTime, accumulatedEnergyKwh,
                accumulatedCost, tariffState);
    }
}
