package com.vegawatt.core.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.common.time.ClockProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartupReconciliationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private HomeRepository homeRepository;
    @Mock
    private HomeLiveStatePort homeLiveStatePort;
    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;
    @Mock
    private BillingAccountRepository billingAccountRepository;
    @Mock
    private AssetRegistrationPublisher assetRegistrationPublisher;
    @Mock
    private ClockProvider clockProvider;
    @Mock
    private ApplianceCatalogRepository applianceCatalogRepository;

    private StartupReconciliationService service;

    @BeforeEach
    void setUp() {
        when(clockProvider.now()).thenReturn(NOW);
        EvaluateHomeBillingUseCase evaluateHomeBillingUseCase = new EvaluateHomeBillingUseCase(
                billingAccountRepository);
        service = new StartupReconciliationService(homeRepository, homeLiveStatePort, applianceLiveStatePort,
                evaluateHomeBillingUseCase, assetRegistrationPublisher, clockProvider,
                new ApplianceFactory(applianceCatalogRepository));
    }

    private static Home testHome() {
        Home home = Home.register("Test Ev", "test@example.com", new BigDecimal("500"), new BigDecimal("1000"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), NOW);
        home.addAppliance(Appliance.create(home.id(), "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), new BigDecimal("150")));
        return home;
    }

    @Test
    void recoversColdIgniteHomeAndApplianceStateFromLedger() {
        Home home = testHome();
        when(homeRepository.findAll()).thenReturn(List.of(home));
        when(homeLiveStatePort.get(home.id())).thenReturn(Optional.empty());
        when(applianceLiveStatePort.get(home.id(), home.appliances().get(0).id())).thenReturn(Optional.empty());
        when(billingAccountRepository.findByHomeIdAndBillingPeriod(home.id(), BillingPeriodResolver.currentPeriod(NOW))).thenReturn(Optional.empty());

        service.run(null);

        verify(homeLiveStatePort).initialize(any(HomeLiveState.class));
        verify(applianceLiveStatePort).initialize(any(ApplianceLiveState.class));
        verify(assetRegistrationPublisher).publish(home);
    }

    @Test
    void doesNotReinitializeWarmIgniteStateButStillRepublishesRegistration() {
        Home home = testHome();
        when(homeRepository.findAll()).thenReturn(List.of(home));
        when(homeLiveStatePort.get(home.id())).thenReturn(Optional.of(HomeLiveState.zero(home.id(), home.name(),
                NOW)));
        when(applianceLiveStatePort.get(home.id(), home.appliances().get(0).id())).thenReturn(Optional.of(
                ApplianceLiveState.zero(home.id(), home.appliances().get(0).id(), "Fridge", "REFRIGERATOR",
                        new BigDecimal("200"), NOW)));

        service.run(null);

        verify(homeLiveStatePort, never()).initialize(any());
        verify(applianceLiveStatePort, never()).initialize(any());
        verify(assetRegistrationPublisher).publish(home);
    }

    @Test
    void refreshesCatalogCosmeticsOnAnExistingIgniteEntryWithoutTouchingTelemetryOwnedFields() {
        UUID catalogItemId = UUID.randomUUID();
        Home home = Home.register("Test Ev", "test@example.com", new BigDecimal("500"), new BigDecimal("1000"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), NOW);
        Appliance appliance = new Appliance(UUID.randomUUID(), home.id(), "Mutfak Kahve Makinesi", "COFFEE_MACHINE",
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), true, catalogItemId,
                new ApplianceCatalogCode("COFFEE_MACHINE"),
                ApplianceBehaviorProfile.SHORT_HIGH_POWER, BigDecimal.ZERO,
                new BigDecimal("2"));
        home.addAppliance(appliance);
        ApplianceCatalogItem catalogItem = new ApplianceCatalogItem(catalogItemId,
                new ApplianceCatalogCode("COFFEE_MACHINE"),
                "Kahve Makinesi", "description",
                ApplianceCategory.KITCHEN,
                ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), BigDecimal.ZERO,
                new BigDecimal("2"), true, false, false, "coffee", null, true, true, 60);
        when(applianceCatalogRepository.findEnabledById(catalogItemId)).thenReturn(Optional.of(catalogItem));

        ApplianceLiveState existingLiveState = ApplianceLiveState.zero(home.id(), appliance.id(),
                appliance.name(), appliance.type(), appliance.safePowerLimitWatt(), NOW, "COFFEE_MACHINE", null, null)
                .withStateVersion(3L);

        when(homeRepository.findAll()).thenReturn(List.of(home));
        when(homeLiveStatePort.get(home.id())).thenReturn(Optional.of(HomeLiveState.zero(home.id(), home.name(),
                NOW)));
        when(applianceLiveStatePort.get(home.id(), appliance.id())).thenReturn(Optional.of(existingLiveState));

        service.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UnaryOperator<ApplianceLiveState>> mutatorCaptor = ArgumentCaptor.forClass(
                UnaryOperator.class);
        verify(applianceLiveStatePort).update(eq(home.id()), eq(appliance.id()), mutatorCaptor.capture());
        ApplianceLiveState refreshed = mutatorCaptor.getValue().apply(existingLiveState);

        assertThat(refreshed.catalogDisplayName()).isEqualTo("Kahve Makinesi");
        assertThat(refreshed.catalogIconKey()).isEqualTo("coffee");
        assertThat(refreshed.catalogCode()).isEqualTo("COFFEE_MACHINE");
        assertThat(refreshed.stateVersion()).isEqualTo(existingLiveState.stateVersion());
        assertThat(refreshed.lastProcessedSequence()).isEqualTo(existingLiveState.lastProcessedSequence());
        assertThat(refreshed.lastEventId()).isEqualTo(existingLiveState.lastEventId());
        assertThat(refreshed.telemetryHealthStatus()).isEqualTo(existingLiveState.telemetryHealthStatus());
    }

    @Test
    void continuesReconcilingOtherHomesWhenOneFails() {
        Home failingHome = testHome();
        Home okHome = testHome();
        when(homeRepository.findAll()).thenReturn(List.of(failingHome, okHome));
        when(homeLiveStatePort.get(failingHome.id())).thenThrow(new RuntimeException("ignite unavailable"));
        when(homeLiveStatePort.get(okHome.id())).thenReturn(Optional.empty());
        when(applianceLiveStatePort.get(okHome.id(), okHome.appliances().get(0).id())).thenReturn(Optional.empty());
        when(billingAccountRepository.findByHomeIdAndBillingPeriod(okHome.id(), BillingPeriodResolver.currentPeriod(NOW))).thenReturn(Optional.empty());

        service.run(null);

        verify(assetRegistrationPublisher, times(1)).publish(okHome);
        verify(assetRegistrationPublisher, never()).publish(failingHome);
    }
}
