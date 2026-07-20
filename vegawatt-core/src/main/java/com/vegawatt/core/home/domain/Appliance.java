package com.vegawatt.core.home.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Appliance(
        UUID id,
        UUID homeId,
        String name,
        String type,
        BigDecimal safePowerLimitWatt,
        BigDecimal simulationMinWatt,
        BigDecimal simulationMaxWatt,
        boolean active) {

    public Appliance {
        if (safePowerLimitWatt.signum() <= 0) {
            throw new InvalidApplianceConfigurationException("safePowerLimitWatt must be positive");
        }
        if (simulationMinWatt.signum() < 0) {
            throw new InvalidApplianceConfigurationException("simulationMinWatt must not be negative");
        }
        if (simulationMaxWatt.compareTo(simulationMinWatt) <= 0) {
            throw new InvalidApplianceConfigurationException("simulationMaxWatt must be greater than simulationMinWatt");
        }
    }

    public static Appliance create(UUID homeId, String name, String type, BigDecimal safePowerLimitWatt,
                                    BigDecimal simulationMinWatt, BigDecimal simulationMaxWatt) {
        return new Appliance(UUID.randomUUID(), homeId, name, type, safePowerLimitWatt, simulationMinWatt,
                simulationMaxWatt, true);
    }
}
