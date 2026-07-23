package com.vegawatt.core.home.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplianceTest {

    private static final UUID HOME_ID = UUID.randomUUID();

    private static ApplianceCatalogItem coffeeMachineCatalogItem() {
        return new ApplianceCatalogItem(UUID.randomUUID(), new ApplianceCatalogCode("COFFEE_MACHINE"),
                "Kahve Makinesi", "description", ApplianceCategory.KITCHEN, ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), new BigDecimal("0"),
                new BigDecimal("2"), true, false, false, "coffee", null, true, true, 60);
    }

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

    @Test
    void createsApplianceFromCatalogWithSnapshottedFields() {
        ApplianceCatalogItem catalogItem = coffeeMachineCatalogItem();

        Appliance appliance = Appliance.createFromCatalog(HOME_ID, "Mutfak Kahve Makinesi", catalogItem,
                new BigDecimal("1450"), new BigDecimal("650"), new BigDecimal("1350"));

        assertThat(appliance.type()).isEqualTo("COFFEE_MACHINE");
        assertThat(appliance.catalogItemId()).isEqualTo(catalogItem.id());
        assertThat(appliance.catalogCodeSnapshot()).isEqualTo(catalogItem.code());
        assertThat(appliance.behaviorProfileSnapshot()).isEqualTo(ApplianceBehaviorProfile.SHORT_HIGH_POWER);
        assertThat(appliance.standbyMinWatt()).isEqualByComparingTo("0");
        assertThat(appliance.standbyMaxWatt()).isEqualByComparingTo("2");
    }

    @Test
    void rejectsStandbyMaxLessThanStandbyMin() {
        assertThatThrownBy(() -> new Appliance(UUID.randomUUID(), HOME_ID, "Klima", "AC", new BigDecimal("2500"),
                new BigDecimal("200"), new BigDecimal("2300"), true, null, null, null,
                new BigDecimal("5"), new BigDecimal("1")))
                .isInstanceOf(InvalidApplianceConfigurationException.class);
    }
}
