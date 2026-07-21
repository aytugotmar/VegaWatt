package com.vegawatt.core.notification.domain;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdvisoryContext(
        UUID homeId,
        String homeName,
        AdvisoryTriggerType triggerType,
        BigDecimal energyQuotaPercentage,
        BigDecimal budgetQuotaPercentage,
        Money currentCost,
        TariffState tariffState,
        List<String> anomalousApplianceNames,
        UUID triggerEventId) {
}
