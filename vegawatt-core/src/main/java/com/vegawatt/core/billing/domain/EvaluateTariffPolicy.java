package com.vegawatt.core.billing.domain;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;

public final class EvaluateTariffPolicy {

    private EvaluateTariffPolicy() {
    }

    public static Money cost(BigDecimal energyKwh, TariffState activeTariffState, BigDecimal baseTariffPerKwh,
                              BigDecimal penaltyTariffPerKwh) {
        BigDecimal ratePerKwh = activeTariffState == TariffState.PENALTY ? penaltyTariffPerKwh : baseTariffPerKwh;
        return Money.of(energyKwh.multiply(ratePerKwh));
    }
}
