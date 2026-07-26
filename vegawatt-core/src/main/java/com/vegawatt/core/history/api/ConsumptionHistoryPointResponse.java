package com.vegawatt.core.history.api;

import com.vegawatt.core.history.domain.ConsumptionSnapshot;
import java.math.BigDecimal;
import java.time.Instant;

public record ConsumptionHistoryPointResponse(
        Instant snapshotTime,
        BigDecimal energyKwh,
        BigDecimal cost,
        String tariffState,
        String billingPeriod) {

    public static ConsumptionHistoryPointResponse from(ConsumptionSnapshot snapshot) {
        return new ConsumptionHistoryPointResponse(snapshot.snapshotTime(), snapshot.accumulatedEnergyKwh(),
                snapshot.accumulatedCost().rounded(), snapshot.tariffState().name(), snapshot.billingPeriod());
    }
}
