package com.vegawatt.sensors.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HomeRegistryTest {

    @Test
    void findsAnAppliancePreviouslyUpserted() {
        HomeRegistry registry = new HomeRegistry();
        UUID applianceId = UUID.randomUUID();
        ApplianceConfig config = new ApplianceConfig(applianceId, UUID.randomUUID(), new BigDecimal("2000"),
                new BigDecimal("100"), new BigDecimal("1800"));

        registry.upsert(config);

        assertThat(registry.find(applianceId)).contains(config);
    }

    @Test
    void reRegistrationReplacesConfigForTheSameApplianceId() {
        HomeRegistry registry = new HomeRegistry();
        UUID applianceId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        registry.upsert(new ApplianceConfig(applianceId, homeId, new BigDecimal("2000"), new BigDecimal("100"),
                new BigDecimal("1800")));

        ApplianceConfig updated = new ApplianceConfig(applianceId, homeId, new BigDecimal("2500"),
                new BigDecimal("200"), new BigDecimal("2200"));
        registry.upsert(updated);

        assertThat(registry.find(applianceId)).contains(updated);
        assertThat(registry.registeredApplianceIds()).hasSize(1);
    }

    @Test
    void returnsEmptyForUnknownAppliance() {
        HomeRegistry registry = new HomeRegistry();

        assertThat(registry.find(UUID.randomUUID())).isEmpty();
    }
}
