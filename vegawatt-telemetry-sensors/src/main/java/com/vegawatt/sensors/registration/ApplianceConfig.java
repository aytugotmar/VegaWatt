package com.vegawatt.sensors.registration;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplianceConfig(
        UUID applianceId,
        UUID homeId,
        String type,
        BigDecimal safePowerLimitWatt,
        BigDecimal simulationMinWatt,
        BigDecimal simulationMaxWatt) {
}
