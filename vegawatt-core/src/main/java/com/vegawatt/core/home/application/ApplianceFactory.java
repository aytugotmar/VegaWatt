package com.vegawatt.core.home.application;

import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.InvalidCatalogSelectionException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Shared appliance-construction logic for both initial home registration and adding a device
 * to an existing home later — resolves catalog defaults and validates the catalog selection. */
@Component
class ApplianceFactory {

    private final ApplianceCatalogRepository applianceCatalogRepository;

    ApplianceFactory(ApplianceCatalogRepository applianceCatalogRepository) {
        this.applianceCatalogRepository = applianceCatalogRepository;
    }

    Appliance build(UUID homeId, String name, String type, BigDecimal safePowerLimitWatt,
                     BigDecimal simulationMinWatt, BigDecimal simulationMaxWatt, UUID catalogItemId) {
        if (catalogItemId == null) {
            return Appliance.create(homeId, name, type, safePowerLimitWatt, simulationMinWatt, simulationMaxWatt);
        }

        ApplianceCatalogItem catalogItem = applianceCatalogRepository.findEnabledById(catalogItemId)
                .orElseThrow(() -> new InvalidCatalogSelectionException(
                        "Unknown or disabled appliance catalog item: " + catalogItemId));

        if (!catalogItem.code().value().equals(type)) {
            throw new InvalidCatalogSelectionException(
                    "Appliance type '" + type + "' does not match catalog item code '"
                            + catalogItem.code().value() + "'");
        }

        BigDecimal resolvedSafePowerLimitWatt = safePowerLimitWatt != null
                ? safePowerLimitWatt : catalogItem.defaultSafePowerLimitWatt();
        BigDecimal resolvedSimulationMinWatt = simulationMinWatt != null
                ? simulationMinWatt : catalogItem.defaultActiveMinWatt();
        BigDecimal resolvedSimulationMaxWatt = simulationMaxWatt != null
                ? simulationMaxWatt : catalogItem.defaultActiveMaxWatt();

        return Appliance.createFromCatalog(homeId, name, catalogItem, resolvedSafePowerLimitWatt,
                resolvedSimulationMinWatt, resolvedSimulationMaxWatt);
    }
}
