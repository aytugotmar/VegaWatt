package com.vegawatt.core.home.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegisterHomeCommand(
        String name,
        String contactEmail,
        BigDecimal energyQuotaKwh,
        BigDecimal budgetQuotaTry,
        BigDecimal baseTariffPerKwh,
        BigDecimal penaltyTariffPerKwh,
        List<ApplianceCommand> appliances,
        UUID ownerUserId) {

    public record ApplianceCommand(
            String name,
            String type,
            BigDecimal safePowerLimitWatt,
            BigDecimal simulationMinWatt,
            BigDecimal simulationMaxWatt,
            UUID catalogItemId) {
    }
}
