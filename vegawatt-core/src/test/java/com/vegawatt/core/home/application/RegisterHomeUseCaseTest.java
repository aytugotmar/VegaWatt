package com.vegawatt.core.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.access.domain.HomeAccessService;
import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.home.domain.InvalidCatalogSelectionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterHomeUseCaseTest {

    @Mock
    private HomeRepository homeRepository;

    @Mock
    private BillingAccountRepository billingAccountRepository;

    @Mock
    private HomeLiveStatePort homeLiveStatePort;

    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;

    @Mock
    private AssetRegistrationPublisher assetRegistrationPublisher;

    @Mock
    private HomeAccessService homeAccessService;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private ApplianceCatalogRepository applianceCatalogRepository;

    private static final UUID CATALOG_ITEM_ID = UUID.randomUUID();

    private RegisterHomeUseCase useCase() {
        return new RegisterHomeUseCase(homeRepository, billingAccountRepository, homeLiveStatePort,
                applianceLiveStatePort, assetRegistrationPublisher, homeAccessService, clockProvider,
                applianceCatalogRepository);
    }

    private static ApplianceCatalogItem coffeeMachineCatalogItem() {
        return new ApplianceCatalogItem(CATALOG_ITEM_ID, new ApplianceCatalogCode("COFFEE_MACHINE"),
                "Kahve Makinesi", "description", ApplianceCategory.KITCHEN, ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), BigDecimal.ZERO,
                new BigDecimal("2"), true, false, false, "coffee", null, true, true, 60);
    }

    private void mockSuccessfulPersistence() {
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-20T10:00:00Z"));
        when(homeRepository.save(any(Home.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billingAccountRepository.save(any(BillingAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registersHomeWithAppliancesAndInitializesLiveState() {
        mockSuccessfulPersistence();

        UUID ownerUserId = UUID.randomUUID();
        RegisterHomeCommand command = new RegisterHomeCommand(
                "Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"),
                List.of(new RegisterHomeCommand.ApplianceCommand("Buzdolabı", "REFRIGERATOR",
                        new BigDecimal("2200"), new BigDecimal("100"), new BigDecimal("2000"), null)),
                ownerUserId);

        Home result = useCase().execute(command);

        assertThat(result.name()).isEqualTo("Ayşe'nin Evi");
        assertThat(result.appliances()).hasSize(1);
        assertThat(result.appliances().get(0).catalogItemId()).isNull();
        verify(homeRepository).save(result);
        verify(assetRegistrationPublisher).publish(result);
        verify(homeLiveStatePort).initialize(any());
        verify(applianceLiveStatePort).initialize(any());
        verify(homeAccessService).grantOwnership(result.id(), ownerUserId, Instant.parse("2026-07-20T10:00:00Z"));
    }

    @Test
    void registersApplianceFromCatalogWithProvidedWattOverrides() {
        mockSuccessfulPersistence();
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID))
                .thenReturn(Optional.of(coffeeMachineCatalogItem()));

        RegisterHomeCommand command = new RegisterHomeCommand(
                "Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"),
                List.of(new RegisterHomeCommand.ApplianceCommand("Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                        new BigDecimal("1450"), new BigDecimal("650"), new BigDecimal("1350"), CATALOG_ITEM_ID)),
                UUID.randomUUID());

        Home result = useCase().execute(command);

        var appliance = result.appliances().get(0);
        assertThat(appliance.safePowerLimitWatt()).isEqualByComparingTo("1450");
        assertThat(appliance.simulationMinWatt()).isEqualByComparingTo("650");
        assertThat(appliance.simulationMaxWatt()).isEqualByComparingTo("1350");
        assertThat(appliance.catalogItemId()).isEqualTo(CATALOG_ITEM_ID);
        assertThat(appliance.catalogCodeSnapshot().value()).isEqualTo("COFFEE_MACHINE");
        assertThat(appliance.behaviorProfileSnapshot()).isEqualTo(ApplianceBehaviorProfile.SHORT_HIGH_POWER);
    }

    @Test
    void registersApplianceFromCatalogUsingDefaultsWhenWattFieldsOmitted() {
        mockSuccessfulPersistence();
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID))
                .thenReturn(Optional.of(coffeeMachineCatalogItem()));

        RegisterHomeCommand command = new RegisterHomeCommand(
                "Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"),
                List.of(new RegisterHomeCommand.ApplianceCommand("Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                        null, null, null, CATALOG_ITEM_ID)),
                UUID.randomUUID());

        Home result = useCase().execute(command);

        var appliance = result.appliances().get(0);
        assertThat(appliance.safePowerLimitWatt()).isEqualByComparingTo("1500");
        assertThat(appliance.simulationMinWatt()).isEqualByComparingTo("600");
        assertThat(appliance.simulationMaxWatt()).isEqualByComparingTo("1300");
        assertThat(appliance.standbyMinWatt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(appliance.standbyMaxWatt()).isEqualByComparingTo("2");
    }

    @Test
    void rejectsUnknownOrDisabledCatalogItemId() {
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-20T10:00:00Z"));
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID)).thenReturn(Optional.empty());

        RegisterHomeCommand command = new RegisterHomeCommand(
                "Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"),
                List.of(new RegisterHomeCommand.ApplianceCommand("Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                        null, null, null, CATALOG_ITEM_ID)),
                UUID.randomUUID());

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOf(InvalidCatalogSelectionException.class);
    }

    @Test
    void rejectsTypeMismatchWithCatalogCode() {
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-20T10:00:00Z"));
        when(applianceCatalogRepository.findEnabledById(CATALOG_ITEM_ID))
                .thenReturn(Optional.of(coffeeMachineCatalogItem()));

        RegisterHomeCommand command = new RegisterHomeCommand(
                "Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"),
                List.of(new RegisterHomeCommand.ApplianceCommand("Yanlış Cihaz", "REFRIGERATOR",
                        null, null, null, CATALOG_ITEM_ID)),
                UUID.randomUUID());

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOf(InvalidCatalogSelectionException.class);
    }
}
