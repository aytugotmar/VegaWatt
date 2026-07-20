package com.vegawatt.core.home.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class HomeTest {

    private static final Instant NOW = Instant.parse("2026-07-20T10:00:00Z");

    @Test
    void registersHomeWithValidTariffs() {
        Home home = Home.register("Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"),
                new BigDecimal("2500"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW);

        assertThat(home.id()).isNotNull();
        assertThat(home.appliances()).isEmpty();
    }

    @Test
    void rejectsPenaltyTariffBelowBaseTariff() {
        assertThatThrownBy(() -> Home.register("Ev", "e@example.com", new BigDecimal("500"),
                new BigDecimal("2500"), new BigDecimal("3.50"), new BigDecimal("2.10"), NOW))
                .isInstanceOf(InvalidHomeConfigurationException.class);
    }

    @Test
    void rejectsNonPositiveEnergyQuota() {
        assertThatThrownBy(() -> Home.register("Ev", "e@example.com", BigDecimal.ZERO,
                new BigDecimal("2500"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW))
                .isInstanceOf(InvalidHomeConfigurationException.class);
    }

    @Test
    void rejectsDuplicateApplianceNameCaseInsensitive() {
        Home home = Home.register("Ev", "e@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), NOW);
        home.addAppliance(Appliance.create(home.id(), "Buzdolabı", "REFRIGERATOR", new BigDecimal("2200"),
                new BigDecimal("100"), new BigDecimal("2000")));

        assertThatThrownBy(() -> home.addAppliance(Appliance.create(home.id(), "buzdolabı", "REFRIGERATOR",
                new BigDecimal("2200"), new BigDecimal("100"), new BigDecimal("2000"))))
                .isInstanceOf(DuplicateApplianceNameException.class);
    }

    @Test
    void allowsMultipleAppliancesWithDistinctNames() {
        Home home = Home.register("Ev", "e@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), NOW);
        home.addAppliance(Appliance.create(home.id(), "Buzdolabı", "REFRIGERATOR", new BigDecimal("2200"),
                new BigDecimal("100"), new BigDecimal("2000")));
        home.addAppliance(Appliance.create(home.id(), "Klima", "AC", new BigDecimal("2500"),
                new BigDecimal("200"), new BigDecimal("2300")));

        assertThat(home.appliances()).hasSize(2);
    }
}
