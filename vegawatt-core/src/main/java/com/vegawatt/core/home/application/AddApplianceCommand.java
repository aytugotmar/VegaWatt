package com.vegawatt.core.home.application;

import java.math.BigDecimal;
import java.util.UUID;

public record AddApplianceCommand(
        UUID homeId,
        String name,
        String type,
        BigDecimal safePowerLimitWatt,
        BigDecimal simulationMinWatt,
        BigDecimal simulationMaxWatt,
        UUID catalogItemId) {
}
