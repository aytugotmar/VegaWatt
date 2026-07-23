package com.vegawatt.core.home.api;

import com.vegawatt.core.home.domain.Home;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RegisterHomeResponse(
        UUID homeId,
        String name,
        String contactEmail,
        BigDecimal energyQuotaKwh,
        BigDecimal budgetQuotaTry,
        BigDecimal baseTariffPerKwh,
        BigDecimal penaltyTariffPerKwh,
        Instant createdAt,
        List<ApplianceResponse> appliances) {

    public record ApplianceResponse(
            UUID applianceId,
            String name,
            String type,
            BigDecimal safePowerLimitWatt,
            BigDecimal simulationMinWatt,
            BigDecimal simulationMaxWatt,
            UUID catalogItemId) {
    }

    public static RegisterHomeResponse from(Home home) {
        List<ApplianceResponse> appliances = home.appliances().stream()
                .map(appliance -> new ApplianceResponse(appliance.id(), appliance.name(), appliance.type(),
                        appliance.safePowerLimitWatt(), appliance.simulationMinWatt(), appliance.simulationMaxWatt(),
                        appliance.catalogItemId()))
                .toList();
        return new RegisterHomeResponse(home.id(), home.name(), home.contactEmail(), home.energyQuotaKwh(),
                home.budgetQuotaTry(), home.baseTariffPerKwh(), home.penaltyTariffPerKwh(), home.createdAt(),
                appliances);
    }
}
