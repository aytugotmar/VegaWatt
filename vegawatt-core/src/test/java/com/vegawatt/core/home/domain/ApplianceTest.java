package com.vegawatt.core.home.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplianceTest {

    private static final UUID HOME_ID = UUID.randomUUID();

    @Test
    void createsActiveApplianceWithGeneratedId() {
        Appliance appliance = Appliance.create(HOME_ID, "Klima", "AC", new BigDecimal("2500"),
                new BigDecimal("200"), new BigDecimal("2300"));

        assertThat(appliance.id()).isNotNull();
        assertThat(appliance.active()).isTrue();
    }

    @Test
    void rejectsNonPositiveSafePowerLimit() {
        assertThatThrownBy(() -> Appliance.create(HOME_ID, "Klima", "AC", BigDecimal.ZERO,
                new BigDecimal("200"), new BigDecimal("2300")))
                .isInstanceOf(InvalidApplianceConfigurationException.class);
    }

    @Test
    void rejectsSimulationMaxNotGreaterThanMin() {
        assertThatThrownBy(() -> Appliance.create(HOME_ID, "Klima", "AC", new BigDecimal("2500"),
                new BigDecimal("2000"), new BigDecimal("2000")))
                .isInstanceOf(InvalidApplianceConfigurationException.class);
    }

    @Test
    void rejectsNegativeSimulationMin() {
        assertThatThrownBy(() -> Appliance.create(HOME_ID, "Klima", "AC", new BigDecimal("2500"),
                new BigDecimal("-1"), new BigDecimal("2300")))
                .isInstanceOf(InvalidApplianceConfigurationException.class);
    }
}
