package com.vegawatt.core.billing.application;

import com.vegawatt.core.billing.domain.QuotaTransition;
import java.math.BigDecimal;

public record HomeUpdateOutcome(
        BigDecimal energyIncrementKwh,
        BigDecimal costIncrement,
        QuotaTransition energyTransition,
        QuotaTransition budgetTransition) {
}
