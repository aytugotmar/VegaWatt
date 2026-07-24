package com.vegawatt.core.home.api;

import com.vegawatt.core.home.domain.Appliance;
import java.math.BigDecimal;
import java.util.UUID;

public record AddApplianceResponse(
        UUID applianceId,
        String name,
        String type,
        BigDecimal safePowerLimitWatt,
        BigDecimal simulationMinWatt,
        BigDecimal simulationMaxWatt,
        UUID catalogItemId) {

    public static AddApplianceResponse from(Appliance appliance) {
        return new AddApplianceResponse(appliance.id(), appliance.name(), appliance.type(),
                appliance.safePowerLimitWatt(), appliance.simulationMinWatt(), appliance.simulationMaxWatt(),
                appliance.catalogItemId());
    }
}
