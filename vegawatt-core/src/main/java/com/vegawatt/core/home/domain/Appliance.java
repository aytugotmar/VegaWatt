package com.vegawatt.core.home.domain;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
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
        boolean active,
        UUID catalogItemId,
        ApplianceCatalogCode catalogCodeSnapshot,
        ApplianceBehaviorProfile behaviorProfileSnapshot,
        BigDecimal standbyMinWatt,
        BigDecimal standbyMaxWatt) {

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
        if (simulationMaxWatt.compareTo(safePowerLimitWatt) > 0) {
            throw new InvalidApplianceConfigurationException("simulationMaxWatt must not exceed safePowerLimitWatt");
        }
        if (standbyMinWatt != null && standbyMaxWatt != null && standbyMaxWatt.compareTo(standbyMinWatt) < 0) {
            throw new InvalidApplianceConfigurationException("standbyMaxWatt must be >= standbyMinWatt");
        }
    }

    public static Appliance create(UUID homeId, String name, String type, BigDecimal safePowerLimitWatt,
                                    BigDecimal simulationMinWatt, BigDecimal simulationMaxWatt) {
        return new Appliance(UUID.randomUUID(), homeId, name, type, safePowerLimitWatt, simulationMinWatt,
                simulationMaxWatt, true, null, null, null, null, null);
    }

    public static Appliance createFromCatalog(UUID homeId, String name, ApplianceCatalogItem catalogItem,
                                               BigDecimal safePowerLimitWatt, BigDecimal simulationMinWatt,
                                               BigDecimal simulationMaxWatt) {
        return new Appliance(UUID.randomUUID(), homeId, name, catalogItem.code().value(), safePowerLimitWatt,
                simulationMinWatt, simulationMaxWatt, true, catalogItem.id(), catalogItem.code(),
                catalogItem.behaviorProfile(), catalogItem.defaultStandbyMinWatt(),
                catalogItem.defaultStandbyMaxWatt());
    }
}
