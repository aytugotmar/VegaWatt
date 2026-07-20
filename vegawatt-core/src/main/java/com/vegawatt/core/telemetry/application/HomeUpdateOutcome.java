package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.billing.domain.QuotaTransition;
import java.math.BigDecimal;

record HomeUpdateOutcome(
        BigDecimal energyIncrementKwh,
        BigDecimal costIncrement,
        QuotaTransition energyTransition,
        QuotaTransition budgetTransition) {
}
