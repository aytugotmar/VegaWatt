package com.vegawatt.core.home.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record RegisterHomeRequest(
        @NotBlank String name,
        @NotBlank @Email String contactEmail,
        @NotNull @Positive BigDecimal energyQuotaKwh,
        @NotNull @Positive BigDecimal budgetQuotaTry,
        @NotNull @Positive BigDecimal baseTariffPerKwh,
        @NotNull @Positive BigDecimal penaltyTariffPerKwh,
        @NotEmpty @Valid List<ApplianceRequest> appliances) {

    public record ApplianceRequest(
            @NotBlank String name,
            @NotBlank String type,
            @NotNull @Positive BigDecimal safePowerLimitWatt,
            @NotNull @PositiveOrZero BigDecimal simulationMinWatt,
            @NotNull @Positive BigDecimal simulationMaxWatt) {
    }
}
