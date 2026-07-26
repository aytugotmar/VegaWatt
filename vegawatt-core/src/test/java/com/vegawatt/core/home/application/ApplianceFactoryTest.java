package com.vegawatt.core.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import com.vegawatt.core.home.domain.Appliance;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplianceFactoryTest {

    private static final UUID CATALOG_ITEM_ID = UUID.randomUUID();
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();

    private final ApplianceCatalogRepository applianceCatalogRepository = mock(ApplianceCatalogRepository.class);
    private final ApplianceFactory applianceFactory = new ApplianceFactory(applianceCatalogRepository);

    private static ApplianceCatalogItem coffeeMachineCatalogItem() {
        return new ApplianceCatalogItem(CATALOG_ITEM_ID, new ApplianceCatalogCode("COFFEE_MACHINE"),
                "Kahve Makinesi", "description", ApplianceCategory.KITCHEN, ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), BigDecimal.ZERO,
                new BigDecimal("2"), true, false, false, "coffee", null, true, true, 60);
    }

    @Test
    void resolvesDisplayNameAndIconKeyForACatalogLinkedAppliance() {
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID))
                .thenReturn(Optional.of(coffeeMachineCatalogItem()));
        Appliance appliance = new Appliance(APPLIANCE_ID, HOME_ID, "Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), true, CATALOG_ITEM_ID,
                new ApplianceCatalogCode("COFFEE_MACHINE"), ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                BigDecimal.ZERO, new BigDecimal("2"));

        ApplianceCatalogView view = applianceFactory.resolveCatalogView(appliance);

        assertThat(view.catalogCode()).isEqualTo("COFFEE_MACHINE");
        assertThat(view.catalogDisplayName()).isEqualTo("Kahve Makinesi");
        assertThat(view.catalogIconKey()).isEqualTo("coffee");
    }

    @Test
    void customApplianceWithNoCatalogItemIdGetsAllNullCosmetics() {
        Appliance customAppliance = new Appliance(APPLIANCE_ID, HOME_ID, "Özel Cihaz", "CUSTOM",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("80"), true, null, null, null, null,
                null);

        ApplianceCatalogView view = applianceFactory.resolveCatalogView(customAppliance);

        assertThat(view.catalogCode()).isNull();
        assertThat(view.catalogDisplayName()).isNull();
        assertThat(view.catalogIconKey()).isNull();
    }

    @Test
    void catalogItemIdPointingAtADisabledOrDeletedItemStillKeepsTheCodeSnapshotButNullsCosmetics() {
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID)).thenReturn(Optional.empty());
        Appliance appliance = new Appliance(APPLIANCE_ID, HOME_ID, "Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), true, CATALOG_ITEM_ID,
                new ApplianceCatalogCode("COFFEE_MACHINE"), ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                BigDecimal.ZERO, new BigDecimal("2"));

        ApplianceCatalogView view = applianceFactory.resolveCatalogView(appliance);

        assertThat(view.catalogCode()).isEqualTo("COFFEE_MACHINE");
        assertThat(view.catalogDisplayName()).isNull();
        assertThat(view.catalogIconKey()).isNull();
    }
}
