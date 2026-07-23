package com.vegawatt.sensors.registration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record AssetRegistrationEventPayload(
        UUID eventId,
        int eventVersion,
        Instant occurredAt,
        HomePayload home,
        List<AppliancePayload> appliances) {

    record HomePayload(
            UUID homeId,
            String name,
            String contactEmail,
            BigDecimal energyQuotaKwh,
            BigDecimal budgetQuotaTry,
            BigDecimal baseTariffPerKwh,
            BigDecimal penaltyTariffPerKwh) {
    }

    record AppliancePayload(
            UUID applianceId,
            String name,
            String type,
            BigDecimal safePowerLimitWatt,
            BigDecimal simulationMinWatt,
            BigDecimal simulationMaxWatt,
            String catalogCode,
            String behaviorProfile,
            BigDecimal standbyMinWatt,
            BigDecimal standbyMaxWatt) {
    }
}
