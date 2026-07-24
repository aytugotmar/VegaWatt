package com.vegawatt.core.home.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record AddApplianceRequest(
        @NotBlank String name,
        @NotBlank String type,
        @Positive BigDecimal safePowerLimitWatt,
        @PositiveOrZero BigDecimal simulationMinWatt,
        @Positive BigDecimal simulationMaxWatt,
        UUID catalogItemId) {

    @AssertTrue(message = "safePowerLimitWatt, simulationMinWatt and simulationMaxWatt are required when catalogItemId is not provided")
    boolean isPowerRangeProvidedWhenNoCatalogItem() {
        if (catalogItemId != null) {
            return true;
        }
        return safePowerLimitWatt != null && simulationMinWatt != null && simulationMaxWatt != null;
    }
}
