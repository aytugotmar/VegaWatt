package com.vegawatt.core.home.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssetRegistrationEventPayload(
        UUID eventId,
        int eventVersion,
        Instant occurredAt,
        HomePayload home,
        List<AppliancePayload> appliances) {

    public record HomePayload(
            UUID homeId,
            String name,
            String contactEmail,
            BigDecimal energyQuotaKwh,
            BigDecimal budgetQuotaTry,
            BigDecimal baseTariffPerKwh,
            BigDecimal penaltyTariffPerKwh) {
    }

    public record AppliancePayload(
            UUID applianceId,
            String name,
            String type,
            BigDecimal safePowerLimitWatt,
            BigDecimal simulationMinWatt,
            BigDecimal simulationMaxWatt) {
    }
}
