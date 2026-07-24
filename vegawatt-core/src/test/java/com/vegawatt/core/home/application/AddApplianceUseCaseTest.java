package com.vegawatt.core.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.DuplicateApplianceNameException;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.home.domain.InvalidCatalogSelectionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddApplianceUseCaseTest {

    @Mock
    private HomeRepository homeRepository;

    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;

    @Mock
    private AssetRegistrationPublisher assetRegistrationPublisher;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private ApplianceCatalogRepository applianceCatalogRepository;

    private static final UUID CATALOG_ITEM_ID = UUID.randomUUID();

    private AddApplianceUseCase useCase() {
        return new AddApplianceUseCase(homeRepository, applianceLiveStatePort, assetRegistrationPublisher,
                new ApplianceFactory(applianceCatalogRepository), clockProvider);
    }

    private static ApplianceCatalogItem coffeeMachineCatalogItem() {
        return new ApplianceCatalogItem(CATALOG_ITEM_ID, new ApplianceCatalogCode("COFFEE_MACHINE"),
                "Kahve Makinesi", "description", ApplianceCategory.KITCHEN, ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), BigDecimal.ZERO,
                new BigDecimal("2"), true, false, false, "coffee", null, true, true, 60);
    }

    private Home existingHome() {
        return Home.register("Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), Instant.parse("2026-07-20T10:00:00Z"));
    }

    @Test
    void addsAppliancesToExistingHomeAndInitializesLiveState() {
        Home home = existingHome();
        when(homeRepository.findById(home.id())).thenReturn(Optional.of(home));
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-23T10:00:00Z"));

        AddApplianceCommand command = new AddApplianceCommand(home.id(), "Buzdolabı", "REFRIGERATOR",
                new BigDecimal("2200"), new BigDecimal("100"), new BigDecimal("2000"), null);

        Appliance appliance = useCase().execute(command);

        assertThat(appliance.name()).isEqualTo("Buzdolabı");
        assertThat(home.appliances()).containsExactly(appliance);
        verify(homeRepository).save(home);
        verify(assetRegistrationPublisher).publish(home);
        verify(applianceLiveStatePort).initialize(any());
    }

    @Test
    void addsApplianceFromCatalogUsingDefaultsWhenWattFieldsOmitted() {
        Home home = existingHome();
        when(homeRepository.findById(home.id())).thenReturn(Optional.of(home));
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-23T10:00:00Z"));
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID))
                .thenReturn(Optional.of(coffeeMachineCatalogItem()));

        AddApplianceCommand command = new AddApplianceCommand(home.id(), "Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                null, null, null, CATALOG_ITEM_ID);

        Appliance appliance = useCase().execute(command);

        assertThat(appliance.safePowerLimitWatt()).isEqualByComparingTo("1500");
        assertThat(appliance.catalogItemId()).isEqualTo(CATALOG_ITEM_ID);
    }

    @Test
    void rejectsUnknownHome() {
        UUID homeId = UUID.randomUUID();
        when(homeRepository.findById(homeId)).thenReturn(Optional.empty());

        AddApplianceCommand command = new AddApplianceCommand(homeId, "Buzdolabı", "REFRIGERATOR",
                new BigDecimal("2200"), new BigDecimal("100"), new BigDecimal("2000"), null);

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOf(HomeNotFoundException.class);
    }

    @Test
    void rejectsDuplicateApplianceNameWithinHome() {
        Home home = existingHome();
        home.addAppliance(Appliance.create(home.id(), "Buzdolabı", "REFRIGERATOR",
                new BigDecimal("2200"), new BigDecimal("100"), new BigDecimal("2000")));
        when(homeRepository.findById(home.id())).thenReturn(Optional.of(home));

        AddApplianceCommand command = new AddApplianceCommand(home.id(), "buzdolabı", "REFRIGERATOR",
                new BigDecimal("2200"), new BigDecimal("100"), new BigDecimal("2000"), null);

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOf(DuplicateApplianceNameException.class);
    }

    @Test
    void rejectsUnknownCatalogItem() {
        Home home = existingHome();
        when(homeRepository.findById(home.id())).thenReturn(Optional.of(home));
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID)).thenReturn(Optional.empty());

        AddApplianceCommand command = new AddApplianceCommand(home.id(), "Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                null, null, null, CATALOG_ITEM_ID);

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOf(InvalidCatalogSelectionException.class);
    }
}
