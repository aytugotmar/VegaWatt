package com.vegawatt.core.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.config.AnomalyProperties;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.notification.application.NotificationOrchestrator;
import com.vegawatt.core.notification.domain.AdvisoryTrigger;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.telemetry.domain.InvalidTelemetryReadingException;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessTelemetryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();

    @Mock
    private HomeRepository homeRepository;
    @Mock
    private ApplianceRepository applianceRepository;
    @Mock
    private HomeLiveStatePort homeLiveStatePort;
    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;
    @Mock
    private BillingAccountRepository billingAccountRepository;
    @Mock
    private ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    @Mock
    private TelemetryBillingRecorder telemetryBillingRecorder;
    @Mock
    private NotificationOrchestrator notificationOrchestrator;
    @Mock
    private ClockProvider clockProvider;

    private ProcessTelemetryUseCase useCase;
    private HomeLiveState homeLiveStateSlot;
    private ApplianceLiveState applianceLiveStateSlot;

    @BeforeEach
    void setUp() {
        EvaluateHomeBillingUseCase evaluateHomeBillingUseCase = new EvaluateHomeBillingUseCase(
                billingAccountRepository);
        EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase = new EvaluateApplianceAnomalyUseCase(
                new AnomalyProperties(3));

        useCase = new ProcessTelemetryUseCase(homeRepository, applianceRepository, homeLiveStatePort,
                applianceLiveStatePort, evaluateHomeBillingUseCase, evaluateApplianceAnomalyUseCase,
                processedTelemetryEventRepository, telemetryBillingRecorder, notificationOrchestrator, clockProvider);

        lenient().when(clockProvider.now()).thenReturn(NOW);
        lenient().when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(testHome()));
        lenient().when(billingAccountRepository.findByHomeId(HOME_ID)).thenReturn(Optional.empty());
        lenient().when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(testAppliance(true)));
        lenient().when(homeLiveStatePort.update(eq(HOME_ID), any())).thenAnswer(invocation -> {
            UnaryOperator<HomeLiveState> mutator = invocation.getArgument(1);
            homeLiveStateSlot = mutator.apply(homeLiveStateSlot);
            return homeLiveStateSlot;
        });
        lenient().when(applianceLiveStatePort.update(eq(HOME_ID), eq(APPLIANCE_ID), any())).thenAnswer(invocation -> {
            UnaryOperator<ApplianceLiveState> mutator = invocation.getArgument(2);
            applianceLiveStateSlot = mutator.apply(applianceLiveStateSlot);
            return applianceLiveStateSlot;
        });
        lenient().when(telemetryBillingRecorder.persist(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
    }

    private static Home testHome() {
        return Home.reconstitute(HOME_ID, "Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
    }

    private static Appliance testAppliance(boolean active) {
        return new Appliance(APPLIANCE_ID, HOME_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), new BigDecimal("150"), active);
    }

    private static TelemetryReading reading(UUID eventId, BigDecimal powerWatt) {
        return new TelemetryReading(eventId, HOME_ID, APPLIANCE_ID, powerWatt, 5, NOW);
    }

    @Test
    void skipsAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        when(processedTelemetryEventRepository.existsByEventId(eventId)).thenReturn(true);

        useCase.execute(reading(eventId, new BigDecimal("1000")));

        verifyNoInteractions(homeLiveStatePort, applianceLiveStatePort, telemetryBillingRecorder,
                notificationOrchestrator);
    }

    @Test
    void rejectsTelemetryWhenApplianceDoesNotBelongToHome() {
        UUID otherHomeId = UUID.randomUUID();
        TelemetryReading mismatched = new TelemetryReading(UUID.randomUUID(), otherHomeId, APPLIANCE_ID,
                new BigDecimal("1000"), 5, NOW);
        when(homeRepository.findById(otherHomeId)).thenReturn(Optional.of(
                Home.reconstitute(otherHomeId, "Other Ev", "other@example.com", new BigDecimal("500"),
                        new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of())));

        assertThatThrownBy(() -> useCase.execute(mismatched))
                .isInstanceOf(InvalidTelemetryReadingException.class);

        verifyNoInteractions(homeLiveStatePort, applianceLiveStatePort, telemetryBillingRecorder);
    }

    @Test
    void skipsInactiveApplianceWithoutTouchingLiveState() {
        when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(testAppliance(false)));

        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        verifyNoInteractions(homeLiveStatePort, applianceLiveStatePort, telemetryBillingRecorder,
                notificationOrchestrator);
    }

    @Test
    void firesAdvisoryOnlyAfterBillingPersistReturns() {
        UUID operationalEventId = UUID.randomUUID();
        when(telemetryBillingRecorder.persist(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(new AdvisoryTrigger(AdvisoryTriggerType.QUOTA_80, operationalEventId)));

        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        InOrder inOrder = Mockito.inOrder(telemetryBillingRecorder, notificationOrchestrator);
        inOrder.verify(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), anyInt());
        inOrder.verify(notificationOrchestrator).triggerAdvisory(HOME_ID, AdvisoryTriggerType.QUOTA_80,
                operationalEventId);
    }

    @Test
    void doesNotFireAdvisoryWhenNoTriggersReturned() {
        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        verify(notificationOrchestrator, never()).triggerAdvisory(any(), any(), any());
    }

    @Test
    void recoversAccumulatedStateFromBillingAccountAfterIgniteCacheMiss() {
        BillingAccount billingAccount = BillingAccount.reconstitute(UUID.randomUUID(), HOME_ID, "2026-01",
                new BigDecimal("400.5").setScale(9), Money.of(new BigDecimal("900.00")), true, true, false, true,
                false, 0L, NOW, NOW);
        when(billingAccountRepository.findByHomeId(HOME_ID)).thenReturn(Optional.of(billingAccount));

        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        assertThat(homeLiveStateSlot.currentEnergyKwh()).isGreaterThan(new BigDecimal("400.5"));
        assertThat(homeLiveStateSlot.currentCost().amount()).isGreaterThan(new BigDecimal("900.00"));
        assertThat(homeLiveStateSlot.tariffState()).isEqualTo(TariffState.PENALTY);
    }

    @Test
    void accumulatesSubKurusCostAcrossManySmallTelemetryEvents() {
        for (int i = 0; i < 500; i++) {
            useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1450.5")));
        }

        assertThat(homeLiveStateSlot.currentCost().rounded()).isNotEqualByComparingTo("0.00");
    }
}
